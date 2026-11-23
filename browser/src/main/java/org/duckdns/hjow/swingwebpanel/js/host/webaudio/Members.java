package org.duckdns.hjow.swingwebpanel.js.host.webaudio;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.dom.HostObject;

/**
 * Installs the members of an interface on its prototype from short specifications, by reflection on the public Java API of the engine class that the script
 * object stands for. The point is that an interface of the Web Audio API is almost entirely a list of attributes and operations whose script form follows from
 * the Java form: the attribute <code>fftSize</code> is <code>getFftSize()</code> and <code>setFftSize(int)</code>, the operation
 * <code>setValueAtTime(float, double)</code> is a method with those parameters. The conversions follow the Java types: <code>float</code> and <code>double</code> need
 * a finite number (a <code>TypeError</code> otherwise, as WebIDL says), <code>int</code> is an <code>unsigned long</code>, an enumeration of the engine is a string of its script
 * names (an invalid name is ignored when an <em>attribute</em> is assigned and is a <code>TypeError</code> for an <em>argument</em>), <code>float[]</code> and
 * <code>double[]</code> are sequences. The results follow the Java types too (an {@code AudioParam} becomes its wrapper, an enumeration becomes its name). All the
 * names are resolved when the interface is defined, so a wrong specification fails when a page is created (the tests create a page), never while a script runs.
 * <p>
 * Specifications. Attributes: <code>a:name</code> (read only) or <code>a:name=</code> (read and write); the Java accessors are <code>getName</code> or <code>isName</code> and
 * <code>setName</code>; <code>a:jsName@JavaBase</code> names another Java base. Operations: <code>name</code>, <code>name/arity</code> to choose among overloads, or
 * <code>jsName@javaName/arity</code>.
 * <p>
 * 인터페이스의 멤버를 짧은 명세에서 프로토타입에 설치한다. 스크립트 객체가 나타내는 엔진 클래스의 공개 Java API 를 리플렉션으로 쓴다. Web Audio API 의 인터페이스는 거의
 * 전부가 스크립트 형태가 Java 형태에서 따라 나오는 속성과 연산의 목록이기 때문이다: 속성 <code>fftSize</code> 는 <code>getFftSize()</code> 와
 * <code>setFftSize(int)</code>, 연산 <code>setValueAtTime(float, double)</code> 은 그 매개변수를 가진 메서드이다. 변환은 Java 형식을 따른다: <code>float</code>, <code>double</code> 은
 * 유한한 수가 필요하고(아니면 WebIDL 대로 <code>TypeError</code>), <code>int</code> 는 <code>unsigned long</code>, 엔진의 열거형은 스크립트 이름 문자열(잘못된 이름은 <em>속성</em> 대입에서는 무시되고
 * <em>인수</em>에서는 <code>TypeError</code>), <code>float[]</code> 와 <code>double[]</code> 는 시퀀스이다. 결과도 Java 형식을 따른다({@code AudioParam} 은 래퍼가 되고 열거형은 이름이 된다).
 * 이름은 모두 인터페이스를 정의할 때 찾으므로 잘못된 명세는 스크립트가 실행되는 중이 아니라 페이지를 만들 때(시험이 페이지를 만든다) 실패한다.
 * <p>
 * 명세. 속성: <code>a:name</code>(읽기 전용) 또는 <code>a:name=</code>(읽기/쓰기). Java 접근자는 <code>getName</code> 또는 <code>isName</code> 과 <code>setName</code>이며
 * <code>a:jsName@JavaBase</code> 는 다른 Java 기본 이름을 쓴다. 연산: <code>name</code>, 오버로드 중 고르는 <code>name/arity</code>, <code>jsName@javaName/arity</code>.
 */
final class Members {

    /**
     * The owner of the script interface.
     * <p>
     * 스크립트 인터페이스의 소유자.
     */
    private final WebAudioApi api;

    /**
     * Creates the installer.
     * <p>
     * 설치기를 만든다.
     *
     * @param api the owner of the script interface / 스크립트 인터페이스의 소유자
     */
    Members(WebAudioApi api) {
        this.api = api;
    }

    /**
     * Returns the engine object that a script object stands for: the engine node of a node value, otherwise the target itself (a parameter, a listener, a wave).
     * A receiver that is not such an object is the "Illegal invocation" <code>TypeError</code>.
     * <p>
     * 스크립트 객체가 나타내는 엔진 객체를 반환한다: 노드 값이면 엔진 노드, 아니면 대상 자신(파라미터, 청취자, 파형). 그런 객체가 아닌 수신자는 "Illegal invocation"
     * <code>TypeError</code> 이다.
     *
     * @param self   the receiver / 수신자
     * @param engine the engine class that the receiver must be an instance of / 수신자가 인스턴스여야 하는 엔진 클래스
     * @return the engine object / 엔진 객체
     */
    static Object target(JsValue self, Class<?> engine) {
        Object t = self instanceof HostObject ? ((HostObject) self).getTarget() : null;
        if (t instanceof NodeValue) {
            t = ((NodeValue) t).node();
        }
        if (t == null || !engine.isInstance(t)) {
            throw JsException.typeError("Illegal invocation");
        }
        return t;
    }

