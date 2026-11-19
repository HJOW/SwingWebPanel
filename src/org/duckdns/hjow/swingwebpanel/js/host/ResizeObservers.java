package org.duckdns.hjow.swingwebpanel.js.host;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
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
 * <code>ResizeObserver</code>, <code>ResizeObserverEntry</code> and <code>ResizeObserverSize</code> (Resize Observer Module Level 1, W3C Working Draft,
 * fetched 2026-11-17): the constructor, <code>observe(target, {box})</code>, <code>unobserve</code>, <code>disconnect</code>, and the processing model: a
 * target is <em>active</em> when the size of the observed box differs from the size reported last (it starts as 0 x 0, so an element that is not rendered
 * never reports), the active targets of the shallowest depth are delivered, and the loop goes on with the targets that are deeper than the deepest delivered one
 * until none is active; targets that stay behind are skipped and an <code>error</code> event with the message "ResizeObserver loop completed with undelivered
 * notifications." is reported at the window. Choices where this page differs from a browser:
 * <ul>
 * <li>the sizes come from the border box of the layout hook minus the computed padding and border widths (percentages of the padding refer to the width of
 * the parent box; scrollbars are not subtracted); the box of an element that is a <em>non-replaced inline</em> element is empty, and the sizes of an element
 * that is not rendered are zero;</li>
 * <li><code>device-pixel-content-box</code> is the content box times <code>devicePixelRatio</code>, rounded to integers;</li>
 * <li>the inline size and the block size follow <code>writing-mode</code>: they are the height and the width in a vertical writing mode;</li>
 * <li>an element of an <code>svg</code> is not measured with its bounding box: it is measured like any element (the layout hook reports the border box);</li>
 * <li>the sequences of sizes have one item (the first fragment).</li>
 * </ul>
 * <p>
 * <code>ResizeObserver</code>, <code>ResizeObserverEntry</code>, <code>ResizeObserverSize</code>(Resize Observer Module Level 1, W3C Working Draft, 2026-11-17 조회):
 * 생성자, <code>observe(target, {box})</code>, <code>unobserve</code>, <code>disconnect</code> 와 처리 모델. 관찰하는 상자의 크기가 마지막으로 보고한 크기와
 * 다르면 대상이 <em>활성</em>이다(처음 크기는 0 x 0 이므로 렌더링되지 않는 요소는 보고하지 않는다). 가장 얕은 깊이의 활성 대상을 전달하고, 전달한 가장 깊은 것보다 더
 * 깊은 대상으로 활성이 없을 때까지 되풀이한다. 남은 대상은 건너뛰며 window 에 "ResizeObserver loop completed with undelivered notifications." 메시지의
 * <code>error</code> 이벤트를 보고한다. 브라우저와 다른 선택:
 * <ul>
 * <li>크기는 레이아웃 훅의 border box 에서 계산된 padding 과 border 두께를 뺀 것이다(padding 의 백분율은 부모 상자의 폭에 대한 비율이고 스크롤바는 빼지 않는다).
 * <em>치환되지 않은 인라인</em> 요소의 상자는 비어 있고 렌더링되지 않는 요소의 크기는 0 이다.</li>
 * <li><code>device-pixel-content-box</code> 는 content box 에 <code>devicePixelRatio</code> 를 곱해 정수로 반올림한 값이다.</li>
 * <li>인라인 크기와 블록 크기는 <code>writing-mode</code> 를 따른다. 세로쓰기에서는 높이와 폭이다.</li>
 * <li><code>svg</code> 안의 요소를 bounding box 로 재지 않고 다른 요소와 같이 잰다(레이아웃 훅이 border box 를 알려 준다).</li>
 * <li>크기 시퀀스에는 항목이 하나(첫 조각) 있다.</li>
 * </ul>
 */
final class ResizeObservers {

    /**
     * The elements that are measured as boxes even if their <code>display</code> is <code>inline</code> (replaced and control elements).
     * <p>
     * <code>display</code> 가 <code>inline</code> 이어도 상자로 재는 요소(치환 요소와 컨트롤).
     */
    private static final Set<String> REPLACED = new HashSet<String>(Arrays.asList("img", "video", "canvas", "iframe", "object", "embed", "svg", "input",
            "button", "select", "textarea", "meter", "progress", "audio", "image"));

