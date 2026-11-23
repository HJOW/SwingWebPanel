package org.duckdns.hjow.swingwebpanel.js.host;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.duckdns.hjow.swingwebpanel.dom.MutationRecord;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;

/**
 * <code>MutationObserver</code> of one page (plan item P15-11): the observers register for a node and a set of change kinds
 * (<code>childList</code>, <code>attributes</code>, <code>characterData</code>, <code>subtree</code>, <code>attributeOldValue</code>,
 * <code>characterDataOldValue</code>, <code>attributeFilter</code>); every DOM change is queued as a record for the observers that match,
 * and the records are delivered to the callbacks at the next microtask checkpoint (after the running script, or after the task). Changes
 * made by a callback are delivered in the same checkpoint. Differences from the standard: <code>addedNodes</code> and
 * <code>removedNodes</code> are arrays, and there are no transient registrations, so a removed subtree stops reporting at once.
 * <p>
 * 한 페이지의 <code>MutationObserver</code>(계획 P15-11): 관찰자는 노드와 변경 종류 집합(<code>childList</code>, <code>attributes</code>,
 * <code>characterData</code>, <code>subtree</code>, <code>attributeOldValue</code>, <code>characterDataOldValue</code>,
 * <code>attributeFilter</code>)을 등록하고, 모든 DOM 변경은 맞는 관찰자의 기록으로 대기열에 들어가며 다음 마이크로태스크 검사점(실행 중인 스크립트나
 * 작업이 끝난 뒤)에 콜백으로 전달된다. 콜백이 만든 변경도 같은 검사점에서 전달된다. 표준과의 차이: <code>addedNodes</code>, <code>removedNodes</code> 는
 * 배열이고 임시 등록이 없어 제거된 하위 트리는 즉시 보고를 멈춘다.
 */
final class MutationObservers {

    /**
     * What one observer watches on one node.
     * <p>
     * 관찰자 하나가 노드 하나에서 보는 것.
     */
    private static final class Registration {
        /**
         * The observed node.
         * <p>
         * 관찰하는 노드.
         */
        Node target;
        /**
         * Whether child list changes are reported.
         * <p>
         * 자식 목록 변경을 보고하는지 여부.
         */
        boolean childList;
        /**
         * Whether attribute changes are reported.
         * <p>
         * 속성 변경을 보고하는지 여부.
         */
        boolean attributes;
        /**
         * Whether character data changes are reported.
         * <p>
         * 문자 데이터 변경을 보고하는지 여부.
         */
        boolean characterData;
        /**
         * Whether the whole subtree is observed.
         * <p>
         * 하위 트리 전체를 관찰하는지 여부.
         */
        boolean subtree;
        /**
         * Whether the old attribute value is recorded.
         * <p>
         * 이전 속성 값을 기록하는지 여부.
         */
        boolean attributeOldValue;
        /**
         * Whether the old character data is recorded.
         * <p>
         * 이전 문자 데이터를 기록하는지 여부.
         */
        boolean characterDataOldValue;
        /**
         * The attribute names to report, or null for all.
         * <p>
         * 보고할 속성 이름. 모두이면 null.
         */
        Set<String> attributeFilter;
    }

    /**
     * One observer.
     * <p>
     * 관찰자 하나.
     */
    private static final class Observer {
        /**
         * The callback function.
         * <p>
         * 콜백 함수.
         */
        JsObject callback;
        /**
         * The script object of the observer.
         * <p>
         * 관찰자의 스크립트 객체.
         */
        JsObject self;
        /**
         * The registrations.
         * <p>
         * 등록 목록.
         */
        final List<Registration> registrations = new ArrayList<Registration>();
        /**
         * The queued records as script objects.
         * <p>
         * 스크립트 객체로 만든 대기 중인 기록.
         */
        final List<JsValue> queue = new ArrayList<JsValue>();
    }

    /**
     * The page.
     * <p>
     * 페이지.
     */
    private final PageScripts page;

    /**
     * All observers that have registrations or queued records.
     * <p>
     * 등록이나 대기 기록이 있는 모든 관찰자.
     */
    private final List<Observer> observers = new ArrayList<Observer>();

    /**
     * The prototype of the records (the interface <code>MutationRecord</code>).
     * <p>
     * 기록의 프로토타입(<code>MutationRecord</code> 인터페이스).
     */
    private JsObject recordProto;

    /**
     * True while records are delivered (a delivery does not nest).
     * <p>
     * 기록을 전달하는 동안 true(전달은 중첩되지 않는다).
     */
    private boolean delivering;

    /**
     * Creates the helper.
     * <p>
     * 도우미를 만든다.
     *
     * @param page the page / 페이지
     */
    MutationObservers(PageScripts page) {
        this.page = page;
    }

