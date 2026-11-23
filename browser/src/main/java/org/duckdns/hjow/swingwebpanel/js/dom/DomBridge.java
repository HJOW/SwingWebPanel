package org.duckdns.hjow.swingwebpanel.js.dom;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;

/**
 * The bridge between the Java host objects of one page (DOM nodes, events, collections ...) and the JavaScript realm of that page. It owns
 * the interface table (one prototype and one constructor object per WebIDL interface), the identity-preserving wrapper cache, and the
 * conversion of arguments and results. Interfaces are described by short specification strings (see
 * {@link #defineInterface(String, String, Class, String[])}); every described Java method is looked up once, when the interface is defined,
 * so a wrong description fails at start-up (and in the tests) and never at run time. Only methods that are listed in a description can be
 * reached from script: there is no reflection, class or package object visible to JavaScript.
 * <p>
 * Wrappers are cached weakly (the same Java object gives the same JavaScript object as long as the page holds it) and are pinned as soon as
 * a script stores a property on them. All methods must be called on the thread that owns the realm.
 * <p>
 * 한 페이지의 Java 호스트 객체(DOM 노드, 이벤트, 컬렉션 등)와 그 페이지의 JavaScript realm 을 잇는 다리. 인터페이스 표(WebIDL 인터페이스마다
 * 프로토타입 하나와 생성자 객체 하나), 동일성을 유지하는 래퍼 캐시, 인수와 결과의 변환을 가진다. 인터페이스는 짧은 명세 문자열로 기술하며
 * ({@link #defineInterface(String, String, Class, String[])} 참고) 기술한 Java 메서드는 인터페이스를 정의할 때 한 번 찾으므로 잘못된 기술은 실행 중이
 * 아니라 시작할 때(시험에서) 실패한다. 기술에 나열된 메서드만 스크립트에서 닿을 수 있으며 JavaScript 에 보이는 리플렉션, 클래스, 패키지 객체는 없다.
 * <p>
 * 래퍼는 약하게 캐시하며(같은 Java 객체는 페이지가 붙잡고 있는 동안 같은 JavaScript 객체를 준다) 스크립트가 프로퍼티를 저장하면 고정한다.
 * 모든 메서드는 realm 을 소유한 스레드에서 호출해야 한다.
 */
public final class DomBridge {

    /**
     * Chooses the interface name of a Java host object.
     * <p>
     * Java 호스트 객체의 인터페이스 이름을 고른다.
     */
    public interface Classifier {
        /**
         * Returns the interface name for an object, or null if this classifier does not know it.
         * <p>
         * 객체의 인터페이스 이름을 반환한다. 이 분류기가 모르면 null.
         *
         * @param target the Java object / Java 객체
         * @return the interface name or null / 인터페이스 이름 또는 null
         */
        String classify(Object target);
    }

    /**
     * Creates special wrapper objects for an interface (for example the named-property wrapper of Storage).
     * <p>
     * 인터페이스의 특수 래퍼 객체를 만든다(예: Storage 의 이름 있는 프로퍼티 래퍼).
     */
    public interface Factory {
        /**
         * Creates the wrapper.
         * <p>
         * 래퍼를 만든다.
         *
         * @param bridge the bridge / 브리지
         * @param proto  the prototype / 프로토타입
         * @param target the Java object / Java 객체
         * @return the wrapper / 래퍼
         */
        HostObject create(DomBridge bridge, JsObject proto, Object target);
    }

    /**
     * The runtime description of one interface.
     * <p>
     * 인터페이스 하나의 실행 시간 기술.
     */
    public static final class InterfaceInfo {
        /**
         * The interface name (the name of the global constructor).
         * <p>
         * 인터페이스 이름(전역 생성자의 이름).
         */
        final String name;

        /**
         * The parent interface or null.
         * <p>
         * 부모 인터페이스 또는 null.
         */
        final InterfaceInfo parent;

