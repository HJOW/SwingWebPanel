package org.duckdns.hjow.swingwebpanel.js.host.web;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;

/**
 * Small helpers that all web API installers share: defining interfaces (constructor, prototype, <code>Symbol.toStringTag</code>), binding
 * the methods of an installer object to script-visible functions, and the WebIDL conversions that the Fetch, XHR and URL standards use
 * (<code>USVString</code>, <code>ByteString</code>, argument counts). Methods are bound by name with reflection on a public API of the
 * JRE, so a mistyped name fails when the page is created (the unit tests create a page), never while a script runs. Nothing of the Java
 * side becomes visible to scripts: only the functions created here are.
 * <p>
 * 모든 웹 API 설치기가 함께 쓰는 작은 도우미: 인터페이스 정의(생성자, 프로토타입, <code>Symbol.toStringTag</code>), 설치기 객체의 메서드를 스크립트에 보이는
 * 함수에 묶기, Fetch/XHR/URL 표준이 쓰는 WebIDL 변환(<code>USVString</code>, <code>ByteString</code>, 인수 개수). 메서드는 JRE 의 공개 API 인 리플렉션으로
 * 이름을 묶으므로 이름이 틀리면 스크립트가 실행되는 중이 아니라 페이지를 만들 때(단위 시험이 페이지를 만든다) 실패한다. Java 쪽은 스크립트에 전혀 보이지
 * 않고 여기서 만든 함수만 보인다.
 */
final class Natives {

    /**
     * The result of {@link #defineClass}: the interface object and its prototype.
     * <p>
     * {@link #defineClass} 의 결과: 인터페이스 객체와 그 프로토타입.
     */
    static final class ClassDef {
        /** The interface object (the constructor). / 인터페이스 객체(생성자). */
        final NativeFunction constructor;

        /** The prototype object. / 프로토타입 객체. */
        final JsObject prototype;

        /**
         * Creates the pair.
         * <p>
         * 쌍을 만든다.
         *
         * @param constructor the constructor / 생성자
         * @param prototype   the prototype / 프로토타입
         */
        ClassDef(NativeFunction constructor, JsObject prototype) {
            this.constructor = constructor;
            this.prototype = prototype;
        }
    }

    /**
     * Calls a package-private method of an installer object by reflection; the method has the signature
     * <code>JsValue name(JsValue self, JsValue[] args)</code>.
     * <p>
     * 설치기 객체의 패키지 비공개 메서드를 리플렉션으로 부른다. 메서드의 시그니처는 <code>JsValue name(JsValue self, JsValue[] args)</code> 이다.
     */
    private static final class Bound implements NativeFunction.Call {
        /** The object that owns the method. / 메서드를 가진 객체. */
        private final Object owner;

        /** The method. / 메서드. */
        private final Method method;

        /**
         * Binds a method.
         * <p>
         * 메서드를 묶는다.
         *
         * @param owner  the object / 객체
         * @param method the method / 메서드
         */
        Bound(Object owner, Method method) {
            this.owner = owner;
            this.method = method;
        }

