package org.duckdns.hjow.swingwebpanel.js.builtins;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.js.BuiltinInstaller;
import org.duckdns.hjow.swingwebpanel.js.ConsoleRecord;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsArray;
import org.duckdns.hjow.swingwebpanel.js.JsNumber;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsSymbol;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;

/**
 * The global value properties and functions the language needs: <code>NaN</code>, <code>Infinity</code>, <code>undefined</code>,
 * <code>globalThis</code>, the indirect <code>eval</code> function, the host-connected <code>console</code> object and the
 * test-friendly <code>print</code> function. Output goes to the realm's {@link org.duckdns.hjow.swingwebpanel.js.ConsoleHook}.
 * <p>
 * 언어가 필요로 하는 전역 값 프로퍼티와 함수: <code>NaN</code>, <code>Infinity</code>, <code>undefined</code>, <code>globalThis</code>,
 * 간접 <code>eval</code> 함수, 호스트에 연결된 <code>console</code> 객체, 시험하기 쉬운 <code>print</code> 함수. 출력은 realm 의
 * {@link org.duckdns.hjow.swingwebpanel.js.ConsoleHook} 으로 간다.
 */
final class GlobalBuiltins implements BuiltinInstaller {

    /**
     * Creates the installer.
     * <p>
     * 설치기를 만든다.
     */
    GlobalBuiltins() {
    }