        /**
         * The Java class that targets of this interface must be instances of.
         * <p>
         * 이 인터페이스의 대상이 인스턴스여야 하는 Java 클래스.
         */
        final Class<?> javaClass;

        /**
         * The prototype object.
         * <p>
         * 프로토타입 객체.
         */
        final JsObject prototype;

        /**
         * The interface object (the constructor function).
         * <p>
         * 인터페이스 객체(생성자 함수).
         */
        final NativeFunction constructor;

        /**
         * The integer constants of the interface (they are also defined on a constructor that replaces the placeholder).
         * <p>
         * 인터페이스의 정수 상수(자리표시를 대체하는 생성자에도 정의된다).
         */
        final java.util.Map<String, JsValue> constants = new java.util.LinkedHashMap<String, JsValue>();

        /**
         * Reader for list-like interfaces, or null.
         * <p>
         * 목록 형태 인터페이스의 읽기 방법. 아니면 null.
         */
        IndexedObject.Source indexed;

        /**
         * Creates the wrapper objects of this interface, or null for plain {@link HostObject}s.
         * <p>
         * 이 인터페이스의 래퍼 객체를 만드는 쪽. 일반 {@link HostObject} 이면 null.
         */
        Factory factory;

        /**
         * Creates the description.
         * <p>
         * 기술을 만든다.
         *
         * @param name        interface name / 인터페이스 이름
         * @param parent      parent or null / 부모 또는 null
         * @param javaClass   target class / 대상 클래스
         * @param prototype   prototype / 프로토타입
         * @param constructor interface object / 인터페이스 객체
         */
        InterfaceInfo(String name, InterfaceInfo parent, Class<?> javaClass, JsObject prototype, NativeFunction constructor) {
            this.name = name;
            this.parent = parent;
            this.javaClass = javaClass;
            this.prototype = prototype;
            this.constructor = constructor;
        }

        /**
         * Returns the prototype object.
         * <p>
         * 프로토타입 객체를 반환한다.
         *
         * @return the prototype / 프로토타입
         */
        public JsObject getPrototype() {
            return prototype;
        }

        /**
         * Returns the interface object.
         * <p>
         * 인터페이스 객체를 반환한다.
         *
         * @return the constructor function / 생성자 함수
         */
        public NativeFunction getConstructor() {
            return constructor;
        }
    }

    /**
     * The realm of the page.
     * <p>
     * 페이지의 realm.
     */
    private final JsRealm realm;

    /**
     * Interfaces by name.
     * <p>
     * 이름별 인터페이스.
     */
    private final Map<String, InterfaceInfo> interfaces = new HashMap<String, InterfaceInfo>();

    /**
     * Classifiers in the order they are asked.
     * <p>
     * 물어 보는 순서대로의 분류기.
     */
    private final List<Classifier> classifiers = new ArrayList<Classifier>();

    /**
     * Weak wrapper cache: Java object to wrapper.
     * <p>
     * 약한 래퍼 캐시: Java 객체에서 래퍼로.
     */
    private final Map<Object, WeakReference<HostObject>> cache = new WeakHashMap<Object, WeakReference<HostObject>>();

    /**
     * Wrappers that must stay alive (they hold script-defined data).
     * <p>
     * 계속 살려 둘 래퍼(스크립트가 정의한 데이터를 가진다).
     */
    private final Set<HostObject> pinned = new HashSet<HostObject>();

    /**
     * Creates the bridge of a realm.
     * <p>
     * realm 의 브리지를 만든다.
     *
     * @param realm the page realm, not null / 페이지 realm(null 불가)
     */
    public DomBridge(JsRealm realm) {
        if (realm == null) {
            throw new IllegalArgumentException("realm must not be null");
        }
        this.realm = realm;
    }

    /**
     * The event handler registry of the page.
     * <p>
     * 페이지의 이벤트 핸들러 등록부.
     */
    private final EventHandlers eventHandlers = new EventHandlers(this);

