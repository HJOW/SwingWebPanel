package org.duckdns.hjow.swingwebpanel.js.dom;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Attr;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DocumentFragment;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.EventTarget;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.dom.Text;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;

/**
 * Parses the member specification strings of {@link DomBridge#defineInterface(String, String, Class, String[])} and installs the members
 * (attributes as accessor properties, operations as methods, constants, reflected content attributes) on the prototype. Java methods are
 * resolved when the interface is defined; an unknown method throws an {@link IllegalArgumentException} at start-up.
 * <p>
 * {@link DomBridge#defineInterface(String, String, Class, String[])} 의 멤버 명세 문자열을 해석해 멤버(접근자 프로퍼티인 속성, 메서드인 연산, 상수,
 * 반영되는 콘텐츠 속성)를 프로토타입에 설치한다. Java 메서드는 인터페이스를 정의할 때 찾으며 알 수 없는 메서드는 시작할 때
 * {@link IllegalArgumentException} 을 던진다.
 */
final class MemberInstaller {

    /**
     * An empty argument array.
     * <p>
     * 빈 인수 배열.
     */
    private static final Object[] NONE = new Object[0];

    /**
     * One parameter of an operation.
     * <p>
     * 연산의 매개변수 하나.
     */
    private static final class Param {
        /**
         * The Java parameter class.
         * <p>
         * Java 매개변수 클래스.
         */
        Class<?> type;
        /**
         * Whether null and undefined mean Java null.
         * <p>
         * null 과 undefined 가 Java null 인지 여부.
         */
        boolean nullable;
        /**
         * Whether the argument may be left out.
         * <p>
         * 인수를 생략할 수 있는지 여부.
         */
        boolean optional;
        /**
         * Whether this is the variable-length last parameter.
         * <p>
         * 가변 길이 마지막 매개변수인지 여부.
         */
        boolean varargs;
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private MemberInstaller() {
    }

    /**
     * Installs one member.
     * <p>
     * 멤버 하나를 설치한다.
     *
     * @param bridge the bridge / 브리지
     * @param info   the interface / 인터페이스
     * @param spec   the specification string / 명세 문자열
     */
    static void install(DomBridge bridge, DomBridge.InterfaceInfo info, String spec) {
        int colon = spec.indexOf(':');
        if (colon != 1) {
            throw new IllegalArgumentException("bad member specification: " + spec);
        }
        String rest = spec.substring(2);
        switch (spec.charAt(0)) {
        case 'a':
            attribute(bridge, info, rest);
            break;
        case 'm':
            operation(bridge, info, rest);
            break;
        case 'c':
            constant(bridge, info, rest);
            break;
        case 'r':
            reflect(bridge, info, rest);
            break;
        default:
            throw new IllegalArgumentException("bad member kind: " + spec);
        }
    }

    /**
     * Returns the Java target of a call after checking that the receiver is a wrapper of this interface.
     * <p>
     * 수신자가 이 인터페이스의 래퍼인지 확인한 뒤 호출의 Java 대상을 반환한다.
     *
     * @param info the interface / 인터페이스
     * @param self the this value / this 값
     * @return the Java target / Java 대상
     */
    private static Object target(DomBridge.InterfaceInfo info, JsValue self) {
        Object t = DomBridge.targetOf(self, info.javaClass);
        if (t == null) {
            throw JsException.typeError("Illegal invocation");
        }
        return t;
    }

