package org.duckdns.hjow.swingwebpanel.js.builtins.meta;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;

/**
 * Installs WeakRef and FinalizationRegistry (ECMA-262 2022 section 26.1). The host objects use Java
 * {@link WeakReference} and {@link ReferenceQueue}; finalizer callbacks are ordinary realm jobs and never run on a
 * garbage-collector thread.
 * <p>
 * WeakRef 와 FinalizationRegistry(ECMA-262 2022 26.1절)를 설치한다. 호스트 객체는 Java {@link WeakReference} 와
 * {@link ReferenceQueue} 를 쓰며 종료 콜백은 GC 스레드에서 실행하지 않고 일반 realm 작업으로 만든다.
 */
public final class WeakReferenceBuiltins {

    /**
     * Registries by realm. Values do not strongly retain the registry objects.
     * <p>
     * realm 별 registry. 값은 registry 객체를 강하게 유지하지 않는다.
     */
    private static final Map<JsRealm, List<WeakReference<FinalizationRegistryObject>>> REGISTRIES =
            Collections.synchronizedMap(new WeakHashMap<JsRealm, List<WeakReference<FinalizationRegistryObject>>>());

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private WeakReferenceBuiltins() {
    }

    /**
     * Installs both constructors and their prototypes.
     * <p>
     * 두 생성자와 프로토타입을 설치한다.
     *
     * @param realm the realm / realm
     */
    static void install(final JsRealm realm) {
        final JsObject weakPrototype = realm.newObject();
        NativeFunction weakConstructor = new NativeFunction(realm, "WeakRef", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                throw JsException.typeError("WeakRef constructor cannot be invoked without 'new'");
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                JsValue target = Js.arg(args, 0);
                if (!(target instanceof JsObject)) {
                    throw JsException.typeError("Invalid WeakRef target");
                }
                return new WeakRefObject(Ops.getPrototypeFromConstructor(r, newTarget, weakPrototype), (JsObject) target, r);
            }
        });
        weakConstructor.defineData("prototype", weakPrototype, false, false, false);
        weakPrototype.defineBuiltin("constructor", weakConstructor);
        weakPrototype.defineData(WellKnownSymbols.TO_STRING_TAG, JsString.of("WeakRef"), false, false, true);
        realm.method(weakPrototype, "deref", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                if (!(thisValue instanceof WeakRefObject)) {
                    throw JsException.typeError("WeakRef.prototype.deref called on incompatible receiver");
                }
                return ((WeakRefObject) thisValue).deref(r);
            }
        });
        realm.getGlobalObject().defineBuiltin("WeakRef", weakConstructor);

        final JsObject registryPrototype = realm.newObject();
        NativeFunction registryConstructor = new NativeFunction(realm, "FinalizationRegistry", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                throw JsException.typeError("FinalizationRegistry constructor cannot be invoked without 'new'");
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                JsValue cleanup = Js.arg(args, 0);
                if (!cleanup.isCallable()) {
                    throw JsException.typeError("FinalizationRegistry cleanup callback must be a function");
                }
                FinalizationRegistryObject registry = new FinalizationRegistryObject(
                        Ops.getPrototypeFromConstructor(r, newTarget, registryPrototype), cleanup, r);
                addRegistry(r, registry);
                return registry;
            }
        });
        registryConstructor.defineData("prototype", registryPrototype, false, false, false);
        registryPrototype.defineBuiltin("constructor", registryConstructor);
        registryPrototype.defineData(WellKnownSymbols.TO_STRING_TAG, JsString.of("FinalizationRegistry"), false, false, true);
        realm.method(registryPrototype, "register", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                requireRegistry(thisValue, "register").register(Js.arg(args, 0), Js.arg(args, 1), Js.arg(args, 2));
                return Js.UNDEFINED;
            }
        });
        realm.method(registryPrototype, "unregister", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.bool(requireRegistry(thisValue, "unregister").unregister(Js.arg(args, 0)));
            }
        });
        realm.method(registryPrototype, "cleanupSome", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                requireRegistry(thisValue, "cleanupSome").cleanupSome(Js.arg(args, 0));
                return Js.UNDEFINED;
            }
        });
        realm.getGlobalObject().defineBuiltin("FinalizationRegistry", registryConstructor);
    }

    /**
     * Adds a registry to the per-realm polling list.
     * <p>
     * realm 의 확인 목록에 registry 를 추가한다.
     *
     * @param realm the realm / realm
     * @param registry the registry / registry
     */
    private static void addRegistry(JsRealm realm, FinalizationRegistryObject registry) {
        List<WeakReference<FinalizationRegistryObject>> list = REGISTRIES.get(realm);
        if (list == null) {
            list = new ArrayList<WeakReference<FinalizationRegistryObject>>();
            REGISTRIES.put(realm, list);
        }
        list.add(new WeakReference<FinalizationRegistryObject>(registry));
    }

    /**
     * Polls reference queues and enqueues available cleanup callbacks for a realm.
     * <p>
     * 참조 큐를 확인하고 사용할 수 있는 정리 콜백을 realm 작업으로 넣는다.
     *
     * @param realm the realm / realm
     */
    public static void poll(JsRealm realm) {
        List<WeakReference<FinalizationRegistryObject>> list = REGISTRIES.get(realm);
        if (list == null) {
            return;
        }
        // 약한 목록에서 죽은 registry 를 빼고 살아 있는 큐만 확인한다.
        for (int i = list.size() - 1; i >= 0; i--) {
            FinalizationRegistryObject registry = list.get(i).get();
            if (registry == null) {
                list.remove(i);
            } else {
                registry.enqueueCollected();
            }
        }
    }

    /**
     * Returns the required FinalizationRegistry receiver.
     * <p>
     * 필요한 FinalizationRegistry 수신 객체를 반환한다.
     *
     * @param value receiver / 수신 값
     * @param method method name / 메서드 이름
     * @return registry / registry
     */
    private static FinalizationRegistryObject requireRegistry(JsValue value, String method) {
        if (!(value instanceof FinalizationRegistryObject)) {
            throw JsException.typeError("FinalizationRegistry.prototype." + method + " called on incompatible receiver");
        }
        return (FinalizationRegistryObject) value;
    }

    /**
     * WeakRef instance.
     * <p>
     * WeakRef 인스턴스.
     */
    private static final class WeakRefObject extends JsObject {
        /**
         * Weak target.
         * <p>
         * 약한 대상.
         */
        private final WeakReference<JsObject> target;

        /**
         * Creates a WeakRef instance.
         * <p>
         * WeakRef 인스턴스를 만든다.
         *
         * @param prototype prototype / 프로토타입
         * @param value target / 대상
         * @param realm realm / realm
         */
        WeakRefObject(JsObject prototype, JsObject value, JsRealm realm) {
            super(prototype);
            target = new WeakReference<JsObject>(value);
        }

        /**
         * Dereferences the target and keeps it alive through this realm task.
         * <p>
         * 대상을 돌려주고 이번 realm 작업 동안 살아 있게 유지한다.
         *
         * @param realm realm / realm
         * @return target or undefined / 대상 또는 undefined
         */
        JsValue deref(JsRealm realm) {
            JsObject value = target.get();
            if (value == null) {
                return Js.UNDEFINED;
            }
            realm.keepWeaklyHeld(value);
            return value;
        }
    }

    /**
     * A FinalizationRegistry registration whose target reference is enqueued after collection.
     * <p>
     * 대상 수거 뒤 큐에 들어가는 FinalizationRegistry 등록.
     */
    private static final class RegistrationReference extends WeakReference<JsObject> {
        /**
         * Registration state.
         * <p>
         * 등록 상태.
         */
        final Cell cell;

        /**
         * Creates a target reference.
         * <p>
         * 대상 참조를 만든다.
         *
         * @param target target / 대상
         * @param queue queue / 큐
         * @param value cell / 등록 상태
         */
        RegistrationReference(JsObject target, ReferenceQueue<JsObject> queue, Cell value) {
            super(target, queue);
            cell = value;
        }
    }

    /**
     * One FinalizationRegistry cell.
     * <p>
     * FinalizationRegistry 등록 하나.
     */
    private static final class Cell {
        /**
         * Weak target reference.
         * <p>
         * 약한 대상 참조.
         */
        RegistrationReference target;
        /**
         * Value passed to cleanup.
         * <p>
         * 정리 콜백에 줄 값.
         */
        JsValue holdings;
        /**
         * Weak unregister token.
         * <p>
         * 약한 등록 해제 토큰.
         */
        WeakReference<JsObject> unregisterToken;
        /**
         * Whether a cleanup has been queued.
         * <p>
         * 정리 작업을 넣었는지.
         */
        boolean queued;
        /**
         * Whether this registration has been removed.
         * <p>
         * 등록을 제거했는지.
         */
        boolean removed;
    }

    /**
     * FinalizationRegistry instance.
     * <p>
     * FinalizationRegistry 인스턴스.
     */
    private static final class FinalizationRegistryObject extends JsObject {
        /**
         * Cleanup callback.
         * <p>
         * 정리 콜백.
         */
        private final JsValue cleanupCallback;
        /**
         * Owning realm.
         * <p>
         * 소유 realm.
         */
        private final JsRealm realm;
        /**
         * Collected target queue.
         * <p>
         * 수거된 대상 큐.
         */
        private final ReferenceQueue<JsObject> queue = new ReferenceQueue<JsObject>();
        /**
         * Registered cells.
         * <p>
         * 등록 상태 목록.
         */
        private final List<Cell> cells = new ArrayList<Cell>();

        /**
         * Creates a registry.
         * <p>
         * registry 를 만든다.
         *
         * @param prototype prototype / 프로토타입
         * @param callback cleanup callback / 정리 콜백
         * @param owner realm / realm
         */
        FinalizationRegistryObject(JsObject prototype, JsValue callback, JsRealm owner) {
            super(prototype);
            cleanupCallback = callback;
            realm = owner;
        }

        /**
         * Registers a target and optional token.
         * <p>
         * 대상과 선택적 토큰을 등록한다.
         *
         * @param target target / 대상
         * @param holdings holdings / 정리 값
         * @param token unregister token / 등록 해제 토큰
         */
        void register(JsValue target, JsValue holdings, JsValue token) {
            if (!(target instanceof JsObject)) {
                throw JsException.typeError("FinalizationRegistry target must be an object");
            }
            if (Ops.sameValue(target, holdings)) {
                throw JsException.typeError("FinalizationRegistry target and holdings must not be the same");
            }
            WeakReference<JsObject> weakToken = null;
            if (!token.isUndefined()) {
                if (!(token instanceof JsObject)) {
                    throw JsException.typeError("FinalizationRegistry unregisterToken must be an object");
                }
                weakToken = new WeakReference<JsObject>((JsObject) token);
            }
            Cell cell = new Cell();
            cell.holdings = holdings;
            cell.unregisterToken = weakToken;
            cell.target = new RegistrationReference((JsObject) target, queue, cell);
            cells.add(cell);
        }

        /**
         * Unregisters every matching token.
         * <p>
         * 일치하는 토큰을 모두 등록 해제한다.
         *
         * @param token token / 토큰
         * @return true if anything was removed / 하나라도 제거했으면 true
         */
        boolean unregister(JsValue token) {
            if (!(token instanceof JsObject)) {
                throw JsException.typeError("FinalizationRegistry unregisterToken must be an object");
            }
            boolean removed = false;
            // 같은 약한 토큰으로 등록된 셀을 모두 찾아 제거한다.
            for (int i = cells.size() - 1; i >= 0; i--) {
                Cell cell = cells.get(i);
                if (!cell.removed && cell.unregisterToken != null && cell.unregisterToken.get() == token) {
                    cell.removed = true;
                    cells.remove(i);
                    removed = true;
                }
            }
            return removed;
        }

        /**
         * Queues callbacks for targets observed in the Java reference queue.
         * <p>
         * Java 참조 큐에서 관찰한 대상의 콜백을 큐에 넣는다.
         */
        void enqueueCollected() {
            RegistrationReference ref;
            // GC 큐에서 관찰한 대상마다 정리 콜백 작업을 넣는다.
            while ((ref = (RegistrationReference) queue.poll()) != null) {
                final Cell cell = ref.cell;
                if (!cell.removed && !cell.queued) {
                    cell.queued = true;
                    realm.enqueueJob(new Runnable() {
                        public void run() {
                            try {
                                if (!cell.removed) {
                                    Ops.call(cleanupCallback, FinalizationRegistryObject.this, new JsValue[] { cell.holdings });
                                }
                            } finally {
                                cells.remove(cell);
                            }
                        }
                    });
                }
            }
        }

        /**
         * Runs cleanup synchronously for currently collected targets.
         * <p>
         * 현재 수거된 대상의 정리를 동기적으로 실행한다.
         *
         * @param alternate optional cleanup callback / 선택적 정리 콜백
         */
        void cleanupSome(JsValue alternate) {
            JsValue callback = alternate.isUndefined() ? cleanupCallback : alternate;
            if (!callback.isCallable()) {
                throw JsException.typeError("FinalizationRegistry cleanup callback must be a function");
            }
            RegistrationReference ref;
            // 현재 확인 가능한 모든 수거 대상을 바로 정리한다.
            while ((ref = (RegistrationReference) queue.poll()) != null) {
                Cell cell = ref.cell;
                if (!cell.removed && !cell.queued) {
                    cell.queued = true;
                    try {
                        Ops.call(callback, this, new JsValue[] { cell.holdings });
                    } finally {
                        cells.remove(cell);
                    }
                }
            }
        }
    }
}