    /**
     * Returns the event handler registry of the page.
     * <p>
     * 페이지의 이벤트 핸들러 등록부를 반환한다.
     *
     * @return the registry / 등록부
     */
    public EventHandlers getEventHandlers() {
        return eventHandlers;
    }

    /**
     * Returns the realm.
     * <p>
     * realm 을 반환한다.
     *
     * @return the realm / realm
     */
    public JsRealm getRealm() {
        return realm;
    }

    /**
     * Registers a classifier (asked after the ones registered earlier).
     * <p>
     * 분류기를 등록한다(먼저 등록된 것들 다음에 묻는다).
     *
     * @param c the classifier / 분류기
     */
    public void addClassifier(Classifier c) {
        classifiers.add(c);
    }

    /**
     * Java objects that stand for an existing script object (the window stands for the global object).
     * <p>
     * 이미 있는 스크립트 객체를 대신하는 Java 객체(window 는 전역 객체를 대신한다).
     */
    private final Map<Object, JsObject> aliases = new java.util.IdentityHashMap<Object, JsObject>();

    /**
     * Declares that a Java object is represented by an existing script object.
     * <p>
     * Java 객체를 이미 있는 스크립트 객체로 나타내도록 선언한다.
     *
     * @param target the Java object / Java 객체
     * @param object the script object / 스크립트 객체
     */
    public void alias(Object target, JsObject object) {
        aliases.put(target, object);
    }

    /**
     * Installs the wrapper factory of an interface.
     * <p>
     * 인터페이스의 래퍼 팩터리를 설치한다.
     *
     * @param name    the interface name / 인터페이스 이름
     * @param factory the factory / 팩터리
     */
    public void setFactory(String name, Factory factory) {
        interfaces.get(name).factory = factory;
    }

    /**
     * Keeps a wrapper alive for the lifetime of the bridge.
     * <p>
     * 래퍼를 브리지가 살아 있는 동안 계속 붙잡는다.
     *
     * @param w the wrapper / 래퍼
     */
    void pin(HostObject w) {
        pinned.add(w);
    }

    /**
     * Drops all wrappers and caches (the page is being replaced or disposed).
     * <p>
     * 모든 래퍼와 캐시를 버린다(페이지가 교체되거나 폐기될 때).
     */
    public void dispose() {
        stopEventExecution();
        cache.clear();
        pinned.clear();
    }

    /** Permanent callback gate, independent of the page execution lock. / 페이지 실행 잠금과 독립적인 영구 콜백 차단 표시. */
    private volatile boolean eventExecutionEnabled = true;

    /** Stops callbacks before locked cleanup can begin; safe from any thread. / 잠금이 필요한 해제 전 콜백을 중단하며 어느 스레드에서나 안전하다. */
    public void stopEventExecution() {
        eventExecutionEnabled = false;
    }

    /** Returns whether DOM listeners may enter JavaScript. / DOM 리스너가 JavaScript를 실행할 수 있는지 반환한다.
     * @return callback permission / 콜백 허용 여부 */
    public boolean isEventExecutionEnabled() {
        return eventExecutionEnabled;
    }

    /**
     * Makes an event the current event while a listener runs: sets the <code>event</code> property of the global object
     * (<code>window.event</code>, HTML Standard "current event") and returns the previous value so that nested dispatches restore it.
     * Pair every call with {@link #leaveEvent(JsValue)} in a <code>finally</code> block.
     * <p>
     * 리스너가 실행되는 동안 이벤트를 현재 이벤트로 만든다: 전역 객체의 <code>event</code> 프로퍼티(<code>window.event</code>, HTML Standard
     * "current event")를 지정하고 중첩된 전달이 복원할 수 있도록 이전 값을 반환한다. 모든 호출은 <code>finally</code> 블록의
     * {@link #leaveEvent(JsValue)} 와 짝을 이룬다.
     *
     * @param event the JavaScript event object / 자바스크립트 이벤트 객체
     * @return the previous value of <code>window.event</code> / <code>window.event</code> 의 이전 값
     */
    public JsValue enterEvent(JsValue event) {
        JsObject g = realm.getGlobalObject();
        JsValue previous = g.get("event");
        g.defineData("event", event, true, true, true);
        return previous;
    }