    /**
     * Converts a Java result to a script value.
     * <p>
     * Java 결과를 스크립트 값으로 바꾼다.
     *
     * @param r the result / 결과
     * @return the value / 값
     */
    JsValue toJs(Object r) {
        if (r == null) {
            return Js.NULL;
        } else if (r instanceof Number) {
            return Js.num(((Number) r).doubleValue());
        } else if (r instanceof Boolean) {
            return Js.bool(((Boolean) r).booleanValue());
        } else if (r instanceof String) {
            return Js.str((String) r);
        } else if (r instanceof Enum) {
            return Js.str(Idl.enumName((Enum<?>) r));
        }
        return api.bridge().toJs(r);
    }

    /**
     * Converts a script value to a Java argument of a type.
     * <p>
     * 스크립트 값을 어떤 형식의 Java 인수로 바꾼다.
     *
     * @param t        the Java type / Java 형식
     * @param v        the value / 값
     * @param strict   true for an argument (an invalid enumeration name is a TypeError), false for an attribute (it gives null, which the setter ignores) / 인수이면 true
     *                 (잘못된 열거형 이름은 TypeError), 속성이면 false(null 을 주고 setter 는 무시한다)
     * @param typeName the WebIDL name of an enumeration for the message / 열거형의 WebIDL 이름(메시지용)
     * @return the Java value / Java 값
     */
    Object fromJs(Class<?> t, JsValue v, boolean strict, String typeName) {
        if (t == float.class) {
            return Float.valueOf(Idl.flt(v));
        } else if (t == double.class) {
            return Double.valueOf(Idl.dbl(v));
        } else if (t == int.class) {
            return Integer.valueOf(Idl.ulong(v));
        } else if (t == boolean.class) {
            return Boolean.valueOf(Ops.toBoolean(v));
        } else if (t == String.class) {
            return Ops.toString(v);
        } else if (t == float[].class) {
            return Idl.floatSequence(api.realm(), v, "The provided value");
        } else if (t == double[].class) {
            return Idl.doubleSequence(api.realm(), v, "The provided value");
        } else if (t.isEnum()) {
            return strict ? Idl.enumStrict(t, v, typeName) : Idl.enumByName(t, Ops.toString(v));
        }
        throw new IllegalArgumentException("unsupported parameter type " + t.getName());
    }

    /**
     * Installs attributes.
     * <p>
     * 속성을 설치한다.
     *
     * @param proto  the prototype / 프로토타입
     * @param engine the engine class / 엔진 클래스
     * @param iface  the WebIDL name of the interface for the messages / 메시지에 쓸 인터페이스의 WebIDL 이름
     * @param specs  the specifications, see the class description / 명세. 클래스 설명 참고
     */
    void attributes(JsObject proto, final Class<?> engine, String iface, String... specs) {
        // 명세마다 속성 하나를 설치한다.
        for (int i = 0; i < specs.length; i++) {
            attribute(proto, engine, iface, specs[i]);
        }
    }

