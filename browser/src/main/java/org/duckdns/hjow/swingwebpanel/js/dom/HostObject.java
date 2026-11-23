package org.duckdns.hjow.swingwebpanel.js.dom;

import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.Property;

/**
 * The JavaScript object that stands for one Java host object (a DOM node, an event, a collection ...). It keeps the Java target and tells
 * the bridge when it receives own properties, because a wrapper that holds script-defined data must stay alive as long as its target (the
 * wrapper cache holds wrappers weakly otherwise). Wrappers are created only by {@link DomBridge}; scripts can never reach the Java class.
 * <p>
 * Java 호스트 객체(DOM 노드, 이벤트, 컬렉션 등) 하나를 나타내는 JavaScript 객체. Java 대상을 보관하고, 자체 프로퍼티를 받으면 브리지에 알린다.
 * 스크립트가 정의한 데이터를 가진 래퍼는 대상과 같은 기간 살아 있어야 하기 때문이다(래퍼 캐시는 그 밖의 경우 래퍼를 약하게 보관한다).
 * 래퍼는 {@link DomBridge} 만 만들며 스크립트는 Java 클래스에 접근할 수 없다.
 */
public class HostObject extends JsObject {

    /**
     * The Java object behind this wrapper.
     * <p>
     * 이 래퍼 뒤의 Java 객체.
     */
    private final Object target;

    /**
     * The bridge that owns this wrapper (null for objects created in tests without a bridge).
     * <p>
     * 이 래퍼를 소유한 브리지(브리지 없는 시험에서 만든 객체는 null).
     */
    private final DomBridge bridge;

    /**
     * Whether the bridge was already told to keep this wrapper alive.
     * <p>
     * 브리지에 이 래퍼를 계속 살려 두라고 이미 알렸는지 여부.
     */
    private boolean pinned;

    /**
     * Creates a wrapper.
     * <p>
     * 래퍼를 만든다.
     *
     * @param bridge the owning bridge / 소유한 브리지
     * @param proto  prototype object / 프로토타입 객체
     * @param target the Java object / Java 객체
     */
    public HostObject(DomBridge bridge, JsObject proto, Object target) {
        super(proto);
        this.bridge = bridge;
        this.target = target;
    }

    /**
     * Returns the Java object behind this wrapper.
     * <p>
     * 이 래퍼 뒤의 Java 객체를 반환한다.
     *
     * @return the target / 대상
     */
    public Object getTarget() {
        return target;
    }

    /**
     * Returns the bridge that owns this wrapper.
     * <p>
     * 이 래퍼를 소유한 브리지를 반환한다.
     *
     * @return the bridge / 브리지
     */
    public DomBridge getBridge() {
        return bridge;
    }

    /**
     * Pins the wrapper when the first own property is stored, so that script-defined data is not lost when the wrapper would otherwise be
     * collected.
     * <p>
     * 첫 자체 프로퍼티를 저장할 때 래퍼를 고정해, 스크립트가 정의한 데이터가 래퍼 수거로 사라지지 않게 한다.
     *
     * @param key the property key / 프로퍼티 키
     * @param p   the property / 프로퍼티
     */
    protected void storeProperty(Object key, Property p) {
        super.storeProperty(key, p);
        if (!pinned && bridge != null) {
            pinned = true;
            bridge.pin(this);
        }
    }

    /**
     * Returns the interface name for diagnostics.
     * <p>
     * 진단용 인터페이스 이름을 반환한다.
     *
     * @return class name / 클래스 이름
     */
    public String className() {
        return "Object";
    }
}