    /**
     * Installs the globals.
     * <p>
     * 전역 객체의 프로퍼티를 설치한다.
     *
     * @param realm the realm / realm
     */
    public void install(JsRealm realm) {
        JsObject g = realm.getGlobalObject();
        g.defineData("NaN", JsNumber.NAN, false, false, false);
        g.defineData("Infinity", JsNumber.of(Double.POSITIVE_INFINITY), false, false, false);
        g.defineData("undefined", Js.UNDEFINED, false, false, false);
        g.defineBuiltin("globalThis", realm.getGlobalThisValue());
        NativeFunction eval = realm.method(g, "eval", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue x = Js.arg(args, 0);
                if (!(x instanceof JsString)) {
                    return x;
                }
                return r.getInterpreter().evalIndirect(((JsString) x).getValue());
            }
        });
        realm.setIntrinsic("%eval%", eval);
        realm.method(g, "print", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                // print 도 콘솔 호출과 같은 기록으로 보내 호출 위치와 인수 문자열을 함께 전달한다.
                ConsoleRecord.deliver(r.getConsole(), ConsoleRecord.call("print", "log", format(args), argumentTexts(args),
                        r.getCallSourceName(), r.getCallLine(), r.getCallColumn(), 0));
                return Js.UNDEFINED;
            }
        });
        // console 객체는 Console Standard 의 모든 메서드를 가진 ConsoleBuiltins 가 만든다.
        g.defineBuiltin("console", ConsoleBuiltins.create(realm));
    }

    /**
     * Returns the text of each argument the way {@link #format(JsValue[])} prints it, as a list (a string as is, another value in a
     * compact readable form). It never runs script code.
     * <p>
     * {@link #format(JsValue[])} 가 출력하는 방식으로 각 인수의 문자열을 목록으로 반환한다(문자열은 그대로, 다른 값은 간결하고 읽기 쉬운
     * 형태). 스크립트 코드를 실행하지 않는다.
     *
     * @param args the arguments / 인수
     * @return the texts, one per argument / 인수마다 하나씩의 문자열
     */
    public static List<String> argumentTexts(JsValue[] args) {
        List<String> out = new ArrayList<String>(args.length);
        // 인수를 하나씩 문자열로 바꿔 목록에 담는다.
        for (int i = 0; i < args.length; i++) {
            StringBuilder sb = new StringBuilder();
            if (args[i] instanceof JsString) {
                sb.append(((JsString) args[i]).getValue());
            } else {
                inspect(args[i], 0, sb, new IdentityHashMap<JsObject, Boolean>());
            }
            out.add(sb.toString());
        }
        return out;
    }

    /**
     * Formats arguments the way console methods print them: strings as is, other values in a compact readable form, separated by
     * spaces.
     * <p>
     * 콘솔 메서드가 출력하는 방식으로 인수를 서식화한다: 문자열은 그대로, 다른 값은 간결하고 읽기 쉬운 형태로, 공백으로 구분한다.
     *
     * @param args the arguments / 인수
     * @return the text / 문자열
     */
    static String format(JsValue[] args) {
        StringBuilder sb = new StringBuilder();
        // 인수를 하나씩 서식화해 공백으로 잇는다.
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            if (args[i] instanceof JsString) {
                sb.append(((JsString) args[i]).getValue());
            } else {
                inspect(args[i], 0, sb, new IdentityHashMap<JsObject, Boolean>());
            }
        }
        return sb.toString();
    }

    /**
     * Appends a compact readable form of a value without running script code (getters are not called).
     * <p>
     * 스크립트 코드를 실행하지 않고(getter 를 호출하지 않고) 값의 간결한 읽기 쉬운 형태를 덧붙인다.
     *
     * @param v     the value / 값
     * @param depth the nesting depth / 중첩 깊이
     * @param sb    the output / 출력
     * @param seen  the objects being printed (cycles) / 출력 중인 객체(순환)
     */
    static void inspect(JsValue v, int depth, StringBuilder sb, Map<JsObject, Boolean> seen) {
        if (v instanceof JsString) {
            sb.append('\'').append(((JsString) v).getValue()).append('\'');
        } else if (v instanceof JsNumber) {
            double d = ((JsNumber) v).getValue();
            sb.append(d == 0 && 1 / d < 0 ? "-0" : v.toString());
        } else if (!(v instanceof JsObject)) {
            sb.append(v.toString());
        } else {
            JsObject o = (JsObject) v;
            if (o.isCallable()) {
                JsValue n = o.getOwnProperty("name") == null ? Js.UNDEFINED : o.get("name");
                sb.append("[Function").append(n instanceof JsString && ((JsString) n).length() > 0 ? ": " + n : "").append(']');
            } else if ("Error".equals(o.className())) {
                JsValue stack = o.get("stack");
                sb.append(stack instanceof JsString ? ((JsString) stack).getValue() : JsException_describe(o));
            } else if (depth > 2 || seen.containsKey(o)) {
                sb.append(o instanceof JsArray ? "[Array]" : "[Object]");
            } else {
                seen.put(o, Boolean.TRUE);
                inspectObject(o, depth, sb, seen);
                seen.remove(o);
            }
        }
    }

    /**
     * Describes an error object by name and message.
     * <p>
     * 오류 객체를 이름과 메시지로 설명한다.
     *
     * @param o the error object / 오류 객체
     * @return the text / 문자열
     */
    private static String JsException_describe(JsObject o) {
        return org.duckdns.hjow.swingwebpanel.js.JsException.describe(o);
    }

    /**
     * Appends the elements of an array or the properties of an object.
     * <p>
     * 배열의 요소나 객체의 프로퍼티를 덧붙인다.
     *
     * @param o     the object / 객체
     * @param depth the nesting depth / 중첩 깊이
     * @param sb    the output / 출력
     * @param seen  the objects being printed / 출력 중인 객체
     */
    private static void inspectObject(JsObject o, int depth, StringBuilder sb, Map<JsObject, Boolean> seen) {
        boolean array = o instanceof JsArray;
        sb.append(array ? "[ " : "{ ");
        boolean first = true;
        // 열거 가능한 자체 프로퍼티를 순서대로 출력한다. 데이터 프로퍼티의 값만 읽는다.
        for (Object k : o.ownKeys()) {
            PropertyDescriptor d = o.getOwnProperty(k);
            if (d == null || !d.isEnumerableTrue() || k instanceof JsSymbol) {
                continue;
            }
            if (!first) {
                sb.append(", ");
            }
            first = false;
            if (!array) {
                sb.append(k).append(": ");
            }
            if (d.isAccessor()) {
                sb.append("[Getter/Setter]");
            } else {
                inspect(d.value, depth + 1, sb, seen);
            }
        }
        if (first) {
            sb.setLength(sb.length() - 1);
            sb.append(array ? "]" : "}");
        } else {
            sb.append(array ? " ]" : " }");
        }
    }

    /**
     * Converts a value to a Java string for helper use in tests.
     * <p>
     * 시험의 도우미로 쓰도록 값을 Java 문자열로 바꾼다.
     *
     * @param v the value / 값
     * @return the string / 문자열
     */
    static String text(JsValue v) {
        return Ops.toString(v);
    }
}
