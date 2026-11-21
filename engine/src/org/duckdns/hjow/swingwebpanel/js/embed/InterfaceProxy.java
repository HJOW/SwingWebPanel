package org.duckdns.hjow.swingwebpanel.js.embed;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsValue;

/**
 * Implements a Java interface with the functions of a script object (or with the global functions): a call of an interface method calls the
 * function with the same name and converts arguments and result by the rules of {@link JsEngine}. A script function that is itself the
 * target and has no property with the method's name implements an interface with a single method (a "functional interface" such as
 * <code>Runnable</code>). The reflection here only builds the Java proxy object; scripts get no access to Java classes through it.
 * <p>
 * Java 인터페이스를 스크립트 객체의 함수(또는 전역 함수)로 구현한다. 인터페이스 메서드를 호출하면 같은 이름의 함수를 호출하고 인수와 결과를
 * {@link JsEngine} 의 규칙대로 바꾼다. 대상이 스크립트 함수 자신이고 메서드 이름의 프로퍼티가 없으면, 메서드가 하나뿐인 인터페이스
 * (<code>Runnable</code> 같은 "함수형 인터페이스")를 구현한다. 여기서 리플렉션은 Java 프록시 객체를 만드는 데만 쓰이며 스크립트가 이를 통해
 * Java 클래스에 접근하는 일은 없다.
 */
final class InterfaceProxy implements InvocationHandler {

    /**
     * The engine that owns the script object.
     * <p>
     * 스크립트 객체를 가진 엔진.
     */
    private final JsEngine engine;

    /**
     * The script object whose functions implement the interface (the global object for global functions).
     * <p>
     * 함수로 인터페이스를 구현하는 스크립트 객체(전역 함수이면 전역 객체).
     */
    private final JsObject target;

    /**
     * The interface.
     * <p>
     * 인터페이스.
     */
    private final Class<?> iface;

    /**
     * Creates the handler.
     * <p>
     * 핸들러를 만든다.
     *
     * @param engine the engine / 엔진
     * @param target the script object / 스크립트 객체
     * @param iface  the interface / 인터페이스
     */
    private InterfaceProxy(JsEngine engine, JsObject target, Class<?> iface) {
        this.engine = engine;
        this.target = target;
        this.iface = iface;
    }

    /**
     * Creates a proxy that implements an interface with a script object.
     * <p>
     * 스크립트 객체로 인터페이스를 구현하는 프록시를 만든다.
     *
     * @param engine     the engine / 엔진
     * @param target     the script object, or null for the global object / 스크립트 객체. 전역 객체이면 null
     * @param iface      the interface / 인터페이스
     * @param requireAll true to return null unless the object implements every method of the interface / 인터페이스의 모든 메서드를 객체가
     *                   구현하지 않으면 null 을 돌려주려면 true
     * @return the proxy, or null if <code>requireAll</code> is set and a method is missing / 프록시. <code>requireAll</code> 이고 메서드가 없으면 null
     * @throws IllegalArgumentException if the type is not an interface / 인터페이스가 아니면
     */
    static Object create(JsEngine engine, JsObject target, Class<?> iface, boolean requireAll) {
        if (iface == null || !iface.isInterface()) {
            throw new IllegalArgumentException("not an interface: " + iface);
        }
        JsObject object = target == null ? engine.getRealm().getGlobalObject() : target;
        Method[] methods = iface.getMethods();
        // 모든 메서드를 구현해야 하면 객체가 같은 이름의 함수를 가졌는지 먼저 확인한다.
        for (int i = 0; requireAll && i < methods.length; i++) {
            if (!implemented(object, methods[i], methods.length)) {
                return null;
            }
        }
        return Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] { iface }, new InterfaceProxy(engine, object, iface));
    }

    /**
     * Tells whether the object has a function for an interface method.
     * <p>
     * 객체가 인터페이스 메서드에 해당하는 함수를 가졌는지 알려 준다.
     *
     * @param object      the script object / 스크립트 객체
     * @param m           the method / 메서드
     * @param methodCount the number of methods of the interface / 인터페이스의 메서드 수
     * @return true if it is implemented / 구현되어 있으면 true
     */
    private static boolean implemented(JsObject object, Method m, int methodCount) {
        JsValue f = object.get(m.getName());
        if (f.isCallable()) {
            return true;
        }
        return object.isCallable() && methodCount == 1;
    }

    /**
     * Handles a call of an interface method.
     * <p>
     * 인터페이스 메서드의 호출을 처리한다.
     *
     * @param proxy  the proxy / 프록시
     * @param method the method / 메서드
     * @param args   the arguments or null / 인수. 없으면 null
     * @return the result / 결과
     * @throws Throwable a script error as {@link JsScriptError}, or the failure of a Java call in a host function / 스크립트 오류는 {@link JsScriptError} 로
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            String n = method.getName();
            if (n.equals("equals")) {
                return Boolean.valueOf(proxy == args[0]);
            }
            if (n.equals("hashCode")) {
                return Integer.valueOf(System.identityHashCode(proxy));
            }
            return "script implementation of " + iface.getName();
        }
        synchronized (engine) {
            try {
                JsValue f = target.get(method.getName());
                JsValue self = target;
                // 같은 이름의 함수가 없으면 대상이 함수 자신인 경우(함수형 인터페이스)에 대상을 직접 호출한다.
                if (!f.isCallable() && target.isCallable()) {
                    f = target;
                    self = Js.UNDEFINED;
                }
                if (!f.isCallable()) {
                    throw new UnsupportedOperationException("the script does not implement " + iface.getName() + "." + method.getName());
                }
                int n = args == null ? 0 : args.length;
                JsValue[] jsArgs = new JsValue[n];
                // 인수를 스크립트 값으로 바꾼다.
                for (int i = 0; i < n; i++) {
                    jsArgs[i] = JavaValues.toJs(engine, args[i]);
                }
                JsValue result = engine.callValue(f, self, jsArgs);
                Object converted = JavaValues.coerce(engine, result, method.getReturnType());
                engine.runJobs();
                return converted;
            } catch (JsException e) {
                throw engine.wrap(e);
            }
        }
    }
}