    /**
     * Installs the <code>MutationObserver</code> constructor on the global object.
     * <p>
     * 전역 객체에 <code>MutationObserver</code> 생성자를 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    void install(JsObject g) {
        final JsRealm realm = page.getRealm();
        recordProto = InterfaceNames.define(page, g, "MutationRecord");
        final JsObject proto = realm.newObject();
        NativeFunction ctor = new NativeFunction(realm, "MutationObserver", 1, new NativeFunction.Call() {
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
                throw JsException.typeError("Failed to construct 'MutationObserver': Please use the 'new' operator.");
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
                    throw JsException.typeError("Failed to construct 'MutationObserver': The callback provided as parameter 1 is not a "
                            + "function.");
                }
                Observer o = new Observer();
                o.callback = (JsObject) cb;
                o.self = new ObserverObject(Ops.getPrototypeFromConstructor(r, newTarget, proto), o);
                return o.self;
            }
        });
        ctor.defineData("prototype", proto, false, false, false);
        proto.defineBuiltin("constructor", ctor);
        realm.method(proto, "observe", 1, new NativeFunction.Call() {
            /**
             * Starts observing a node.
             * <p>
             * 노드 관찰을 시작한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                observe(observerOf(self), Js.arg(args, 0), Js.arg(args, 1));
                return Js.UNDEFINED;
            }
        });
        realm.method(proto, "disconnect", 0, new NativeFunction.Call() {
            /**
             * Stops observing and drops the queued records.
             * <p>
             * 관찰을 멈추고 대기 중인 기록을 버린다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Observer o = observerOf(self);
                o.registrations.clear();
                o.queue.clear();
                observers.remove(o);
                return Js.UNDEFINED;
            }
        });
        realm.method(proto, "takeRecords", 0, new NativeFunction.Call() {
            /**
             * Returns and clears the queued records.
             * <p>
             * 대기 중인 기록을 반환하고 비운다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the records / 기록
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Observer o = observerOf(self);
                JsValue arr = r.newArray(new ArrayList<JsValue>(o.queue));
                o.queue.clear();
                return arr;
            }
        });
        g.defineBuiltin("MutationObserver", ctor);
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
     * Reads a boolean option.
     * <p>
     * 불리언 옵션을 읽는다.
     *
     * @param o the options object / 옵션 객체
     * @param n the member / 멤버
     * @return the flag / 플래그
     */
    private static boolean opt(JsObject o, String n) {
        return Ops.toBoolean(o.get(n));
    }

    /**
     * Implements <code>observe(target, options)</code>.
     * <p>
     * <code>observe(target, options)</code> 를 구현한다.
     *
     * @param o       the observer / 관찰자
     * @param target  the target value / 대상 값
     * @param options the options value / 옵션 값
     */
    private void observe(Observer o, JsValue target, JsValue options) {
        Node node = (Node) DomBridge.targetOf(target, Node.class);
        if (node == null) {
            throw JsException.typeError("Failed to execute 'observe' on 'MutationObserver': parameter 1 is not of type 'Node'.");
        }
        if (!options.isObject()) {
            throw JsException.typeError("Failed to execute 'observe' on 'MutationObserver': The options object must set at least one of "
                    + "'attributes', 'characterData', or 'childList' to true.");
        }
        JsObject op = (JsObject) options;
        Registration r = new Registration();
        r.target = node;
        r.childList = opt(op, "childList");
        r.subtree = opt(op, "subtree");
        JsValue a = op.get("attributes");
        JsValue cd = op.get("characterData");
        r.attributeOldValue = opt(op, "attributeOldValue");
        r.characterDataOldValue = opt(op, "characterDataOldValue");
        JsValue filter = op.get("attributeFilter");
        r.attributes = !a.isUndefined() ? Ops.toBoolean(a) : (r.attributeOldValue || filter.isObject());
        r.characterData = !cd.isUndefined() ? Ops.toBoolean(cd) : r.characterDataOldValue;
        if (filter.isObject()) {
            r.attributeFilter = new HashSet<String>();
            JsObject fo = (JsObject) filter;
            int n = (int) Ops.lengthOf(fo);
            // 속성 이름 필터를 배열에서 읽는다.
            for (int i = 0; i < n; i++) {
                r.attributeFilter.add(Ops.toString(fo.get(String.valueOf(i))));
            }
        }
        if (!r.childList && !r.attributes && !r.characterData) {
            throw JsException.typeError("Failed to execute 'observe' on 'MutationObserver': The options object must set at least one of "
                    + "'attributes', 'characterData', or 'childList' to true.");
        }
        if ((r.attributeOldValue && !r.attributes) || (r.attributeFilter != null && !r.attributes)
                || (r.characterDataOldValue && !r.characterData)) {
            throw JsException.typeError("Failed to execute 'observe' on 'MutationObserver': The options object is inconsistent.");
        }
        // 같은 노드의 이전 등록은 새 등록으로 바꾼다.
        for (int i = o.registrations.size() - 1; i >= 0; i--) {
            if (o.registrations.get(i).target == node) {
                o.registrations.remove(i);
            }
        }
        o.registrations.add(r);
        if (!observers.contains(o)) {
            observers.add(o);
        }
    }