    /**
     * The observed boxes in the order of the numbers 0, 1, 2.
     * <p>
     * 0, 1, 2 번호 순서의 관찰하는 상자.
     */
    private static final String[] BOXES = { "content-box", "border-box", "device-pixel-content-box" };

    /**
     * One observed target (the <code>ResizeObservation</code> record).
     * <p>
     * 관찰하는 대상 하나(<code>ResizeObservation</code> 기록).
     */
    private static final class Observation {
        /**
         * The target.
         * <p>
         * 대상.
         */
        Element target;

        /**
         * The observed box: an index into {@link #BOXES}.
         * <p>
         * 관찰하는 상자: {@link #BOXES} 의 인덱스.
         */
        int box;

        /**
         * The inline size that was reported last.
         * <p>
         * 마지막으로 보고한 인라인 크기.
         */
        double lastInline;

        /**
         * The block size that was reported last.
         * <p>
         * 마지막으로 보고한 블록 크기.
         */
        double lastBlock;
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
         * The observed targets (the <code>observationTargets</code> slot).
         * <p>
         * 관찰하는 대상(<code>observationTargets</code> 슬롯).
         */
        final List<Observation> targets = new ArrayList<Observation>();

        /**
         * The observations that are active in the current loop (the <code>activeTargets</code> slot).
         * <p>
         * 현재 루프에서 활성인 관찰(<code>activeTargets</code> 슬롯).
         */
        final List<Observation> active = new ArrayList<Observation>();

        /**
         * The observations that changed but are too shallow for the current loop (the <code>skippedTargets</code> slot).
         * <p>
         * 바뀌었지만 현재 루프에는 너무 얕은 관찰(<code>skippedTargets</code> 슬롯).
         */
        final List<Observation> skipped = new ArrayList<Observation>();
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
     * The script object of an entry or of a size: the values of the attributes in the order of the attribute names of its interface.
     * <p>
     * 항목이나 크기의 스크립트 객체: 인터페이스의 속성 이름 순서의 속성 값.
     */
    private static final class ValueObject extends JsObject {
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
        ValueObject(JsObject proto, JsValue[] values) {
            super(proto);
            this.values = values;
        }
    }

    /**
     * The measured boxes of an element.
     * <p>
     * 요소에서 잰 상자.
     */
    private static final class Sizes {
        /**
         * The inline and block size of each observed box, indexed like {@link #BOXES}.
         * <p>
         * {@link #BOXES} 와 같은 인덱스의 관찰하는 상자별 인라인·블록 크기.
         */
        final double[][] size = new double[3][2];

        /**
         * The left padding.
         * <p>
         * 왼쪽 padding.
         */
        double paddingLeft;

        /**
         * The top padding.
         * <p>
         * 위쪽 padding.
         */
        double paddingTop;

        /**
         * The content width (physical).
         * <p>
         * content 폭(물리).
         */
        double contentWidth;

        /**
         * The content height (physical).
         * <p>
         * content 높이(물리).
         */
        double contentHeight;
    }

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
     * The observers (all of them that were created and not collected; an observer without targets does nothing).
     * <p>
     * 관찰자(만들어졌고 수거되지 않은 모든 것. 대상이 없는 관찰자는 아무것도 하지 않는다).
     */
    private final List<Observer> observers = new ArrayList<Observer>();

    /**
     * The prototype of the entries.
     * <p>
     * 항목의 프로토타입.
     */
    private JsObject entryProto;

    /**
     * The prototype of the sizes.
     * <p>
     * 크기의 프로토타입.
     */
    private JsObject sizeProto;