    /**
     * Capitalizes the first letter.
     * <p>
     * 첫 글자를 대문자로 만든다.
     *
     * @param s the text / 문자열
     * @return the capitalized text / 대문자로 시작하는 문자열
     */
    private static String cap(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Finds a public method by name and parameter classes on the interface's Java class.
     * <p>
     * 인터페이스의 Java 클래스에서 이름과 매개변수 클래스로 공개 메서드를 찾는다.
     *
     * @param cls  the class / 클래스
     * @param name the method name / 메서드 이름
     * @param ps   the parameter classes / 매개변수 클래스
     * @return the method or null / 메서드 또는 null
     */
    private static Method find(Class<?> cls, String name, Class<?>[] ps) {
        try {
            return cls.getMethod(name, ps);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Installs an attribute (accessor property).
     * <p>
     * 속성(접근자 프로퍼티)을 설치한다.
     *
     * @param bridge the bridge / 브리지
     * @param info   the interface / 인터페이스
     * @param rest   the specification after <code>a:</code> / <code>a:</code> 뒤의 명세
     */
    private static void attribute(final DomBridge bridge, final DomBridge.InterfaceInfo info, String rest) {
        String base = null;
        int at = rest.indexOf('@');
        if (at >= 0) {
            base = rest.substring(at + 1);
            rest = rest.substring(0, at);
        }
        boolean nullToEmpty = rest.endsWith("?");
        if (nullToEmpty) {
            rest = rest.substring(0, rest.length() - 1);
        }
        boolean writable = rest.endsWith("=");
        if (writable) {
            rest = rest.substring(0, rest.length() - 1);
        }
        final String name = rest;
        String javaBase = base != null ? base : cap(name);
        Method g = find(info.javaClass, "get" + javaBase, new Class<?>[0]);
        if (g == null) {
            g = find(info.javaClass, "is" + javaBase, new Class<?>[0]);
        }
        if (g == null) {
            throw new IllegalArgumentException("no getter for " + info.name + "." + name + " in " + info.javaClass.getName());
        }
        final Method getter = g;
        JsRealm realm = bridge.getRealm();
        JsObject getFn = realm.newFunction("get " + name, 0, new NativeFunction.Call() {
            /**
             * Reads the attribute.
             * <p>
             * 속성을 읽는다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the value / 값
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return bridge.toJs(bridge.invoke(getter, target(info, self), NONE));
            }
        });
        JsObject setFn = null;
        if (writable) {
            Method s = null;
            Method[] all = info.javaClass.getMethods();
            // 이름이 setXxx 이고 매개변수가 하나인 공개 메서드를 찾는다.
            for (int i = 0; i < all.length && s == null; i++) {
                if (all[i].getName().equals("set" + javaBase) && all[i].getParameterTypes().length == 1) {
                    s = all[i];
                }
            }
            if (s == null) {
                throw new IllegalArgumentException("no setter for " + info.name + "." + name);
            }
            final Method setter = s;
            final Class<?> pt = s.getParameterTypes()[0];
            final boolean emptyForNull = nullToEmpty;
            setFn = realm.newFunction("set " + name, 1, new NativeFunction.Call() {
                /**
                 * Writes the attribute.
                 * <p>
                 * 속성을 쓴다.
                 *
                 * @param r    the realm / realm
                 * @param self this value / this 값
                 * @param args arguments / 인수
                 * @return undefined / undefined
                 */
                public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                    JsValue v = Js.arg(args, 0);
                    Object arg;
                    if (pt == String.class && emptyForNull && v.isNull()) {
                        arg = "";
                    } else {
                        Param p = new Param();
                        p.type = pt;
                        arg = convert(bridge, v, p, "set " + name, 0);
                    }
                    bridge.invoke(setter, target(info, self), new Object[] { arg });
                    return Js.UNDEFINED;
                }
            });
        }
        info.prototype.defineAccessor(name, getFn, setFn, true, true);
    }

    /**
     * Parses a parameter token such as <code>Node?</code>, <code>boolean*</code> or <code>Object...</code>.
     * <p>
     * <code>Node?</code>, <code>boolean*</code>, <code>Object...</code> 같은 매개변수 토큰을 해석한다.
     *
     * @param tok the token / 토큰
     * @return the parameter / 매개변수
     */
    private static Param param(String tok) {
        Param p = new Param();
        String t = tok.trim();
        if (t.endsWith("...")) {
            p.varargs = true;
            p.type = t.startsWith("String") ? String[].class : Object[].class;
            return p;
        }
        // 접미사 ?(null 허용)와 *(생략 가능)를 떼어 낸다.
        while (t.endsWith("?") || t.endsWith("*")) {
            if (t.endsWith("?")) {
                p.nullable = true;
            } else {
                p.optional = true;
            }
            t = t.substring(0, t.length() - 1);
        }
        p.type = classOf(t);
        return p;
    }

    /**
     * Resolves a type name of the specification.
     * <p>
     * 명세의 타입 이름을 클래스로 바꾼다.
     *
     * @param t the type name / 타입 이름
     * @return the class / 클래스
     */
    private static Class<?> classOf(String t) {
        if ("String".equals(t)) {
            return String.class;
        } else if ("int".equals(t)) {
            return int.class;
        } else if ("long".equals(t)) {
            return long.class;
        } else if ("short".equals(t)) {
            return short.class;
        } else if ("double".equals(t)) {
            return double.class;
        } else if ("boolean".equals(t)) {
            return boolean.class;
        } else if ("Boolean".equals(t)) {
            return Boolean.class;
        } else if ("Node".equals(t)) {
            return Node.class;
        } else if ("Element".equals(t)) {
            return Element.class;
        } else if ("Attr".equals(t)) {
            return Attr.class;
        } else if ("Text".equals(t)) {
            return Text.class;
        } else if ("Document".equals(t)) {
            return Document.class;
        } else if ("DocumentFragment".equals(t)) {
            return DocumentFragment.class;
        } else if ("Event".equals(t)) {
            return Event.class;
        } else if ("Range".equals(t)) {
            return org.duckdns.hjow.swingwebpanel.dom.Range.class;
        } else if ("EventTarget".equals(t)) {
            return EventTarget.class;
        } else if ("Object".equals(t)) {
            return Object.class;
        }
        throw new IllegalArgumentException("unknown parameter type " + t);
    }

    /**
     * Installs an operation (method).
     * <p>
     * 연산(메서드)을 설치한다.
     *
     * @param bridge the bridge / 브리지
     * @param info   the interface / 인터페이스
     * @param rest   the specification after <code>m:</code> / <code>m:</code> 뒤의 명세
     */
    private static void operation(final DomBridge bridge, final DomBridge.InterfaceInfo info, String rest) {
        int open = rest.indexOf('(');
        int close = rest.lastIndexOf(')');
        if (open < 0 || close < open) {
            throw new IllegalArgumentException("bad operation specification: " + rest);
        }
        final String name = rest.substring(0, open);
        String javaName = close + 1 < rest.length() && rest.charAt(close + 1) == '@' ? rest.substring(close + 2) : name;
        String plist = rest.substring(open + 1, close).trim();
        final List<Param> params = new ArrayList<Param>();
        if (plist.length() > 0) {
            String[] toks = plist.split(",");
            // 매개변수 토큰을 하나씩 해석한다.
            for (int i = 0; i < toks.length; i++) {
                params.add(param(toks[i]));
            }
        }
        Class<?>[] classes = new Class<?>[params.size()];
        int required = 0;
        // 매개변수 클래스 배열과 필수 인수 수를 계산한다.
        for (int i = 0; i < classes.length; i++) {
            classes[i] = params.get(i).type;
            if (!params.get(i).optional && !params.get(i).varargs) {
                required++;
            }
        }
        final Method m = find(info.javaClass, javaName, classes);
        if (m == null) {
            throw new IllegalArgumentException("no method " + javaName + " for " + info.name + "." + name + " in " + info.javaClass.getName());
        }
        final int need = required;
        JsObject fn = bridge.getRealm().newFunction(name, need, new NativeFunction.Call() {
            /**
             * Calls the Java method with converted arguments.
             * <p>
             * 변환한 인수로 Java 메서드를 호출한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the converted result / 변환한 결과
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Object t = target(info, self);
                if (args.length < need) {
                    throw JsException.typeError("Failed to execute '" + name + "' on '" + info.name + "': " + need + " argument"
                            + (need == 1 ? "" : "s") + " required, but only " + args.length + " present.");
                }
                Object[] jargs = new Object[params.size()];
                // 매개변수마다 JavaScript 인수를 Java 값으로 바꾼다.
                for (int i = 0; i < jargs.length; i++) {
                    Param p = params.get(i);
                    if (p.varargs) {
                        jargs[i] = rest(bridge, args, i, p.type);
                    } else if (i >= args.length) {
                        jargs[i] = defaultOf(p);
                    } else {
                        jargs[i] = convert(bridge, args[i], p, name, i);
                    }
                }
                Object result = bridge.invoke(m, t, jargs);
                return m.getReturnType() == void.class ? Js.UNDEFINED : bridge.toJs(result);
            }
        });
        info.prototype.defineData(name, fn, true, true, true);
    }

    /**
     * Collects the remaining arguments for a variable-length parameter: nodes stay nodes, everything else becomes a string.
     * <p>
     * 가변 길이 매개변수의 나머지 인수를 모은다. 노드는 노드로 두고 나머지는 문자열로 바꾼다.
     *
     * @param bridge the bridge / 브리지
     * @param args   all arguments / 모든 인수
     * @param from   the first index to collect / 모을 첫 위치
     * @param arrayType the Java array type of the parameter / 매개변수의 Java 배열 타입
     * @return the Java array / Java 배열
     */
    private static Object rest(DomBridge bridge, JsValue[] args, int from, Class<?> arrayType) {
        int n = Math.max(0, args.length - from);
        if (arrayType == String[].class) {
            String[] strings = new String[n];
            // 문자열 가변 인수는 모두 문자열로 바꾼다.
            for (int i = 0; i < n; i++) {
                strings[i] = Ops.toString(args[from + i]);
            }
            return strings;
        }
        Object[] out = new Object[n];
        // 남은 인수를 하나씩 노드 또는 문자열로 바꾼다.
        for (int i = 0; i < n; i++) {
            Object node = DomBridge.targetOf(args[from + i], Node.class);
            out[i] = node != null ? node : Ops.toString(args[from + i]);
        }
        return out;
    }

    /**
     * Returns the Java value for a left-out optional argument.
     * <p>
     * 생략된 선택 인수의 Java 값을 반환한다.
     *
     * @param p the parameter / 매개변수
     * @return the default value / 기본값
     */
    private static Object defaultOf(Param p) {
        if (p.type == boolean.class) {
            return Boolean.FALSE;
        } else if (p.type == int.class) {
            return Integer.valueOf(0);
        } else if (p.type == long.class) {
            return Long.valueOf(0);
        } else if (p.type == short.class) {
            return Short.valueOf((short) 0);
        } else if (p.type == double.class) {
            return Double.valueOf(0);
        }
        return null;
    }

    /**
     * Converts one script value to the Java parameter type.
     * <p>
     * 스크립트 값 하나를 Java 매개변수 타입으로 바꾼다.
     *
     * @param bridge the bridge / 브리지
     * @param v      the value / 값
     * @param p      the parameter / 매개변수
     * @param fn     the operation name for messages / 메시지용 연산 이름
     * @param index  the argument position / 인수 위치
     * @return the Java value / Java 값
     */
    private static Object convert(DomBridge bridge, JsValue v, Param p, String fn, int index) {
        Class<?> t = p.type;
        if (p.nullable && v.isNullOrUndefined()) {
            return null;
        }
        if (t == String.class) {
            return Ops.toString(v);
        } else if (t == boolean.class) {
            return Boolean.valueOf(Ops.toBoolean(v));
        } else if (t == Boolean.class) {
            return v.isNullOrUndefined() ? null : Boolean.valueOf(Ops.toBoolean(v));
        } else if (t == int.class) {
            return Integer.valueOf(Ops.toInt32(v));
        } else if (t == short.class) {
            return Short.valueOf((short) Ops.toInt32(v));
        } else if (t == long.class) {
            return Long.valueOf((long) Ops.toInteger(v));
        } else if (t == double.class) {
            return Double.valueOf(Ops.toNumber(v));
        } else if (t == Object.class) {
            Object h = v instanceof HostObject ? ((HostObject) v).getTarget() : null;
            return h != null ? h : Ops.toString(v);
        }
        Object target = DomBridge.targetOf(v, t);
        if (target == null) {
            throw JsException.typeError("Failed to execute '" + fn + "': parameter " + (index + 1) + " is not of type '" + t.getSimpleName()
                    + "'.");
        }
        return target;
    }

    /**
     * Installs an integer constant on the prototype and on the interface object.
     * <p>
     * 정수 상수를 프로토타입과 인터페이스 객체에 설치한다.
     *
     * @param bridge the bridge / 브리지
     * @param info   the interface / 인터페이스
     * @param rest   <code>NAME=value</code> / <code>NAME=value</code>
     */
    private static void constant(DomBridge bridge, DomBridge.InterfaceInfo info, String rest) {
        int eq = rest.indexOf('=');
        String name = rest.substring(0, eq);
        JsValue v = Js.num(Integer.parseInt(rest.substring(eq + 1).trim()));
        info.prototype.defineData(name, v, false, true, false);
        info.constructor.defineData(name, v, false, true, false);
        info.constants.put(name, v);
    }

    /**
     * Installs a reflected content attribute: <code>prop:attribute:type</code>.
     * <p>
     * 반영되는 콘텐츠 속성을 설치한다: <code>prop:attribute:type</code>.
     *
     * @param bridge the bridge / 브리지
     * @param info   the interface / 인터페이스
     * @param rest   the specification after <code>r:</code> / <code>r:</code> 뒤의 명세
     */
    private static void reflect(final DomBridge bridge, final DomBridge.InterfaceInfo info, String rest) {
        String[] parts = rest.split(":", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("bad reflection specification: " + rest);
        }
        final String prop = parts[0];
        final String attr = parts[1].length() == 0 ? prop.toLowerCase() : parts[1];
        final String type = parts[2];
        JsRealm realm = bridge.getRealm();
        JsObject getFn = realm.newFunction("get " + prop, 0, new NativeFunction.Call() {
            /**
             * Reads the content attribute.
             * <p>
             * 콘텐츠 속성을 읽는다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the value / 값
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Element e = (Element) target(info, self);
                String v = e.getAttribute(attr);
                if ("bool".equals(type)) {
                    return Js.bool(e.hasAttribute(attr));
                } else if ("nstring".equals(type)) {
                    // nstring 은 속성이 없으면 null 을 읽는 반영 속성(ARIA 의 role, aria-*)이다.
                    return v == null ? Js.NULL : Js.str(v);
                } else if (type.startsWith("int") || "uint".equals(type) || "uint1".equals(type) || "uintd1".equals(type)) {
                    // uint1 은 "0 보다 큰 수로 제한하고 대체값은 1"인 반영 속성(span, colSpan)이다.
                    // uintd1 은 "0 이상 65534 이하로 제한하고 대체값은 1"인 반영 속성(rowSpan)이다.
                    int dflt = "int-1".equals(type) ? -1 : ("uint1".equals(type) || "uintd1".equals(type) ? 1 : 0);
                    try {
                        int n = v == null ? dflt : Integer.parseInt(v.trim());
                        if ("uintd1".equals(type)) {
                            return Js.num(n < 0 ? 1 : Math.min(n, 65534));
                        }
                        return Js.num("uint1".equals(type) && n < 1 ? 1 : n);
                    } catch (NumberFormatException ex) {
                        return Js.num(dflt);
                    }
                } else if ("url".equals(type)) {
                    if (v == null) {
                        return Js.str("");
                    }
                    try {
                        return Js.str(UrlUtil.resolve(e.document().getBaseUri(), v));
                    } catch (RuntimeException ex) {
                        return Js.str(v);
                    }
                }
                return Js.str(v == null ? "" : v);
            }
        });
        JsObject setFn = realm.newFunction("set " + prop, 1, new NativeFunction.Call() {
            /**
             * Writes the content attribute.
             * <p>
             * 콘텐츠 속성을 쓴다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Element e = (Element) target(info, self);
                JsValue v = Js.arg(args, 0);
                if ("bool".equals(type)) {
                    if (Ops.toBoolean(v)) {
                        e.setAttribute(attr, "");
                    } else {
                        e.removeAttribute(attr);
                    }
                } else if ("nstring".equals(type)) {
                    // null 을 쓰면 속성을 지운다.
                    if (v.isNull()) {
                        e.removeAttribute(attr);
                    } else {
                        e.setAttribute(attr, Ops.toString(v));
                    }
                } else if (type.startsWith("int") || "uint".equals(type) || "uint1".equals(type) || "uintd1".equals(type)) {
                    e.setAttribute(attr, Integer.toString(Ops.toInt32(v)));
                } else {
                    e.setAttribute(attr, Ops.toString(v));
                }
                return Js.UNDEFINED;
            }
        });
        info.prototype.defineAccessor(prop, getFn, setFn, true, true);
    }
}
