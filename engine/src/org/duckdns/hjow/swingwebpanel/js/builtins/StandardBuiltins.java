package org.duckdns.hjow.swingwebpanel.js.builtins;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.duckdns.hjow.swingwebpanel.js.BuiltinInstaller;
import org.duckdns.hjow.swingwebpanel.js.ExecutionLimits;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;

/**
 * Installs the standard built-in objects into a realm, in a fixed order. First come the minimal built-ins that the language
 * itself needs (Object, Function, Error types, Symbol, iterators and generators, Array basics, primitive wrappers, global functions
 * and the console); after them the optional installers of the full built-in library are loaded by class name. A class that does
 * not exist is silently skipped (so that library parts can be delivered independently); any other failure, including an
 * exception thrown by an installer, propagates.
 * <p>
 * Later installers may extend or override properties created by earlier ones with {@link org.duckdns.hjow.swingwebpanel.js.JsObject#defineBuiltin};
 * all minimal definitions are therefore writable and configurable as the specification says.
 * <p>
 * 표준 내장 객체를 정해진 순서로 realm 에 설치한다. 먼저 언어 자체가 필요로 하는 최소 내장 객체(Object, Function, 오류 형식, Symbol,
 * 이터레이터와 제너레이터, Array 기본, 원시 값 래퍼, 전역 함수, 콘솔)를 설치하고, 그 뒤에 전체 내장 라이브러리의 선택적 설치기를
 * 클래스 이름으로 불러온다. 존재하지 않는 클래스는 조용히 건너뛰며(라이브러리의 각 부분을 따로 전달할 수 있도록), 설치기가 던진 예외를
 * 포함한 그 밖의 실패는 그대로 전파한다.
 * <p>
 * 뒤의 설치기는 {@link org.duckdns.hjow.swingwebpanel.js.JsObject#defineBuiltin} 으로 앞의 설치기가 만든 프로퍼티를 확장하거나 덮어쓸 수 있다.
 * 그래서 모든 최소 정의는 규격대로 writable 이고 configurable 이다.
 */
public final class StandardBuiltins {

    /**
     * Class names of the optional installers of the full library, loaded in this order after the minimal built-ins.
     * <p>
     * 최소 내장 객체 뒤에 이 순서대로 불러오는 전체 라이브러리 선택적 설치기의 클래스 이름.
     */
    private static final String[] OPTIONAL_INSTALLERS = {
        "org.duckdns.hjow.swingwebpanel.js.builtins.corelib.CoreLibInstaller",
        "org.duckdns.hjow.swingwebpanel.js.builtins.stringarray.StringArrayInstaller",
        "org.duckdns.hjow.swingwebpanel.js.builtins.datajson.DataLibInstaller",
        "org.duckdns.hjow.swingwebpanel.js.builtins.typed.TypedLibInstaller",
        "org.duckdns.hjow.swingwebpanel.js.builtins.meta.MetaLibInstaller",
    };

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private StandardBuiltins() {
    }

    /**
     * Installs all standard built-ins into a realm.
     * <p>
     * 모든 표준 내장 객체를 realm 에 설치한다.
     *
     * @param realm the realm / realm
     */
    public static void installAll(JsRealm realm) {
        new ObjectBuiltins().install(realm);
        new FunctionBuiltins().install(realm);
        new ErrorBuiltins().install(realm);
        new SymbolBuiltins().install(realm);
        new IteratorBuiltins().install(realm);
        new ArrayBuiltins().install(realm);
        new PrimitiveBuiltins().install(realm);
        new GlobalBuiltins().install(realm);
        // 전체 라이브러리의 선택적 설치기를 순서대로 불러온다. 클래스가 없으면 건너뛴다.
        for (int i = 0; i < OPTIONAL_INSTALLERS.length; i++) {
            BuiltinInstaller installer = loadInstaller(OPTIONAL_INSTALLERS[i]);
            if (installer != null) {
                installer.install(realm);
            }
        }
        connectRegExpCancellation(realm);
    }

    /**
     * Connects the cancellation hook of the regular expression built-in (when it is present) to the execution limits of the realm,
     * so that a long running match stops on cancellation or when the time limit of the task is exceeded. The regular expression
     * library is reached by name because it is an optional part of the library set.
     * <p>
     * 정규식 내장 객체(있는 경우)의 취소 훅을 realm 의 실행 제한에 연결해, 오래 걸리는 일치가 취소되거나 작업의 시간 제한을 넘으면 멈추게 한다.
     * 정규식 라이브러리는 라이브러리 묶음의 선택 요소이므로 이름으로 찾는다.
     *
     * @param realm the realm / realm
     */
    private static void connectRegExpCancellation(JsRealm realm) {
        final ExecutionLimits limits = realm.getLimits();
        Class<?> hookType;
        Class<?> builtins;
        try {
            hookType = Class.forName("org.duckdns.hjow.swingwebpanel.js.regexp.RegExpCancel");
            builtins = Class.forName("org.duckdns.hjow.swingwebpanel.js.builtins.datajson.RegExpBuiltins");
        } catch (ClassNotFoundException e) {
            return;
        }
        // 훅은 realm 이 아니라 제한 객체만 붙잡는다(realm 을 키로 쓰는 약한 맵이 훅 때문에 비워지지 않게 한다).
        Object hook = Proxy.newProxyInstance(StandardBuiltins.class.getClassLoader(), new Class<?>[] { hookType }, new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) {
                String n = method.getName();
                if (n.equals("isCancelled")) {
                    return Boolean.valueOf(limits.isAbortRequested());
                }
                if (n.equals("hashCode")) {
                    return Integer.valueOf(System.identityHashCode(proxy));
                }
                if (n.equals("equals")) {
                    return Boolean.valueOf(proxy == args[0]);
                }
                return "RegExpCancel connected to ExecutionLimits";
            }
        });
        try {
            Method set = builtins.getMethod("setCancelHook", JsRealm.class, hookType);
            set.invoke(null, realm, hook);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("RegExpBuiltins.setCancelHook is missing: " + e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("RegExpBuiltins.setCancelHook is not accessible: " + e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("RegExpBuiltins.setCancelHook failed: " + e.getCause());
        }
    }

    /**
     * Creates an optional installer by class name.
     * <p>
     * 클래스 이름으로 선택적 설치기를 만든다.
     *
     * @param className the class name / 클래스 이름
     * @return the installer or null if the class does not exist / 설치기. 클래스가 없으면 null
     */
    /**
     * Creates an optional installer by class name.
     * <p>
     * 클래스 이름으로 선택적 설치기를 만든다.
     *
     * @param className the class name / 클래스 이름
     * @return the installer or null if the class does not exist / 설치기. 클래스가 없으면 null
     */
    private static BuiltinInstaller loadInstaller(String className) {
        Class<?> c;
        try {
            c = Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
        try {
            return (BuiltinInstaller) c.newInstance();
        } catch (InstantiationException e) {
            throw new IllegalStateException("Cannot create built-in installer " + className + ": " + e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot create built-in installer " + className + ": " + e);
        }
    }
}