    /**
     * Creates the helper.
     * <p>
     * 도우미를 만든다.
     *
     * @param page  the page / 페이지
     * @param owner the coordinator / 조정자
     */
    ResizeObservers(PageScripts page, RenderObservers owner) {
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
        // 대상이 있는 관찰자를 하나라도 찾으면 true.
        for (int i = 0; i < observers.size(); i++) {
            if (!observers.get(i).targets.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Installs <code>ResizeObserver</code>, <code>ResizeObserverEntry</code> and <code>ResizeObserverSize</code>.
     * <p>
     * <code>ResizeObserver</code>, <code>ResizeObserverEntry</code>, <code>ResizeObserverSize</code> 를 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    void install(JsObject g) {
        final JsRealm realm = page.getRealm();
        entryProto = valueInterface(g, realm, "ResizeObserverEntry", new String[] { "target", "contentRect", "borderBoxSize", "contentBoxSize",
            "devicePixelContentBoxSize" });
        sizeProto = valueInterface(g, realm, "ResizeObserverSize", new String[] { "inlineSize", "blockSize" });
        final JsObject proto = realm.newObject();
        NativeFunction ctor = new NativeFunction(realm, "ResizeObserver", 1, new NativeFunction.Call() {
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
                throw JsException.typeError("Failed to construct 'ResizeObserver': Please use the 'new' operator, this DOM object constructor cannot be called as a function.");
            }
        }, new NativeFunction.Construct() {
            /**
             * Creates an observer with its callback.
             * <p>
             * 콜백과 함께 관찰자를 만든다.
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the observer object / 관찰자 객체
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                JsValue cb = Js.arg(args, 0);
                if (!cb.isCallable()) {
                    throw JsException.typeError("Failed to construct 'ResizeObserver': The callback provided as parameter 1 is not a function.");
                }
                Observer o = new Observer();
                o.callback = (JsObject) cb;
                o.self = new ObserverObject(Ops.getPrototypeFromConstructor(r, newTarget, proto), o);
                return o.self;
            }
        });
        ctor.defineData("prototype", proto, false, false, false);
        proto.defineBuiltin("constructor", ctor);
        proto.defineOwnProperty(WellKnownSymbols.TO_STRING_TAG, org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor.data(Js.str("ResizeObserver"), false, false, true));
        realm.method(proto, "observe", 1, new NativeFunction.Call() {
            /**
             * Observes a target (an observation that exists is replaced).
             * <p>
             * 대상을 관찰한다(있던 관찰은 바뀐다).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Observer o = observerOf(self);
                Element target = elementArg(args, "observe");
                int box = boxOption(Js.arg(args, 1));
                removeTarget(o, target);
                Observation obs = new Observation();
                obs.target = target;
                obs.box = box;
                o.targets.add(obs);
                if (!observers.contains(o)) {
                    observers.add(o);
                }
                // 새로 관찰하는 대상은 다음 렌더링 단계에서 처음 크기를 보고한다.
                owner.requestUpdate();
                return Js.UNDEFINED;
            }
        });
        realm.method(proto, "unobserve", 1, new NativeFunction.Call() {
            /**
             * Stops observing a target.
             * <p>
             * 대상 관찰을 멈춘다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                removeTarget(observerOf(self), elementArg(args, "unobserve"));
                return Js.UNDEFINED;
            }
        });
        realm.method(proto, "disconnect", 0, new NativeFunction.Call() {
            /**
             * Stops observing everything.
             * <p>
             * 모든 관찰을 멈춘다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Observer o = observerOf(self);
                o.targets.clear();
                o.active.clear();
                o.skipped.clear();
                observers.remove(o);
                return Js.UNDEFINED;
            }
        });
        g.defineBuiltin("ResizeObserver", ctor);
    }

    /**
     * Installs an interface whose objects the page cannot construct (<code>Illegal constructor</code>) and whose attributes are getters of the prototype that read
     * the values of a {@link ValueObject}.
     * <p>
     * 페이지가 만들 수 없는(<code>Illegal constructor</code>) 객체의 인터페이스를 설치한다. 속성은 {@link ValueObject} 의 값을 읽는 프로토타입의 게터이다.
     *
     * @param g          the global object / 전역 객체
     * @param realm      the realm / realm
     * @param name       the interface name / 인터페이스 이름
     * @param attributes the attribute names / 속성 이름
     * @return the prototype / 프로토타입
     */
    private JsObject valueInterface(JsObject g, JsRealm realm, final String name, String[] attributes) {
        final JsObject proto = realm.newObject();
        NativeFunction ctor = new NativeFunction(realm, name, 0, new NativeFunction.Call() {
            /**
             * The interface cannot be constructed.
             * <p>
             * 인터페이스는 만들 수 없다.
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
             * The interface cannot be constructed.
             * <p>
             * 인터페이스는 만들 수 없다.
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
        ctor.defineData("prototype", proto, false, false, false);
        proto.defineBuiltin("constructor", ctor);
        proto.defineOwnProperty(WellKnownSymbols.TO_STRING_TAG, org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor.data(Js.str(name), false, false, true));
        // 속성마다 프로토타입에 게터를 정의한다.
        for (int i = 0; i < attributes.length; i++) {
            final int index = i;
            owner.getter(proto, attributes[i], new NativeFunction.Call() {
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
                    if (!(self instanceof ValueObject)) {
                        throw JsException.typeError("Illegal invocation");
                    }
                    return ((ValueObject) self).values[index];
                }
            });
        }
        g.defineBuiltin(name, ctor);
        return proto;
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
            throw JsException.typeError("Failed to execute '" + what + "' on 'ResizeObserver': parameter 1 is not of type 'Element'.");
        }
        return e;
    }

    /**
     * Reads the <code>box</code> of the options of <code>observe</code>.
     * <p>
     * <code>observe</code> 의 옵션에서 <code>box</code> 를 읽는다.
     *
     * @param options the options value / 옵션 값
     * @return the index into {@link #BOXES} / {@link #BOXES} 의 인덱스
     */
    private static int boxOption(JsValue options) {
        if (!options.isObject()) {
            return 0;
        }
        JsValue box = ((JsObject) options).get("box");
        if (box.isUndefined()) {
            return 0;
        }
        String text = Ops.toString(box);
        // 열거형 값 셋 중 하나여야 한다.
        for (int i = 0; i < BOXES.length; i++) {
            if (BOXES[i].equals(text)) {
                return i;
            }
        }
        throw JsException.typeError("Failed to execute 'observe' on 'ResizeObserver': Failed to read the 'box' property from 'ResizeObserverOptions': The provided value '"
                + text + "' is not a valid enum value of type ResizeObserverBoxOptions.");
    }

    /**
     * Removes the observation of a target if there is one.
     * <p>
     * 대상의 관찰이 있으면 제거한다.
     *
     * @param o      the observer / 관찰자
     * @param target the target / 대상
     */
    private static void removeTarget(Observer o, Element target) {
        // 같은 대상의 관찰을 찾아 대상 목록과 활성 목록에서 뺀다.
        for (int i = o.targets.size() - 1; i >= 0; i--) {
            if (o.targets.get(i).target == target) {
                Observation obs = o.targets.remove(i);
                o.active.remove(obs);
                o.skipped.remove(obs);
            }
        }
    }

    /**
     * Returns the depth of a node: the number of nodes on the path from the node to the root of its tree.
     * <p>
     * 노드의 깊이를 반환한다: 노드에서 트리의 루트까지 경로의 노드 수.
     *
     * @param node the node / 노드
     * @return the depth / 깊이
     */
    private static int depth(Node node) {
        int d = 0;
        // 부모를 따라 루트까지 센다.
        for (Node n = node; n != null; n = n.getParentNode()) {
            d++;
        }
        return d;
    }

    /**
     * Measures the boxes of an element: the sizes of the three observable boxes and the padding that places the content rectangle.
     * <p>
     * 요소의 상자를 잰다: 관찰할 수 있는 세 상자의 크기와 content 사각형의 위치를 정하는 padding.
     *
     * @param e the element / 요소
     * @return the sizes / 크기
     */
    private Sizes measure(Element e) {
        Sizes s = new Sizes();
        if (!owner.isRendered(e)) {
            return s;
        }
        ComputedStyle cs = owner.style(e);
        // 치환되지 않은 인라인 요소는 빈 상자이다.
        if (cs != null && "inline".equals(cs.keyword(Prop.DISPLAY)) && !REPLACED.contains(e.getLocalName().toLowerCase(java.util.Locale.ENGLISH))) {
            return s;
        }
        double[] b = owner.bounds(e);
        double base = b[2];
        Element parent = e.getParentElement();
        // 백분율 padding 은 부모 상자의 폭에 대한 비율이다.
        if (parent != null) {
            base = owner.bounds(parent)[2];
        }
        double pl = 0;
        double pr = 0;
        double pt = 0;
        double pb = 0;
        double bl = 0;
        double br = 0;
        double bt = 0;
        double bb = 0;
        boolean vertical = false;
        // 계산 스타일이 있으면 padding 과 border 두께와 쓰기 방향을 읽는다.
        if (cs != null) {
            pl = number(cs.resolve(Prop.PADDING_LEFT, base));
            pr = number(cs.resolve(Prop.PADDING_RIGHT, base));
            pt = number(cs.resolve(Prop.PADDING_TOP, base));
            pb = number(cs.resolve(Prop.PADDING_BOTTOM, base));
            bl = number(cs.px(Prop.BORDER_LEFT_WIDTH));
            br = number(cs.px(Prop.BORDER_RIGHT_WIDTH));
            bt = number(cs.px(Prop.BORDER_TOP_WIDTH));
            bb = number(cs.px(Prop.BORDER_BOTTOM_WIDTH));
            String wm = cs.keyword(Prop.WRITING_MODE);
            vertical = wm != null && (wm.startsWith("vertical") || wm.startsWith("sideways"));
        }
        s.paddingLeft = pl;
        s.paddingTop = pt;
        s.contentWidth = Math.max(0, b[2] - pl - pr - bl - br);
        s.contentHeight = Math.max(0, b[3] - pt - pb - bt - bb);
        double ratio = devicePixelRatio();
        s.size[0] = logical(s.contentWidth, s.contentHeight, vertical);
        s.size[1] = logical(b[2], b[3], vertical);
        s.size[2] = logical(Math.round(s.contentWidth * ratio), Math.round(s.contentHeight * ratio), vertical);
        return s;
    }

    /**
     * Returns the inline and block size for a width and a height (swapped in a vertical writing mode).
     * <p>
     * 폭과 높이에 대한 인라인 크기와 블록 크기를 반환한다(세로쓰기에서는 바뀐다).
     *
     * @param width    the width / 폭
     * @param height   the height / 높이
     * @param vertical true in a vertical writing mode / 세로쓰기이면 true
     * @return inline size and block size / 인라인 크기와 블록 크기
     */
    private static double[] logical(double width, double height, boolean vertical) {
        return vertical ? new double[] { height, width } : new double[] { width, height };
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
     * Reads <code>window.devicePixelRatio</code> (1 if the page has none).
     * <p>
     * <code>window.devicePixelRatio</code> 를 읽는다(없으면 1).
     *
     * @return the ratio / 비율
     */
    private double devicePixelRatio() {
        JsValue v = page.getRealm().getGlobalObject().get("devicePixelRatio");
        double d = Ops.toNumber(v);
        return Double.isNaN(d) || d <= 0 ? 1 : d;
    }

    /**
     * Tells whether the size of the observed box differs from the size that was reported last (<code>isActive()</code>).
     * <p>
     * 관찰하는 상자의 크기가 마지막으로 보고한 크기와 다른지 알려 준다(<code>isActive()</code>).
     *
     * @param obs the observation / 관찰
     * @return true if it is active / 활성이면 true
     */
    private boolean isActive(Observation obs) {
        double[] now = measure(obs.target).size[obs.box];
        return Math.abs(now[0] - obs.lastInline) > 1e-6 || Math.abs(now[1] - obs.lastBlock) > 1e-6;
    }

    /**
     * Runs the loop of the resize observers: gather the active observations that are deeper than a depth, broadcast them, and repeat with the depth of the
     * shallowest broadcast target, then report the observations that stayed behind.
     * <p>
     * 크기 관찰자의 루프를 실행한다: 어떤 깊이보다 깊은 활성 관찰을 모아 방송하고, 방송한 가장 얕은 대상의 깊이로 되풀이한 뒤 남은 관찰을 보고한다.
     */
    void run() {
        if (observers.isEmpty() || !hasTargets()) {
            return;
        }
        int depth = 0;
        gather(depth);
        // 활성 관찰이 없어질 때까지 방송하고 더 깊은 것을 다시 모은다(깊이가 늘어나므로 끝난다).
        while (hasActive()) {
            depth = broadcast();
            gather(depth);
        }
        if (hasSkipped()) {
            page.reportError(JsException.typeError("ResizeObserver loop completed with undelivered notifications."));
            // 남은 관찰은 다음 단계에서 다시 본다.
            owner.requestUpdate();
        }
    }

    /**
     * "Gather active observations at depth": fills the active list with the changed targets deeper than the depth and the skipped list with the others.
     * <p>
     * "Gather active observations at depth": 깊이보다 깊은 바뀐 대상은 활성 목록에, 나머지는 건너뛴 목록에 채운다.
     *
     * @param depth the depth / 깊이
     */
    private void gather(int depth) {
        // 관찰자마다 두 목록을 비우고 대상을 검사한다.
        for (Observer o : new ArrayList<Observer>(observers)) {
            o.active.clear();
            o.skipped.clear();
            // 대상마다 바뀌었는지 보고 깊이에 따라 활성 또는 건너뜀 목록에 넣는다.
            for (Observation obs : o.targets) {
                if (isActive(obs)) {
                    if (depth(obs.target) > depth) {
                        o.active.add(obs);
                    } else {
                        o.skipped.add(obs);
                    }
                }
            }
        }
    }

    /**
     * Tells whether some observer has an active observation.
     * <p>
     * 어떤 관찰자에 활성 관찰이 있는지 알려 준다.
     *
     * @return true if one has / 있으면 true
     */
    private boolean hasActive() {
        // 활성 목록이 비어 있지 않은 관찰자를 찾는다.
        for (int i = 0; i < observers.size(); i++) {
            if (!observers.get(i).active.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether some observer has a skipped observation.
     * <p>
     * 어떤 관찰자에 건너뛴 관찰이 있는지 알려 준다.
     *
     * @return true if one has / 있으면 true
     */
    private boolean hasSkipped() {
        // 건너뛴 목록이 비어 있지 않은 관찰자를 찾는다.
        for (int i = 0; i < observers.size(); i++) {
            if (!observers.get(i).skipped.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * "Broadcast active observations": delivers the entries of the active observations to the callbacks.
     * <p>
     * "Broadcast active observations": 활성 관찰의 항목을 콜백에 전달한다.
     *
     * @return the depth of the shallowest target that was delivered / 전달한 가장 얕은 대상의 깊이
     */
    private int broadcast() {
        int shallowest = Integer.MAX_VALUE;
        // 활성 관찰이 있는 관찰자마다 항목을 만들어 콜백을 부른다.
        for (Observer o : new ArrayList<Observer>(observers)) {
            if (o.active.isEmpty()) {
                continue;
            }
            List<JsValue> entries = new ArrayList<JsValue>();
            List<Observation> delivered = new ArrayList<Observation>(o.active);
            // 활성 관찰마다 현재 크기로 항목을 만들고 보고한 크기를 기억한다.
            for (Observation obs : delivered) {
                Sizes s = measure(obs.target);
                entries.add(entry(obs.target, s));
                obs.lastInline = s.size[obs.box][0];
                obs.lastBlock = s.size[obs.box][1];
                shallowest = Math.min(shallowest, depth(obs.target));
            }
            try {
                Ops.call(o.callback, o.self, new JsValue[] { page.getRealm().newArray(entries), o.self });
            } catch (JsException e) {
                page.reportError(e);
            }
            o.active.clear();
        }
        return shallowest;
    }

    /**
     * "Create and populate a ResizeObserverEntry".
     * <p>
     * "Create and populate a ResizeObserverEntry".
     *
     * @param target the target / 대상
     * @param s      its sizes / 크기
     * @return the entry / 항목
     */
    private JsValue entry(Element target, Sizes s) {
        DomBridge b = page.getBridge();
        JsValue[] values = new JsValue[5];
        values[0] = b.toJs(target);
        values[1] = b.toJs(new DomRectReadOnly(s.paddingLeft, s.paddingTop, s.contentWidth, s.contentHeight));
        values[2] = sizeList(s.size[1]);
        values[3] = sizeList(s.size[0]);
        values[4] = sizeList(s.size[2]);
        return new ValueObject(entryProto, values);
    }

    /**
     * Builds the frozen sequence with one <code>ResizeObserverSize</code>.
     * <p>
     * <code>ResizeObserverSize</code> 하나를 가진 얼린 시퀀스를 만든다.
     *
     * @param size inline and block size / 인라인 크기와 블록 크기
     * @return the array / 배열
     */
    private JsValue sizeList(double[] size) {
        List<JsValue> items = new ArrayList<JsValue>();
        items.add(new ValueObject(sizeProto, new JsValue[] { Js.num(size[0]), Js.num(size[1]) }));
        return owner.freeze(page.getRealm().newArray(items));
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
