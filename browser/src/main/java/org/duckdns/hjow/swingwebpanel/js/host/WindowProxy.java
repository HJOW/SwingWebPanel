package org.duckdns.hjow.swingwebpanel.js.host;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Property;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;

/**
 * Script-visible identity of a browsing context. Each operation resolves the current document and its access policy.
 * <p>
 * 스크립트에서 보이는 브라우징 문맥의 객체. 연산마다 현재 문서와 그 문서에 대한 접근 정책을 확인한다.
 */
final class WindowProxy extends JsObject {

    /** Instances by live context, with neither side rooted by the table. / 양쪽 모두 가비지 수거를 막지 않는 문맥별 객체 표. */
    private static final Map<BrowsingContext, WeakReference<WindowProxy>> WINDOWS =
            new WeakHashMap<BrowsingContext, WeakReference<WindowProxy>>();

    /** The context whose visible window is being accessed. / 접근 대상 창의 브라우징 문맥. */
    private final BrowsingContext context;

    /**
     * Creates the identity for a context.
     * <p>
     * 문맥을 가리키는 객체를 만든다.
     *
     * @param context browsing context / 브라우징 문맥
     */
    private WindowProxy(BrowsingContext context) {
        super(null);
        this.context = context;
    }

    /**
     * Finds the stable object for the given context.
     * <p>
     * 주어진 문맥을 계속 가리키는 객체를 찾는다.
     *
     * @param context browsing context, or null for a detached host / 브라우징 문맥. 호스트에 문맥이 없으면 null
     * @return window identity or null / 창 객체 또는 null
     */
    static synchronized WindowProxy forContext(BrowsingContext context) {
        if (context == null) {
            return null;
        }
        WeakReference<WindowProxy> ref = WINDOWS.get(context);
        WindowProxy window = ref == null ? null : ref.get();
        if (window == null) {
            window = new WindowProxy(context);
            WINDOWS.put(context, new WeakReference<WindowProxy>(window));
        }
        return window;
    }

    /**
     * Resolves the target visible to the current caller.
     * <p>
     * 현재 호출자가 볼 수 있는 대상을 확인한다.
     *
     * @return current same-origin window or the restricted cross-origin view / 현재 같은 오리진 창 또는 교차 오리진 제한 객체
     */
    private JsObject target() {
        return PageScripts.windowTarget(context);
    }

    /**
     * Rejects a private element addition on the navigation-stable identity.
     * <p>
     * 이동 후에도 유지되는 창 객체에 private 요소 추가를 거부한다.
     */
    @Override
    public void ensureCanAddPrivateElement() {
        throw JsException.typeError("Cannot add a private element to WindowProxy");
    }

    /** Returns the window class name. / 창의 클래스 이름을 반환한다. */
    @Override
    public String className() {
        return "Window";
    }

    /** Reads through the current target with the original receiver. / 원래의 수신자로 현재 대상의 속성을 읽는다. */
    @Override
    public JsValue get(Object key, JsValue receiver) {
        return target().get(key, receiver);
    }

    /** Writes through the current target with the original receiver. / 원래의 수신자로 현재 대상의 속성을 쓴다. */
    @Override
    public boolean set(Object key, JsValue value, JsValue receiver) {
        return target().set(key, value, receiver);
    }

    /** Tests whether the current target exposes a property. / 현재 대상이 속성을 노출하는지 검사한다. */
    @Override
    public boolean hasProperty(Object key) {
        return target().hasProperty(key);
    }

    /** Tests whether the current target exposes an own property. / 현재 대상이 자체 속성을 노출하는지 검사한다. */
    @Override
    public boolean hasOwnProperty(Object key) {
        return target().hasOwnProperty(key);
    }

    /** Reads an own property slot through the current access policy. / 현재 접근 정책을 거쳐 자체 속성 슬롯을 읽는다. */
    @Override
    public Property getOwnPropertySlot(Object key) {
        return target().getOwnPropertySlot(key);
    }

    /** Reads a descriptor through the current access policy. / 현재 접근 정책을 거쳐 속성 기술자를 읽는다. */
    @Override
    public PropertyDescriptor getOwnProperty(Object key) {
        return target().getOwnProperty(key);
    }

    /** Defines a property through the current access policy. / 현재 접근 정책을 거쳐 속성을 정의한다. */
    @Override
    public boolean defineOwnProperty(Object key, PropertyDescriptor desc) {
        return target().defineOwnProperty(key, desc);
    }

    /** Deletes a property through the current access policy. / 현재 접근 정책을 거쳐 속성을 삭제한다. */
    @Override
    public boolean delete(Object key) {
        return target().delete(key);
    }

    /** Lists the own keys visible on the current target. / 현재 대상에서 볼 수 있는 자체 키를 나열한다. */
    @Override
    public List<Object> ownKeys() {
        return target().ownKeys();
    }

    /** Returns the prototype visible from the caller's origin. / 호출자의 오리진에서 볼 수 있는 프로토타입을 반환한다. */
    @Override
    public JsObject getPrototypeOf() {
        return target().getPrototypeOf();
    }

    /** WindowProxy has an immutable prototype. / WindowProxy의 프로토타입은 바꿀 수 없다. */
    @Override
    public boolean setPrototypeOf(JsObject prototype) {
        return getPrototypeOf() == prototype;
    }

    /** WindowProxy always reports extensibility. / WindowProxy는 항상 확장 가능으로 보고한다. */
    @Override
    public boolean isExtensible() {
        return true;
    }

    /** Rejects attempts to prevent extensions. / 확장 방지 요청을 거부한다. */
    @Override
    public boolean preventExtensions() {
        return false;
    }
}
