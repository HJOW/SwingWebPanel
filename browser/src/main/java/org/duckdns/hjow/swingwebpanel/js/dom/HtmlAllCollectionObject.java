package org.duckdns.hjow.swingwebpanel.js.dom;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.AllElements;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.Property;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.PropertyKeys;

/**
 * The callable legacy platform object returned by document.all.
 * <p>
 * document.all이 반환하는 호출 가능한 레거시 플랫폼 객체.
 */
final class HtmlAllCollectionObject extends HostObject {

    /** The live index and name data. / 라이브 인덱스와 이름 데이터. */
    private final AllElements elements;

    /**
     * Wraps the legacy collection data of a document.
     * <p>
     * 문서의 레거시 컬렉션 데이터를 래핑한다.
     *
     * @param bridge the owning bridge / 소유하는 브리지
     * @param proto the interface prototype / 인터페이스 프로토타입
     * @param elements the legacy collection / 레거시 컬렉션
     */
    HtmlAllCollectionObject(DomBridge bridge, JsObject proto, AllElements elements) {
        super(bridge, proto, elements);
        this.elements = elements;
    }

    /**
     * Installs the interface and its wrapper factory in the window realm.
     * <p>
     * 창 렐름에 인터페이스와 래퍼 생성기를 설치한다.
     *
     * @param bridge the bridge / 브리지
     */
    static void install(final DomBridge bridge) {
        DomBridge.InterfaceInfo info = bridge.defineInterface("HTMLAllCollection", null, AllElements.class,
                new String[] { "a:length", "m:namedItem(String)" });
        info.getPrototype().defineData("item", new NativeFunction(bridge.getRealm(), "item", 0, new NativeFunction.Call() {
            /**
             * Resolves the optional item argument on a branded receiver.
             * <p>
             * 올바른 수신자에서 선택적 item 인수를 해석한다.
             *
             * @param realm the realm / 렐름
             * @param self the receiver / 수신자
             * @param args the arguments / 인수
             * @return the lookup result / 조회 결과
             */
            public JsValue call(JsRealm realm, JsValue self, JsValue[] args) {
                if (!(self instanceof HtmlAllCollectionObject)) {
                    throw JsException.typeError("HTMLAllCollection.item called on an incompatible receiver");
                }
                return ((HtmlAllCollectionObject) self).lookup(args);
            }
        }), true, true, true);
        bridge.setFactory("HTMLAllCollection", new DomBridge.Factory() {
            /**
             * Creates the collection wrapper.
             * <p>
             * 컬렉션 래퍼를 만든다.
             *
             * @param owner the bridge / 브리지
             * @param proto the prototype / 프로토타입
             * @param target the live collection / 라이브 컬렉션
             * @return the wrapper / 래퍼
             */
            public HostObject create(DomBridge owner, JsObject proto, Object target) {
                return new HtmlAllCollectionObject(owner, proto, (AllElements) target);
            }
        });
    }

    /**
     * Reports the Annex B slot of this particular host object.
     * <p>
     * 이 호스트 객체의 부속서 B 슬롯을 보고한다.
     *
     * @return true / true
     */
    public boolean hasIsHtmlDda() {
        return true;
    }

    /**
     * Allows the legacy collection itself to be called.
     * <p>
     * 레거시 컬렉션 자체를 호출할 수 있게 한다.
     *
     * @return true / true
     */
    public boolean isCallable() {
        return true;
    }

    /**
     * Performs a lookup on this collection regardless of the receiver.
     * <p>
     * 수신자와 무관하게 이 컬렉션에서 조회한다.
     *
     * @param receiver the unused receiver / 사용하지 않는 수신자
     * @param args the arguments / 인수
     * @return the lookup result / 조회 결과
     */
    public JsValue call(JsValue receiver, JsValue[] args) {
        return lookup(args);
    }

    /**
     * Resolves the optional DOMString argument as an index or name.
     * <p>
     * 선택적 DOMString 인수를 인덱스 또는 이름으로 조회한다.
     *
     * @param args the arguments / 인수
     * @return a node, collection, or null / 노드, 컬렉션 또는 null
     */
    private JsValue lookup(JsValue[] args) {
        if (args.length == 0 || args[0].isUndefined()) {
            return Js.NULL;
        }
        String name = Ops.toString(args[0]);
        long index = PropertyKeys.arrayIndex(name);
        // 정규 배열 인덱스이면 범위 밖에서도 이름 조회로 바꾸지 않는다.
        if (index >= 0) {
            return index < elements.getLength() ? getBridge().toJs(elements.item((int) index)) : Js.NULL;
        }
        return getBridge().toJs(elements.namedItem(name));
    }