    /**
     * Queues a DOM change for every observer that watches it.
     * <p>
     * 변경을 지켜보는 모든 관찰자의 대기열에 DOM 변경을 넣는다.
     *
     * @param rec the change / 변경
     */
    void record(MutationRecord rec) {
        if (observers.isEmpty()) {
            return;
        }
        // 관찰자마다 등록을 훑어 변경과 맞는 첫 등록으로 기록을 만든다.
        for (int i = 0; i < observers.size(); i++) {
            Observer o = observers.get(i);
            Registration hit = null;
            // 등록을 차례로 보며 변경과 맞는 첫 등록을 찾는다.
            for (int k = 0; k < o.registrations.size() && hit == null; k++) {
                if (matches(o.registrations.get(k), rec)) {
                    hit = o.registrations.get(k);
                }
            }
            if (hit != null) {
                o.queue.add(toJs(rec, hit));
            }
        }
    }

    /**
     * Tells whether a registration watches a change.
     * <p>
     * 등록이 변경을 지켜보는지 알려 준다.
     *
     * @param r   the registration / 등록
     * @param rec the change / 변경
     * @return true if it matches / 맞으면 true
     */
    private static boolean matches(Registration r, MutationRecord rec) {
        Node t = rec.getTarget();
        if (t != r.target && !(r.subtree && r.target.contains(t))) {
            return false;
        }
        if (rec.getType() == MutationRecord.CHILD_LIST) {
            return r.childList;
        } else if (rec.getType() == MutationRecord.ATTRIBUTES) {
            if (!r.attributes) {
                return false;
            }
            return r.attributeFilter == null || (rec.getAttributeNamespace() == null && r.attributeFilter.contains(rec.getAttributeName()));
        }
        return r.characterData;
    }

    /**
     * Builds the script object of a record.
     * <p>
     * 기록의 스크립트 객체를 만든다.
     *
     * @param rec the change / 변경
     * @param r   the matching registration / 맞는 등록
     * @return the record object / 기록 객체
     */
    private JsValue toJs(MutationRecord rec, Registration r) {
        DomBridge b = page.getBridge();
        JsObject o = new JsObject(recordProto);
        o.createDataProperty("type", Js.str(rec.getTypeName()));
        o.createDataProperty("target", b.toJs(rec.getTarget()));
        o.createDataProperty("addedNodes", list(rec.getAddedNodes()));
        o.createDataProperty("removedNodes", list(rec.getRemovedNodes()));
        o.createDataProperty("previousSibling", b.toJs(rec.getPreviousSibling()));
        o.createDataProperty("nextSibling", b.toJs(rec.getNextSibling()));
        o.createDataProperty("attributeName", rec.getAttributeName() == null ? Js.NULL : Js.str(rec.getAttributeName()));
        o.createDataProperty("attributeNamespace", rec.getAttributeNamespace() == null ? Js.NULL : Js.str(rec.getAttributeNamespace()));
        boolean old = rec.getType() == MutationRecord.ATTRIBUTES ? r.attributeOldValue : r.characterDataOldValue;
        o.createDataProperty("oldValue", old && rec.getOldValue() != null ? Js.str(rec.getOldValue()) : Js.NULL);
        return o;
    }

    /**
     * Converts a node list to a script array.
     * <p>
     * 노드 목록을 스크립트 배열로 바꾼다.
     *
     * @param nodes the nodes / 노드
     * @return the array / 배열
     */
    private JsValue list(List<Node> nodes) {
        List<JsValue> items = new ArrayList<JsValue>();
        // 노드를 하나씩 래퍼로 바꿔 모은다.
        for (int i = 0; i < nodes.size(); i++) {
            items.add(page.getBridge().toJs(nodes.get(i)));
        }
        return page.getRealm().newArray(items);
    }

    /**
     * Delivers the queued records to the observers' callbacks (a microtask checkpoint step).
     * <p>
     * 대기 중인 기록을 관찰자의 콜백에 전달한다(마이크로태스크 검사점의 한 단계).
     *
     * @return true if at least one callback ran / 콜백이 하나라도 실행되었으면 true
     */
    boolean deliver() {
        if (delivering || observers.isEmpty()) {
            return false;
        }
        delivering = true;
        boolean any = false;
        try {
            // 기록이 있는 관찰자마다 기록을 꺼내 콜백을 부른다.
            for (Observer o : new ArrayList<Observer>(observers)) {
                if (o.queue.isEmpty()) {
                    continue;
                }
                JsValue records = page.getRealm().newArray(new ArrayList<JsValue>(o.queue));
                o.queue.clear();
                any = true;
                try {
                    Ops.call(o.callback, o.self, new JsValue[] { records, o.self });
                } catch (JsException e) {
                    page.reportError(e);
                }
            }
        } finally {
            delivering = false;
        }
        return any;
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