    /**
     * Restores <code>window.event</code> after a listener ran.
     * <p>
     * 리스너가 실행된 뒤 <code>window.event</code> 를 복원한다.
     *
     * @param previous the value returned by {@link #enterEvent(JsValue)} / {@link #enterEvent(JsValue)} 가 반환한 값
     */
    public void leaveEvent(JsValue previous) {
        realm.getGlobalObject().defineData("event", previous, true, true, true);
    }

    /**
     * Returns an interface by name.
     * <p>
     * 이름으로 인터페이스를 반환한다.
     *
     * @param name the name / 이름
     * @return the interface or null / 인터페이스 또는 null
     */
    public InterfaceInfo getInterface(String name) {
        return interfaces.get(name);
    }

    /**
     * Defines an interface: creates its prototype and interface object, installs the members and puts the interface object on the global
     * object. Member specifications (one string each):
     * <ul>
     * <li><code>a:name</code> read-only attribute, <code>a:name=</code> read-write attribute; the Java accessors are
     * <code>getName</code>/<code>isName</code> and <code>setName</code>; <code>a:jsName@JavaBase</code> uses another Java base name;</li>
     * <li><code>m:name(Type,Type*)</code> operation with the Java method of the same name; a type followed by <code>*</code> is optional;
     * <code>?</code> after a type makes null and undefined mean Java null; <code>m:jsName(Type)@javaName</code> names the Java method; a
     * last type <code>Object...</code> collects all remaining arguments;</li>
     * <li><code>c:NAME=value</code> integer constant;</li>
     * <li><code>r:prop:attribute:type</code> attribute reflection of a content attribute (types <code>string</code>, <code>url</code>,
     * <code>bool</code>, <code>int</code>, <code>uint</code>); an empty attribute means the lower-cased property name.</li>
     * </ul>
     * Types: String, int, long, short, double, boolean, Boolean, Node, Element, Attr, Document, Event, EventTarget, Object.
     * <p>
     * 인터페이스를 정의한다: 프로토타입과 인터페이스 객체를 만들고 멤버를 설치하며 인터페이스 객체를 전역 객체에 둔다. 멤버 명세(문자열 하나씩):
     * <code>a:name</code>(읽기 전용 속성), <code>a:name=</code>(읽기/쓰기 속성, Java 접근자는 <code>getName</code>/<code>isName</code>,
     * <code>setName</code>, <code>a:jsName@JavaBase</code> 는 다른 Java 기본 이름), <code>m:name(Type,Type*)</code>(같은 이름의 Java 메서드를 쓰는
     * 연산, <code>*</code> 는 선택 인수, <code>?</code> 는 null/undefined 를 Java null 로, <code>m:jsName(Type)@javaName</code> 은 Java 메서드 이름 지정,
     * 마지막 <code>Object...</code> 는 나머지 인수를 모음), <code>c:NAME=value</code>(정수 상수), <code>r:prop:attribute:type</code>(콘텐츠 속성 반영).
     *
     * @param name      the interface name / 인터페이스 이름
     * @param parent    the parent interface name or null / 부모 인터페이스 이름 또는 null
     * @param javaClass the class of the targets / 대상의 클래스
     * @param specs     the member specifications / 멤버 명세
     * @return the interface / 인터페이스
     */
    public InterfaceInfo defineInterface(String name, String parent, Class<?> javaClass, String[] specs) {
        InterfaceInfo p = parent == null ? null : interfaces.get(parent);
        if (parent != null && p == null) {
            throw new IllegalArgumentException("unknown parent interface " + parent);
        }
        JsObject proto = new JsObject(p == null ? realm.getObjectPrototype() : p.prototype);
        final String ifaceName = name;
        NativeFunction ctor = new NativeFunction(realm, p == null ? realm.getFunctionPrototype() : p.constructor, name, 0,
                new NativeFunction.Call() {
                    /**
                     * Interface objects cannot be called.
                     * <p>
                     * 인터페이스 객체는 호출할 수 없다.
                     *
                     * @param r    the realm / realm
                     * @param self this value / this 값
                     * @param args arguments / 인수
                     * @return never / 반환하지 않음
                     */
                    public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                        throw JsException.typeError("Failed to construct '" + ifaceName + "': Please use the 'new' operator, this DOM object "
                                + "constructor cannot be called as a function.");
                    }
                }, null);
        ctor.defineData("prototype", proto, false, false, false);
        proto.defineBuiltin("constructor", ctor);
        proto.defineData(WellKnownSymbols.TO_STRING_TAG, Js.str(name), false, false, true);
        InterfaceInfo info = new InterfaceInfo(name, p, javaClass, proto, ctor);
        interfaces.put(name, info);
        realm.getGlobalObject().defineBuiltin(name, ctor);
        // 명세 문자열을 하나씩 해석해 멤버를 설치한다.
        for (int i = 0; i < specs.length; i++) {
            MemberInstaller.install(this, info, specs[i]);
        }
        return info;
    }

    /**
     * Replaces the constructor behaviour of an interface (for interfaces that scripts may construct, such as <code>Event</code>).
     * <p>
     * 인터페이스의 생성 동작을 바꾼다(<code>Event</code> 처럼 스크립트가 생성할 수 있는 인터페이스용).
     *
     * @param name the interface name / 인터페이스 이름
     * @param ctor the constructor function that replaces the placeholder / 자리표시를 대체할 생성자 함수
     */
    public void setConstructor(String name, NativeFunction ctor) {
        InterfaceInfo info = interfaces.get(name);
        if (info == null) {
            throw new IllegalArgumentException("unknown interface " + name);
        }
        ctor.defineData("prototype", info.prototype, false, false, false);
        info.prototype.defineBuiltin("constructor", ctor);
        // 자리표시 생성자에 정의했던 상수를 새 생성자에도 정의한다(예: Event.NONE, KeyboardEvent.DOM_KEY_LOCATION_LEFT).
        for (java.util.Map.Entry<String, JsValue> c : info.constants.entrySet()) {
            ctor.defineData(c.getKey(), c.getValue(), false, true, false);
        }
        realm.getGlobalObject().defineBuiltin(name, ctor);
    }

    /**
     * Makes an interface list-like (see {@link IndexedObject}).
     * <p>
     * 인터페이스를 목록 형태로 만든다({@link IndexedObject} 참고).
     *
     * @param name   the interface name / 인터페이스 이름
     * @param source how to read the lists / 목록을 읽는 방법
     */
    public void setIndexed(String name, IndexedObject.Source source) {
        InterfaceInfo info = interfaces.get(name);
        if (info == null) {
            throw new IllegalArgumentException("unknown interface " + name);
        }
        info.indexed = source;
    }

    /**
     * Returns the interface that describes a Java object.
     * <p>
     * Java 객체를 기술하는 인터페이스를 반환한다.
     *
     * @param target the Java object / Java 객체
     * @return the interface or null if no classifier knows it / 인터페이스. 어느 분류기도 모르면 null
     */
    private InterfaceInfo interfaceOf(Object target) {
        // 등록된 분류기를 순서대로 물어 처음 아는 것을 쓴다.
        for (int i = 0; i < classifiers.size(); i++) {
            String n = classifiers.get(i).classify(target);
            if (n != null) {
                InterfaceInfo info = interfaces.get(n);
                if (info != null) {
                    return info;
                }
            }
        }
        return null;
    }

    /**
     * Returns the wrapper of a Java host object, creating it when needed (the same Java object gives the same wrapper while it is alive).
     * <p>
     * Java 호스트 객체의 래퍼를 반환하며 필요하면 만든다(같은 Java 객체는 살아 있는 동안 같은 래퍼를 준다).
     *
     * @param target the Java object, not null / Java 객체(null 불가)
     * @return the wrapper / 래퍼
     */
    public HostObject wrap(Object target) {
        WeakReference<HostObject> ref = cache.get(target);
        HostObject w = ref == null ? null : ref.get();
        if (w != null) {
            return w;
        }
        InterfaceInfo info = interfaceOf(target);
        if (info == null) {
            throw new IllegalStateException("no interface for " + target.getClass().getName());
        }
        if (info.factory != null) {
            w = info.factory.create(this, info.prototype, target);
        } else if (info.indexed != null) {
            w = new IndexedObject(this, info.prototype, target, info.indexed);
        } else {
            w = new HostObject(this, info.prototype, target);
        }
        cache.put(target, new WeakReference<HostObject>(w));
        return w;
    }

    /**
     * Converts a Java result to a JavaScript value: null, String, Boolean and numbers become primitives, {@link JsValue}s stay, everything
     * else is wrapped.
     * <p>
     * Java 결과를 JavaScript 값으로 바꾼다. null, String, Boolean, 숫자는 원시 값이 되고 {@link JsValue} 는 그대로이며 나머지는 래핑한다.
     *
     * @param o the Java value / Java 값
     * @return the JavaScript value / JavaScript 값
     */
    public JsValue toJs(Object o) {
        if (o == null) {
            return Js.NULL;
        }
        JsObject aliased = aliases.get(o);
        if (aliased != null) {
            return aliased;
        }
        if (o instanceof JsValue) {
            return (JsValue) o;
        }
        if (o instanceof String) {
            return Js.str((String) o);
        }
        if (o instanceof Boolean) {
            return Js.bool(((Boolean) o).booleanValue());
        }
        if (o instanceof Number) {
            return Js.num(((Number) o).doubleValue());
        }
        if (o instanceof List) {
            List<?> l = (List<?>) o;
            JsValue[] items = new JsValue[l.size()];
            // 목록의 항목을 하나씩 JavaScript 값으로 바꿔 배열을 만든다.
            for (int i = 0; i < items.length; i++) {
                items[i] = toJs(l.get(i));
            }
            return realm.newArray(items);
        }
        return wrap(o);
    }

    /**
     * Returns the Java target of a JavaScript value if it is a wrapper whose target is an instance of a class, otherwise null.
     * <p>
     * JavaScript 값이 래퍼이고 대상이 클래스의 인스턴스이면 Java 대상을 반환하고, 아니면 null 을 반환한다.
     *
     * @param v   the value / 값
     * @param cls the required class / 필요한 클래스
     * @return the target or null / 대상 또는 null
     */
    public static Object targetOf(JsValue v, Class<?> cls) {
        if (v instanceof HostObject) {
            Object t = ((HostObject) v).getTarget();
            if (cls.isInstance(t)) {
                return t;
            }
        }
        return null;
    }

    /**
     * Calls a Java method on behalf of a script and turns a DOM exception into a JavaScript <code>DOMException</code>.
     * <p>
     * 스크립트를 대신해 Java 메서드를 호출하고 DOM 예외를 JavaScript <code>DOMException</code> 으로 바꾼다.
     *
     * @param m      the method / 메서드
     * @param target the receiver / 수신 객체
     * @param args   the arguments / 인수
     * @return the result / 결과
     */
    public Object invoke(Method m, Object target, Object[] args) {
        try {
            return m.invoke(target, args);
        } catch (InvocationTargetException e) {
            Throwable c = e.getCause();
            if (c instanceof DomException) {
                throw domError((DomException) c);
            }
            if (c instanceof org.duckdns.hjow.swingwebpanel.storage.StorageQuotaException) {
                throw new JsException(DomExceptionObjects.create(this, "QuotaExceededError", c.getMessage()));
            }
            if (c instanceof RuntimeException) {
                throw (RuntimeException) c;
            }
            if (c instanceof Error) {
                throw (Error) c;
            }
            throw new IllegalStateException(c);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Creates the JavaScript exception for a DOM exception: a <code>DOMException</code> object with <code>name</code>, <code>message</code>
     * and <code>code</code>.
     * <p>
     * DOM 예외에 해당하는 JavaScript 예외를 만든다: <code>name</code>, <code>message</code>, <code>code</code> 를 가진
     * <code>DOMException</code> 객체.
     *
     * @param e the DOM exception / DOM 예외
     * @return the exception to throw / 던질 예외
     */
    public JsException domError(DomException e) {
        return new JsException(DomExceptionObjects.create(this, e.getName(), e.getMessage()));
    }

    /**
     * Creates a native function (helper for host objects that are not described by specifications).
     * <p>
     * 네이티브 함수를 만든다(명세로 기술하지 않는 호스트 객체용 도우미).
     *
     * @param name   function name / 함수 이름
     * @param length number of declared parameters / 선언된 매개변수 수
     * @param call   behaviour / 동작
     * @return the function / 함수
     */
    public NativeFunction function(String name, int length, NativeFunction.Call call) {
        return realm.newFunction(name, length, call);
    }

    /**
     * Receives errors that scripts, listeners and callbacks throw (the host reports them to <code>window.onerror</code> and the console).
     * <p>
     * 스크립트, 리스너, 콜백이 던진 오류를 받는다(호스트가 <code>window.onerror</code> 와 콘솔로 보고한다).
     */
    public interface ErrorSink {
        /**
         * Reports an uncaught script error.
         * <p>
         * 잡히지 않은 스크립트 오류를 보고한다.
         *
         * @param error the error (a {@link JsException} for a thrown value) / 오류(던져진 값이면 {@link JsException})
         */
        void scriptError(RuntimeException error);
    }

    /**
     * The receiver of script errors, or null for none.
     * <p>
     * 스크립트 오류를 받는 쪽. 없으면 null.
     */
    private ErrorSink errorSink;

    /**
     * Installs the receiver of script errors.
     * <p>
     * 스크립트 오류를 받는 쪽을 설치한다.
     *
     * @param sink the sink or null / 받는 쪽 또는 null
     */
    public void setErrorSink(ErrorSink sink) {
        this.errorSink = sink;
    }

    /**
     * Reports an error thrown by script code that was called from the host; aborts ({@link org.duckdns.hjow.swingwebpanel.js.ScriptAbortedException}) are
     * rethrown because they must stop the running task.
     * <p>
     * 호스트가 부른 스크립트 코드가 던진 오류를 보고한다. 중단({@link org.duckdns.hjow.swingwebpanel.js.ScriptAbortedException})은 실행 중인 작업을 멈춰야 하므로 다시 던진다.
     *
     * @param error the error / 오류
     */
    public void reportError(RuntimeException error) {
        if (error instanceof org.duckdns.hjow.swingwebpanel.js.ScriptAbortedException) {
            throw error;
        }
        if (errorSink != null) {
            errorSink.scriptError(error);
        }
    }

    /**
     * Converts a script value to a string the way WebIDL <code>DOMString</code> does.
     * <p>
     * 스크립트 값을 WebIDL <code>DOMString</code> 처럼 문자열로 바꾼다.
     *
     * @param v the value / 값
     * @return the string / 문자열
     */
    public static String string(JsValue v) {
        return Ops.toString(v);
    }
}