        /**
         * Invokes the bound method.
         * <p>
         * 묶은 메서드를 호출한다.
         *
         * @param realm the realm / realm
         * @param self  this value / this 값
         * @param args  arguments / 인수
         * @return the result / 결과
         */
        public JsValue call(JsRealm realm, JsValue self, JsValue[] args) {
            try {
                Object r = method.invoke(owner, new Object[] { self, args });
                return r == null ? Js.UNDEFINED : (JsValue) r;
            } catch (InvocationTargetException e) {
                Throwable c = e.getCause();
                // 스크립트 오류와 중단은 그대로 다시 던져 스크립트가 잡거나 작업이 끝나게 한다.
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
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private Natives() {
    }

    /**
     * Finds the handler method of an installer object.
     * <p>
     * 설치기 객체의 처리 메서드를 찾는다.
     *
     * @param owner the object / 객체
     * @param name  the Java method name / Java 메서드 이름
     * @return the method / 메서드
     */
    private static Method find(Object owner, String name) {
        try {
            return owner.getClass().getDeclaredMethod(name, new Class<?>[] { JsValue.class, JsValue[].class });
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("no handler " + name + " in " + owner.getClass().getName());
        }
    }

    /**
     * Creates a script function bound to a handler method.
     * <p>
     * 처리 메서드에 묶인 스크립트 함수를 만든다.
     *
     * @param realm  the realm / realm
     * @param name   the function name / 함수 이름
     * @param length the declared length / 선언된 길이
     * @param owner  the installer object / 설치기 객체
     * @param method the Java method name / Java 메서드 이름
     * @return the function / 함수
     */
    static NativeFunction function(JsRealm realm, String name, int length, Object owner, String method) {
        return realm.newFunction(name, length, new Bound(owner, find(owner, method)));
    }

    /**
     * Defines an operation (a method) on a prototype or an interface object.
     * <p>
     * 프로토타입이나 인터페이스 객체에 연산(메서드)을 정의한다.
     *
     * @param realm  the realm / realm
     * @param target the object / 객체
     * @param name   the script name / 스크립트 이름
     * @param length the declared length / 선언된 길이
     * @param owner  the installer object / 설치기 객체
     * @param method the Java method name / Java 메서드 이름
     */
    static void method(JsRealm realm, JsObject target, String name, int length, Object owner, String method) {
        target.defineData(name, function(realm, name, length, owner, method), true, true, true);
    }

    /**
     * Defines a read-only attribute.
     * <p>
     * 읽기 전용 속성을 정의한다.
     *
     * @param realm  the realm / realm
     * @param target the prototype / 프로토타입
     * @param name   the script name / 스크립트 이름
     * @param owner  the installer object / 설치기 객체
     * @param getter the Java getter method name / Java getter 메서드 이름
     */
    static void getter(JsRealm realm, JsObject target, String name, Object owner, String getter) {
        target.defineAccessor(name, function(realm, "get " + name, 0, owner, getter), null, true, true);
    }

    /**
     * Defines a read-write attribute.
     * <p>
     * 읽기/쓰기 속성을 정의한다.
     *
     * @param realm  the realm / realm
     * @param target the prototype / 프로토타입
     * @param name   the script name / 스크립트 이름
     * @param owner  the installer object / 설치기 객체
     * @param getter the Java getter method name / Java getter 메서드 이름
     * @param setter the Java setter method name / Java setter 메서드 이름
     */
    static void accessor(JsRealm realm, JsObject target, String name, Object owner, String getter, String setter) {
        target.defineAccessor(name, function(realm, "get " + name, 0, owner, getter), function(realm, "set " + name, 1, owner, setter), true,
                true);
    }

    /**
     * Defines an interface: constructor, prototype, <code>constructor</code> and <code>Symbol.toStringTag</code> properties, and the global
     * binding.
     * <p>
     * 인터페이스를 정의한다: 생성자, 프로토타입, <code>constructor</code> 와 <code>Symbol.toStringTag</code> 프로퍼티, 전역 바인딩.
     *
     * @param realm     the realm / realm
     * @param global    the global object / 전역 객체
     * @param name      the interface name / 인터페이스 이름
     * @param length    the constructor length / 생성자 길이
     * @param parent    the parent prototype or null / 부모 프로토타입 또는 null
     * @param construct the construct behaviour, or null if the interface cannot be constructed / 생성 동작. 생성할 수 없으면 null
     * @return the interface object and the prototype / 인터페이스 객체와 프로토타입
     */
    static ClassDef defineClass(JsRealm realm, JsObject global, final String name, int length, JsObject parent,
            NativeFunction.Construct construct) {
        JsObject proto = parent == null ? realm.newObject() : new JsObject(parent);
        NativeFunction.Construct c = construct;
        if (c == null) {
            c = new NativeFunction.Construct() {
                public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                    throw JsException.typeError("Illegal constructor");
                }
            };
        }
        NativeFunction ctor = new NativeFunction(realm, name, length, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                throw JsException.typeError("Failed to construct '" + name + "': Please use the 'new' operator, this DOM object constructor "
                        + "cannot be called as a function.");
            }
        }, c);
        ctor.defineData("prototype", proto, false, false, false);
        proto.defineBuiltin("constructor", ctor);
        proto.defineData(WellKnownSymbols.TO_STRING_TAG, Js.str(name), false, false, true);
        global.defineBuiltin(name, ctor);
        return new ClassDef(ctor, proto);
    }

    /**
     * Requires a number of arguments the way WebIDL operations do.
     * <p>
     * WebIDL 연산처럼 인수 개수를 요구한다.
     *
     * @param args     the arguments / 인수
     * @param required the required count / 필요한 개수
     * @param method   the method name for the message / 메시지용 메서드 이름
     * @param iface    the interface name for the message / 메시지용 인터페이스 이름
     */
    static void requireArgs(JsValue[] args, int required, String method, String iface) {
        if (args.length < required) {
            throw JsException.typeError("Failed to execute '" + method + "' on '" + iface + "': " + required + " argument"
                    + (required == 1 ? "" : "s") + " required, but only " + args.length + " present.");
        }
    }

