package org.duckdns.hjow.swingwebpanel.js.host;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.Property;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.net.Origin;

/**
 * The restricted Java host window shown to one document.
 * <p>
 * 문서 하나에 보이는 제한된 Java 호스트 창.
 */
final class HostMessageWindow extends JsObject {

    /** Allowed own keys. / 허용하는 자체 키. */
    private static final String[] MEMBERS = {"postMessage", "closed", "close", "focus", "blur"};

    /** The associated document, which a retained host window cannot keep alive. / 호스트 창 보관으로 수명이 늘지 않는 연결 문서. */
    private final WeakReference<PageScripts> owner;

    /** The fixed origin for this host. / 이 호스트의 고정 오리진. */
    private final String origin;

    /** Weak method cache scoped to the script that reads a method. / 메서드를 읽은 스크립트 기준의 약한 메서드 캐시. */
    private final Map<JsRealm, Map<String, WeakReference<JsObject>>> methods =
            new WeakHashMap<JsRealm, Map<String, WeakReference<JsObject>>>();

    /**
     * Records the weak document link and the fixed origin.
     * <p>
     * 약한 문서 연결과 고정 오리진을 기록한다.
     *
     * @param page associated document / 연결 문서
     * @param origin fixed host origin / 고정 호스트 오리진
     */
    HostMessageWindow(PageScripts page, String origin) {
        super((JsObject) null);
        owner = new WeakReference<PageScripts>(page);
        this.origin = origin;
    }

    /**
     * Reads an allowed method or reports the associated document's lifetime.
     * <p>
     * 허용한 메서드를 읽거나 연결 문서의 수명을 확인한다.
     *
     * @param key property key / 속성 키
     * @return own property descriptor slot / 자체 프로퍼티 기술자 슬롯
     */
    public Property getOwnPropertySlot(Object key) {
        PageScripts page = owner.get();
        if ("closed".equals(key)) {
            boolean closed = page == null || page.isDisposed() || !page.getEnvironment().isScriptingEnabled();
            return Property.data(Js.bool(closed), false, false, true);
        }
        if ("postMessage".equals(key) || "close".equals(key) || "focus".equals(key) || "blur".equals(key)) {
            PageScripts caller = PageScripts.messageSender(page);
            if (caller != null) {
                return Property.data(method(caller.getRealm(), (String) key), false, false, true);
            }
        }
        throw denied(key);
    }

    /**
     * Obtains a method in the accessing realm without holding that realm or its functions strongly.
     * <p>
     * 접근 렐름의 메서드를 얻되 렐름이나 함수를 강하게 보관하지 않는다.
     *
     * @param realm the accessing realm / 접근 렐름
     * @param name method name / 메서드 이름
     * @return the method / 메서드
     */
    private synchronized JsObject method(JsRealm realm, final String name) {
        Map<String, WeakReference<JsObject>> cached = methods.get(realm);
        if (cached == null) {
            cached = new HashMap<String, WeakReference<JsObject>>();
            methods.put(realm, cached);
        }
        WeakReference<JsObject> reference = cached.get(name);
        JsObject value = reference == null ? null : reference.get();
        if (value == null) {
            value = realm.newFunction(name, "postMessage".equals(name) ? 2 : 0, new NativeFunction.Call() {
                public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                    // 닫기와 포커스 메서드는 Swing 호스트 상태를 바꾸지 않는다.
                    if ("postMessage".equals(name)) {
                        send(args);
                    }
                    return Js.UNDEFINED;
                }
            });
            cached.put(name, new WeakReference<JsObject>(value));
        }
        return value;
    }

    /**
     * Validates and copies a script message before the target check and Java delivery.
     * <p>
     * 스크립트 메시지를 검증·복제한 뒤 대상 검사 및 Java 전달을 한다.
     *
     * @param args postMessage arguments / postMessage 인수
     */
    private void send(JsValue[] args) {
        if (args.length == 0) {
            throw JsException.typeError("A web message value is required");
        }
        PageScripts page = owner.get();
        PageScripts sender = PageScripts.messageSender(page);
        if (sender == null) {
            return;
        }
        JsValue second = Js.arg(args, 1);
        boolean options = second.isObject() || second.isNullOrUndefined();
        JsValue name = second.isObject() ? ((JsObject) second).get("targetOrigin") : options ? Js.UNDEFINED : second;
        String target = name.isUndefined() ? "/" : Ops.toString(name);
        boolean matches = "*".equals(target) || origin.equals(target);
        if (!matches && !"/".equals(target)) {
            try {
                Origin.ofAbsoluteMessageUrl(target);
            } catch (IllegalArgumentException e) {
                throw sender.getBridge().domError(DomException.syntax("Invalid web message target origin: " + target));
            }
        }
        Object data = sender.messaging().copyForJava(args[0], options ? second : Js.arg(args, 2));
        // 복사와 전송 목록 처리를 마친 뒤 유효한 문서와 정확한 호스트 주소에서만 전달한다.
        if (matches && page != null && !page.isDisposed() && !sender.isDisposed()
                && page.getEnvironment().getSharedMemoryCluster() == sender.getEnvironment().getSharedMemoryCluster()
                && sender.getEnvironment().isScriptingEnabled()) {
            sender.getEnvironment().publishHostMessage(data);
        }
    }

    /**
     * Returns the restricted own keys.
     * <p>
     * 제한된 자체 키를 반환한다.
     *
     * @return own keys / 자체 키
     */
    public List<Object> ownKeys() {
        return new ArrayList<Object>(Arrays.asList(MEMBERS));
    }

    /** Rejects prototype replacement. / 프로토타입 교체를 거부한다. */
    public boolean setPrototypeOf(JsObject prototype) {
        return prototype == null;
    }

    /** Retains the host-defined own property contract. / 호스트가 정의한 자체 프로퍼티 계약을 유지한다. */
    public boolean preventExtensions() {
        return false;
    }

    /** Rejects private elements on a restricted host object. / 제한 호스트 객체에 private 요소 추가를 거부한다. */
    public void ensureCanAddPrivateElement() {
        throw JsException.typeError("Cannot add a private element to the Java host window");
    }

    /** Rejects a property assignment. / 프로퍼티 대입을 거부한다. */
    public boolean set(Object key, JsValue value, JsValue receiver) {
        throw denied(key);
    }

    /** Rejects a new own property definition. / 자체 프로퍼티 정의를 거부한다. */
    public boolean defineOwnProperty(Object key, PropertyDescriptor descriptor) {
        throw denied(key);
    }

    /** Rejects a property deletion. / 프로퍼티 삭제를 거부한다. */
    public boolean delete(Object key) {
        throw denied(key);
    }

    /**
     * Builds the error for a forbidden property operation.
     * <p>
     * 금지된 프로퍼티 동작의 오류를 만든다.
     *
     * @param key property key / 프로퍼티 키
     * @return security exception of the accessing page / 접근 페이지의 보안 예외
     */
    private JsException denied(Object key) {
        PageScripts caller = PageScripts.messageSender(owner.get());
        String message = "Property '" + key + "' is inaccessible on the Java host window";
        return caller == null ? JsException.typeError(message) : caller.getBridge().domError(DomException.security(message));
    }
}