    /**
     * Returns a property slot, including indices and visible named properties.
     * <p>
     * 인덱스와 보이는 이름을 포함해 프로퍼티 슬롯을 반환한다.
     *
     * @param key the property key / 프로퍼티 키
     * @return the slot, or null if absent / 슬롯 또는 없으면 null
     */
    public Property getOwnPropertySlot(Object key) {
        if (!(key instanceof String)) {
            return super.getOwnPropertySlot(key);
        }
        long index = PropertyKeys.arrayIndex((String) key);
        // 지원하는 인덱스 속성은 읽기 전용이며 열거할 수 있다.
        if (index >= 0) {
            return index < elements.getLength() ? Property.data(getBridge().toJs(elements.item((int) index)), false, true, true) : null;
        }
        Property own = super.getOwnPropertySlot(key);
        if (own == null && visibleName((String) key)) {
            return Property.data(getBridge().toJs(elements.namedItem((String) key)), false, false, true);
        }
        return own;
    }

    /**
     * Reports whether a supported name is exposed after ordinary properties.
     * <p>
     * 지원하는 이름이 일반 프로퍼티에 가려지지 않고 노출되는지 확인한다.
     *
     * @param name the candidate name / 후보 이름
     * @return whether the name is visible / 이름이 보이면 true
     */
    private boolean visibleName(String name) {
        if (!elements.supportsName(name) || super.getOwnPropertySlot(name) != null) {
            return false;
        }
        // 프로토타입의 자체 프로퍼티가 같은 이름의 요소를 가린다.
        for (JsObject object = getPrototypeOf(); object != null; object = object.getPrototypeOf()) {
            if (object.getOwnProperty(name) != null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the descriptor of an ordinary or live collection property.
     * <p>
     * 일반 또는 라이브 컬렉션 프로퍼티의 기술자를 반환한다.
     *
     * @param key the property key / 프로퍼티 키
     * @return the descriptor, or null if absent / 기술자 또는 없으면 null
     */
    public PropertyDescriptor getOwnProperty(Object key) {
        Property property = getOwnPropertySlot(key);
        return property == null ? null : property.toDescriptor();
    }

    /**
     * Defines an ordinary property outside the collection's read-only keys.
     * <p>
     * 컬렉션의 읽기 전용 키 이외에는 일반 프로퍼티를 정의한다.
     *
     * @param key the property key / 프로퍼티 키
     * @param descriptor the new descriptor / 새 기술자
     * @return whether the definition succeeded / 정의 성공 여부
     */
    public boolean defineOwnProperty(Object key, PropertyDescriptor descriptor) {
        if (key instanceof String && (PropertyKeys.arrayIndex((String) key) >= 0
                || (elements.supportsName((String) key) && super.getOwnPropertySlot(key) == null))) {
            return false;
        }
        return super.defineOwnProperty(key, descriptor);
    }

    /**
     * Deletes ordinary properties while preserving live entries.
     * <p>
     * 라이브 항목을 유지하며 일반 프로퍼티를 삭제한다.
     *
     * @param key the property key / 프로퍼티 키
     * @return whether deletion succeeded / 삭제 성공 여부
     */
    public boolean delete(Object key) {
        if (key instanceof String) {
            long index = PropertyKeys.arrayIndex((String) key);
            if (index >= 0) {
                return index >= elements.getLength();
            }
            if (visibleName((String) key)) {
                return false;
            }
        }
        return super.delete(key);
    }

    /**
     * Keeps the live platform object extensible.
     * <p>
     * 라이브 플랫폼 객체를 확장 가능한 상태로 유지한다.
     *
     * @return false / false
     */
    public boolean preventExtensions() {
        return false;
    }

    /**
     * Lists indices, visible names in tree order, and ordinary own keys.
     * <p>
     * 인덱스, 트리 순서로 보이는 이름, 일반 자체 키를 나열한다.
     *
     * @return the property keys / 프로퍼티 키 목록
     */
    public List<Object> ownKeys() {
        int length = elements.getLength();
        List<Object> keys = new ArrayList<Object>(length);
        // 문서의 숫자 위치를 먼저 나열한다.
        for (int index = 0; index < length; index++) {
            keys.add(PropertyKeys.fromIndex(index));
        }
        // 숫자 위치와 겹치지 않는 보이는 이름을 트리 순서로 추가한다.
        for (String name : elements.supportedNames()) {
            if (PropertyKeys.arrayIndex(name) < 0 && visibleName(name)) {
                keys.add(name);
            }
        }
        keys.addAll(super.ownKeys());
        return keys;
    }
}