    /**
     * Installs one attribute.
     * <p>
     * 속성 하나를 설치한다.
     *
     * @param proto  the prototype / 프로토타입
     * @param engine the engine class / 엔진 클래스
     * @param iface  the interface name / 인터페이스 이름
     * @param spec   the specification / 명세
     */
    private void attribute(JsObject proto, final Class<?> engine, final String iface, String spec) {
        if (!spec.startsWith("a:") && !spec.startsWith("p:")) {
            throw new IllegalArgumentException("bad attribute specification " + spec);
        }
        String rest = spec.substring(2);
        String javaBase = null;
        int at = rest.indexOf('@');
        if (at >= 0) {
            javaBase = rest.substring(at + 1);
            rest = rest.substring(0, at);
        }
        boolean writable = rest.endsWith("=");
        if (writable) {
            rest = rest.substring(0, rest.length() - 1);
        }
        final String name = rest;
        String base = javaBase != null ? javaBase : Character.toUpperCase(name.charAt(0)) + name.substring(1);
        Method g = null;
        try {
            g = engine.getMethod("get" + base);
        } catch (NoSuchMethodException e) {
            try {
                g = engine.getMethod("is" + base);
            } catch (NoSuchMethodException e2) {
                throw new IllegalArgumentException("no getter for " + iface + "." + name + " in " + engine.getName());
            }
        }
        final Method getter = g;
        Method s = null;
        if (writable) {
            s = Idl.find(engine, "set" + base, 1);
        }
        final Method setter = s;
        final Class<?> setType = s == null ? null : s.getParameterTypes()[0];
        api.accessor(proto, name, new WebAudioApi.Op() {
            /**
             * Reads the attribute.
             * <p>
             * 속성을 읽는다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the value / 값
             */
            public JsValue call(JsValue self, JsValue[] args) {
                return toJs(invoke(getter, target(self, engine), new Object[0]));
            }
        }, setter == null ? null : new WebAudioApi.Op() {
            /**
             * Writes the attribute.
             * <p>
             * 속성을 쓴다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsValue self, JsValue[] args) {
                Object t = target(self, engine);
                JsValue v = Js.arg(args, 0);
                Object arg = fromJs(setType, v, false, iface + "." + name);
                // 속성에 대입한 열거형의 잘못된 이름은 규격대로 조용히 무시한다.
                if (arg == null && setType.isEnum()) {
                    return Js.UNDEFINED;
                }
                invoke(setter, t, new Object[] {arg});
                return Js.UNDEFINED;
            }
        });
    }

    /**
     * Installs operations.
     * <p>
     * 연산을 설치한다.
     *
     * @param proto  the prototype / 프로토타입
     * @param engine the engine class / 엔진 클래스
     * @param iface  the WebIDL name of the interface for the messages / 메시지에 쓸 인터페이스의 WebIDL 이름
     * @param specs  the specifications, see the class description / 명세. 클래스 설명 참고
     */
    void methods(JsObject proto, final Class<?> engine, final String iface, String... specs) {
        // 명세마다 연산 하나를 설치한다.
        for (int i = 0; i < specs.length; i++) {
            method(proto, engine, iface, specs[i]);
        }
    }

    /**
     * Installs one operation.
     * <p>
     * 연산 하나를 설치한다.
     *
     * @param proto  the prototype / 프로토타입
     * @param engine the engine class / 엔진 클래스
     * @param iface  the interface name / 인터페이스 이름
     * @param spec   the specification / 명세
     */
    private void method(JsObject proto, final Class<?> engine, final String iface, String spec) {
        String rest = spec;
        int arity = -1;
        int slash = rest.lastIndexOf('/');
        if (slash >= 0) {
            arity = Integer.parseInt(rest.substring(slash + 1));
            rest = rest.substring(0, slash);
        }
        String jsName = rest;
        String javaName = rest;
        int at = rest.indexOf('@');
        if (at >= 0) {
            jsName = rest.substring(0, at);
            javaName = rest.substring(at + 1);
        }
        final String name = jsName;
        final Method m = Idl.find(engine, javaName, arity);
        final Class<?>[] types = m.getParameterTypes();
        final String[] typeNames = new String[types.length];
        // 열거형 매개변수의 메시지용 이름을 정한다.
        for (int i = 0; i < types.length; i++) {
            typeNames[i] = types[i].getSimpleName();
        }
        api.method(proto, name, types.length, new WebAudioApi.Op() {
            /**
             * Calls the engine method with converted arguments.
             * <p>
             * 변환한 인수로 엔진 메서드를 호출한다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the converted result / 변환한 결과
             */
            public JsValue call(JsValue self, JsValue[] args) {
                Object t = target(self, engine);
                Idl.requireArgs(args, types.length, name, iface);
                Object[] jargs = new Object[types.length];
                // 인수마다 매개변수 형식으로 바꾼다.
                for (int i = 0; i < types.length; i++) {
                    jargs[i] = fromJs(types[i], args[i], true, typeNames[i]);
                }
                Object r = invoke(m, t, jargs);
                return m.getReturnType() == void.class ? Js.UNDEFINED : toJs(r);
            }
        });
    }

    /**
     * Invokes an engine method and passes on the exceptions: the engine's own exceptions, the script exceptions and the runtime exceptions are thrown as they are
     * (the function wrapper of {@link WebAudioApi} turns an engine exception into a script exception).
     * <p>
     * 엔진 메서드를 호출하고 예외를 그대로 전한다: 엔진 자신의 예외, 스크립트 예외, 런타임 예외는 그대로 던진다({@link WebAudioApi} 의 함수 래퍼가 엔진 예외를 스크립트
     * 예외로 바꾼다).
     *
     * @param m      the method / 메서드
     * @param target the receiver / 수신 객체
     * @param args   the arguments / 인수
     * @return the result / 결과
     */
    static Object invoke(Method m, Object target, Object[] args) {
        try {
            return m.invoke(target, args);
        } catch (InvocationTargetException e) {
            Throwable c = e.getCause();
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
     * Returns the realm of the page (a convenience for the installers).
     * <p>
     * 페이지의 realm 을 반환한다(설치기를 위한 편의).
     *
     * @return the realm / realm
     */
    JsRealm realm() {
        return api.realm();
    }

    /**
     * Returns the DOM bridge of the page (a convenience for the installers).
     * <p>
     * 페이지의 DOM 브리지를 반환한다(설치기를 위한 편의).
     *
     * @return the bridge / 브리지
     */
    DomBridge bridge() {
        return api.bridge();
    }
}