    /**
     * Returns this value as an instance of a class, or throws the standard <code>Illegal invocation</code> TypeError.
     * <p>
     * this 값을 클래스의 인스턴스로 반환하거나 표준 <code>Illegal invocation</code> TypeError 를 던진다.
     *
     * @param <T>  the class / 클래스
     * @param self this value / this 값
     * @param type the class / 클래스
     * @return the instance / 인스턴스
     */
    static <T> T self(JsValue self, Class<T> type) {
        if (!type.isInstance(self)) {
            throw JsException.typeError("Illegal invocation");
        }
        return type.cast(self);
    }

    /**
     * Converts a value to a <code>USVString</code>: a string where every unpaired surrogate is replaced by U+FFFD.
     * <p>
     * 값을 <code>USVString</code> 으로 바꾼다. 짝 없는 서로게이트를 U+FFFD 로 바꾼 문자열이다.
     *
     * @param v the value / 값
     * @return the string / 문자열
     */
    static String usv(JsValue v) {
        return usv(Ops.toString(v));
    }

    /**
     * Replaces the unpaired surrogates of a string by U+FFFD.
     * <p>
     * 문자열의 짝 없는 서로게이트를 U+FFFD 로 바꾼다.
     *
     * @param s the string / 문자열
     * @return the scalar value string / 스칼라 값 문자열
     */
    static String usv(String s) {
        StringBuilder out = null;
        // 문자를 하나씩 보며 짝이 맞지 않는 서로게이트가 처음 나올 때 복사본을 만든다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean bad = false;
            if (Character.isHighSurrogate(c)) {
                if (i + 1 < s.length() && Character.isLowSurrogate(s.charAt(i + 1))) {
                    if (out != null) {
                        out.append(c).append(s.charAt(i + 1));
                    }
                    i++;
                    continue;
                }
                bad = true;
            } else if (Character.isLowSurrogate(c)) {
                bad = true;
            }
            if (bad && out == null) {
                out = new StringBuilder(s.length());
                out.append(s, 0, i);
            }
            if (out != null) {
                out.append(bad ? (char) 0xFFFD : c);
            }
        }
        return out == null ? s : out.toString();
    }

    /**
     * Converts a value to a <code>ByteString</code>: every code unit must fit in a byte, otherwise the WebIDL conversion throws TypeError.
     * <p>
     * 값을 <code>ByteString</code> 으로 바꾼다. 모든 코드 단위가 한 바이트에 들어가야 하며 아니면 WebIDL 변환이 TypeError 를 던진다.
     *
     * @param v      the value / 값
     * @param method the method name for the message / 메시지용 메서드 이름
     * @param iface  the interface name for the message / 메시지용 인터페이스 이름
     * @return the string / 문자열
     */
    static String byteString(JsValue v, String method, String iface) {
        String s = Ops.toString(v);
        // 255 를 넘는 코드 단위가 있으면 변환할 수 없다.
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) > 0xFF) {
                throw JsException.typeError("Failed to execute '" + method + "' on '" + iface
                        + "': String contains non ISO-8859-1 code point.");
            }
        }
        return s;
    }

    /**
     * Creates an iterator over a snapshot of values: the array iterator of a fresh array (the Web IDL "iterable" declaration uses a live
     * list, which only differs when a script changes the collection while iterating).
     * <p>
     * 값의 스냅샷에 대한 반복자를 만든다: 새 배열의 배열 반복자(WebIDL "iterable" 선언은 살아 있는 목록을 쓰며, 반복 중 스크립트가 컬렉션을 바꿀 때만
     * 차이가 난다).
     *
     * @param realm  the realm / realm
     * @param values the values / 값
     * @return the iterator object / 반복자 객체
     */
    static JsValue iterator(JsRealm realm, java.util.List<JsValue> values) {
        JsObject array = realm.newArray(values);
        JsObject valuesFn = realm.getIntrinsic("%ArrayProto_values%");
        return Ops.call(valuesFn, array, new JsValue[0]);
    }

    /**
     * Creates a two-element array <code>[a, b]</code>.
     * <p>
     * 두 요소 배열 <code>[a, b]</code> 를 만든다.
     *
     * @param realm the realm / realm
     * @param a     the first element / 첫 요소
     * @param b     the second element / 둘째 요소
     * @return the array / 배열
     */
    static JsValue pair(JsRealm realm, JsValue a, JsValue b) {
        return realm.newArray(new JsValue[] { a, b });
    }
}
