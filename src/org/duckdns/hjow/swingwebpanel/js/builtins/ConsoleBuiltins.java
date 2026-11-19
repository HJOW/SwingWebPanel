package org.duckdns.hjow.swingwebpanel.js.builtins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.js.Js;
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
 * The <code>console</code> object of the WHATWG Console Standard: <code>log info warn error debug assert clear count countReset dir dirxml
 * group groupCollapsed groupEnd table time timeEnd timeLog trace</code>. The messages go to the realm's {@link org.duckdns.hjow.swingwebpanel.js.ConsoleHook}
 * with the level <code>log</code>, <code>info</code>, <code>warn</code>, <code>error</code> or <code>debug</code>; groups indent the following
 * lines by two blanks per level. The first argument is a format string when it contains <code>%s %d %i %f %o %O %c %%</code> and there is more
 * than one argument (the Formatter operation). The object is printed without calling script code (no getters, no <code>toString</code>). Each
 * <code>console</code> object has its own group depth, counters and timers. Limits: <code>console.trace</code> prints no call stack (the
 * engine keeps none) and <code>console.table</code> treats a <code>Map</code> or <code>Set</code> as an ordinary object.
 * <p>
 * WHATWG Console Standard 의 <code>console</code> 객체: <code>log info warn error debug assert clear count countReset dir dirxml group
 * groupCollapsed groupEnd table time timeEnd timeLog trace</code>. 메시지는 realm 의 {@link org.duckdns.hjow.swingwebpanel.js.ConsoleHook} 으로
 * <code>log</code>, <code>info</code>, <code>warn</code>, <code>error</code>, <code>debug</code> 수준으로 가고, 그룹은 단계마다 공백 두 칸씩 뒤따르는 줄을
 * 들여 쓴다. 인수가 둘 이상이고 첫 인수가 <code>%s %d %i %f %o %O %c %%</code> 를 포함하면 서식 문자열이다(Formatter 연산). 객체는 스크립트 코드를 호출하지
 * 않고(getter 와 <code>toString</code> 없이) 출력한다. <code>console</code> 객체마다 그룹 깊이, 카운터, 타이머가 따로 있다. 제한: <code>console.trace</code> 는
 * 호출 스택을 출력하지 않고(엔진이 보관하지 않음) <code>console.table</code> 은 <code>Map</code>/<code>Set</code> 을 일반 객체로 취급한다.
 */
final class ConsoleBuiltins {

    /**
     * The blanks added to the lines of a message for each open group.
     * <p>
     * 열린 그룹마다 메시지의 줄에 더하는 공백.
     */
    private static final String INDENT = "  ";

    /**
     * The realm whose console hook receives the messages.
     * <p>
     * 메시지를 받는 콘솔 연결을 가진 realm.
     */
    private final JsRealm realm;

    /**
     * The number of open groups.
     * <p>
     * 열린 그룹 수.
     */
    private int depth;

    /**
     * The counters of <code>console.count</code> by label.
     * <p>
     * <code>console.count</code> 의 라벨별 카운터.
     */
    private final Map<String, Integer> counts = new HashMap<String, Integer>();

    /**
     * The start time (nanoseconds) of the timers of <code>console.time</code> by label.
     * <p>
     * <code>console.time</code> 타이머의 라벨별 시작 시각(나노초).
     */
    private final Map<String, Long> timers = new HashMap<String, Long>();

    /**
     * Creates the state of one console object.
     * <p>
     * 콘솔 객체 하나의 상태를 만든다.
     *
     * @param realm the realm / realm
     */
    private ConsoleBuiltins(JsRealm realm) {
        this.realm = realm;
    }

    /**
     * Creates the <code>console</code> object of a realm.
     * <p>
     * realm 의 <code>console</code> 객체를 만든다.
     *
     * @param realm the realm / realm
     * @return the object / 객체
     */
    static JsObject create(JsRealm realm) {
        final ConsoleBuiltins state = new ConsoleBuiltins(realm);
        JsObject console = realm.newObject();
        String[] levels = { "log", "info", "warn", "error", "debug" };
        // 수준 이름이 그대로 메서드 이름인 출력 메서드를 만든다.
        for (int i = 0; i < levels.length; i++) {
            final String level = levels[i];
            realm.method(console, level, 0, new NativeFunction.Call() {
                public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                    state.print(level, state.formatter(args));
                    return Js.UNDEFINED;
                }
            });
        }
        realm.method(console, "dirxml", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                state.print("log", state.formatter(args));
                return Js.UNDEFINED;
            }
        });
        realm.method(console, "dir", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                StringBuilder sb = new StringBuilder();
                GlobalBuiltins.inspect(Js.arg(args, 0), 0, sb, new java.util.IdentityHashMap<JsObject, Boolean>());
                state.print("log", sb.toString());
                return Js.UNDEFINED;
            }
        });
        realm.method(console, "assert", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                // 조건이 참이면 아무것도 하지 않는다.
                if (Ops.toBoolean(Js.arg(args, 0))) {
                    return Js.UNDEFINED;
                }
                JsValue[] rest = new JsValue[Math.max(0, args.length - 1)];
                System.arraycopy(args, 1, rest, 0, rest.length);
                String text = state.formatter(rest);
                // 추가 데이터가 있으면 "Assertion failed: " 뒤에 붙이고, 없으면 기본 문구만 쓴다.
                state.print("error", rest.length == 0 ? "Assertion failed" : "Assertion failed: " + text);
                return Js.UNDEFINED;
            }
        });
        realm.method(console, "clear", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                // 화면을 지울 수단이 없으므로 그룹 깊이만 되돌린다.
                state.depth = 0;
                return Js.UNDEFINED;
            }
        });
        realm.method(console, "count", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                String label = state.label(args);
                Integer old = state.counts.get(label);
                int n = old == null ? 1 : old.intValue() + 1;
                state.counts.put(label, Integer.valueOf(n));
                state.print("log", label + ": " + n);
                return Js.UNDEFINED;
            }
        });
        realm.method(console, "countReset", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                String label = state.label(args);
                // 없는 카운터를 초기화하면 경고하고, 있으면 0 으로 되돌린다.
                if (state.counts.containsKey(label)) {
                    state.counts.put(label, Integer.valueOf(0));
                } else {
                    state.print("warn", "Count for '" + label + "' does not exist");
                }
                return Js.UNDEFINED;
            }
        });
        installGroups(realm, console, state);
        installTimers(realm, console, state);
        realm.method(console, "trace", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                String text = state.formatter(args);
                state.print("log", (text.length() == 0 ? "Trace" : "Trace: " + text) + "\n    at <anonymous>");
                return Js.UNDEFINED;
            }
        });
        realm.method(console, "table", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue data = Js.arg(args, 0);
                // 표로 만들 수 없는 값은 보통 log 와 같이 출력한다.
                if (!(data instanceof JsObject)) {
                    state.print("log", state.formatter(args));
                } else {
                    state.print("log", state.table((JsObject) data, Js.arg(args, 1)));
                }
                return Js.UNDEFINED;
            }
        });
        return console;
    }

    /**
     * Adds <code>group</code>, <code>groupCollapsed</code> and <code>groupEnd</code>.
     * <p>
     * <code>group</code>, <code>groupCollapsed</code>, <code>groupEnd</code> 를 더한다.
     *
     * @param realm   the realm / realm
     * @param console the console object / 콘솔 객체
     * @param state   the state of the console / 콘솔의 상태
     */
    private static void installGroups(JsRealm realm, JsObject console, final ConsoleBuiltins state) {
        String[] names = { "group", "groupCollapsed" };
        // 두 이름은 같은 동작이다(접힌 상태를 표현할 화면이 없다).
        for (int i = 0; i < names.length; i++) {
            realm.method(console, names[i], 0, new NativeFunction.Call() {
                public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                    // 라벨이 있으면 먼저 출력하고, 그 다음부터 한 단계 들여 쓴다.
                    if (args.length > 0) {
                        state.print("log", state.formatter(args));
                    }
                    state.depth++;
                    return Js.UNDEFINED;
                }
            });
        }
        realm.method(console, "groupEnd", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                // 열린 그룹이 없으면 아무것도 하지 않는다.
                if (state.depth > 0) {
                    state.depth--;
                }
                return Js.UNDEFINED;
            }
        });
    }

    /**
     * Adds <code>time</code>, <code>timeEnd</code> and <code>timeLog</code>.
     * <p>
     * <code>time</code>, <code>timeEnd</code>, <code>timeLog</code> 를 더한다.
     *
     * @param realm   the realm / realm
     * @param console the console object / 콘솔 객체
     * @param state   the state of the console / 콘솔의 상태
     */
    private static void installTimers(JsRealm realm, JsObject console, final ConsoleBuiltins state) {
        realm.method(console, "time", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                String label = state.label(args);
                // 이미 있는 타이머는 경고하고 그대로 둔다.
                if (state.timers.containsKey(label)) {
                    state.print("warn", "Timer '" + label + "' already exists");
                } else {
                    state.timers.put(label, Long.valueOf(System.nanoTime()));
                }
                return Js.UNDEFINED;
            }
        });
        realm.method(console, "timeLog", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                state.report(args, false);
                return Js.UNDEFINED;
            }
        });
        realm.method(console, "timeEnd", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                state.report(args, true);
                return Js.UNDEFINED;
            }
        });
    }

    /**
     * Prints the elapsed time of a timer (<code>timeLog</code>, <code>timeEnd</code>).
     * <p>
     * 타이머의 경과 시간을 출력한다(<code>timeLog</code>, <code>timeEnd</code>).
     *
     * @param args the arguments of the call / 호출의 인수
     * @param end  true to remove the timer (<code>timeEnd</code>) / 타이머를 지우려면 true(<code>timeEnd</code>)
     */
    private void report(JsValue[] args, boolean end) {
        String label = label(args);
        Long start = timers.get(label);
        // 시작하지 않은 타이머는 경고한다.
        if (start == null) {
            print("warn", "Timer '" + label + "' does not exist");
            return;
        }
        double ms = (System.nanoTime() - start.longValue()) / 1000000.0;
        StringBuilder sb = new StringBuilder(label).append(": ");
        sb.append(Math.round(ms * 1000) / 1000.0).append("ms");
        // timeLog 는 라벨 뒤의 추가 데이터를 같은 줄에 붙인다.
        if (!end && args.length > 1) {
            JsValue[] rest = new JsValue[args.length - 1];
            System.arraycopy(args, 1, rest, 0, rest.length);
            sb.append(' ').append(GlobalBuiltins.format(rest));
        }
        if (end) {
            timers.remove(label);
        }
        print("log", sb.toString());
    }

    /**
     * Returns the label argument of <code>count</code> and the timers: the first argument as a string, or <code>default</code> when it is
     * missing or undefined.
     * <p>
     * <code>count</code> 와 타이머의 라벨 인수를 반환한다: 첫 인수의 문자열이며 없거나 undefined 이면 <code>default</code>.
     *
     * @param args the arguments / 인수
     * @return the label / 라벨
     */
    private String label(JsValue[] args) {
        JsValue v = Js.arg(args, 0);
        return v.isUndefined() ? "default" : Ops.toString(v);
    }

    /**
     * Writes a message to the console hook, indented by the open groups.
     * <p>
     * 열린 그룹만큼 들여 써서 메시지를 콘솔 연결에 쓴다.
     *
     * @param level the level / 수준
     * @param text  the text (may have several lines) / 문자열(여러 줄일 수 있음)
     */
    private void print(String level, String text) {
        String shown = text;
        // 그룹 안이면 모든 줄 앞에 들여쓰기를 붙인다.
        if (depth > 0) {
            StringBuilder pad = new StringBuilder();
            // 열린 그룹 수만큼 들여쓰기 단위를 반복한다.
            for (int i = 0; i < depth; i++) {
                pad.append(INDENT);
            }
            shown = pad + text.replace("\n", "\n" + pad);
        }
        realm.getConsole().message(level, shown);
    }

    /**
     * The Formatter operation of the Console Standard: expands <code>%s %d %i %f %o %O %c %%</code> of the first argument with the following
     * arguments and appends the remaining arguments. With one argument, or when the first argument is not a string with a <code>%</code>, the
     * arguments are printed as <code>console.log</code> always did.
     * <p>
     * Console Standard 의 Formatter 연산: 첫 인수의 <code>%s %d %i %f %o %O %c %%</code> 를 뒤따르는 인수로 바꾸고 남은 인수를 덧붙인다. 인수가 하나거나
     * 첫 인수가 <code>%</code> 가 든 문자열이 아니면 지금까지의 <code>console.log</code> 처럼 인수를 출력한다.
     *
     * @param args the arguments / 인수
     * @return the text / 문자열
     */
    String formatter(JsValue[] args) {
        // 서식 문자열이 될 수 없는 경우는 기존 출력 방식을 따른다.
        if (args.length < 2 || !(args[0] instanceof JsString) || ((JsString) args[0]).getValue().indexOf('%') < 0) {
            return GlobalBuiltins.format(args);
        }
        String f = ((JsString) args[0]).getValue();
        StringBuilder sb = new StringBuilder();
        int next = 1;
        // 서식 문자열을 한 글자씩 보며 지정자를 인수로 바꾼다.
        for (int i = 0; i < f.length(); i++) {
            char c = f.charAt(i);
            if (c != '%' || i + 1 >= f.length()) {
                sb.append(c);
                continue;
            }
            char k = f.charAt(i + 1);
            if (k == '%') {
                sb.append('%');
                i++;
            } else if ("sdifoOc".indexOf(k) >= 0 && next < args.length) {
                JsValue a = args[next++];
                appendSubstitution(sb, k, a);
                i++;
            } else {
                sb.append(c);
            }
        }
        // 서식에 쓰이지 않은 인수는 공백으로 이어 붙인다.
        for (; next < args.length; next++) {
            sb.append(' ');
            JsValue a = args[next];
            if (a instanceof JsString) {
                sb.append(((JsString) a).getValue());
            } else {
                GlobalBuiltins.inspect(a, 0, sb, new java.util.IdentityHashMap<JsObject, Boolean>());
            }
        }
        return sb.toString();
    }

    /**
     * Appends the substitution of one specifier.
     * <p>
     * 지정자 하나의 치환 결과를 덧붙인다.
     *
     * @param sb the output / 출력
     * @param k  the specifier letter / 지정자 글자
     * @param a  the argument / 인수
     */
    private static void appendSubstitution(StringBuilder sb, char k, JsValue a) {
        switch (k) {
        case 's':
            // 문자열 지정자: 원시 값은 문자열로, 객체는 코드를 실행하지 않는 읽기 쉬운 형태로 바꾼다.
            if (a instanceof JsString) {
                sb.append(((JsString) a).getValue());
            } else if (a instanceof JsObject) {
                GlobalBuiltins.inspect(a, 1, sb, new java.util.IdentityHashMap<JsObject, Boolean>());
            } else {
                sb.append(a instanceof JsSymbol ? a.toString() : Ops.toString(a));
            }
            break;
        case 'd':
        case 'i':
            sb.append(integerText(a));
            break;
        case 'f':
            sb.append(floatText(a));
            break;
        case 'o':
        case 'O':
            GlobalBuiltins.inspect(a, 0, sb, new java.util.IdentityHashMap<JsObject, Boolean>());
            break;
        default:
            // %c 는 CSS 스타일 인수를 소비만 하고 출력하지 않는다.
            break;
        }
    }

    /**
     * The text of <code>%d</code> and <code>%i</code>: <code>parseInt</code> of the argument; an object or a symbol gives <code>NaN</code>.
     * <p>
     * <code>%d</code> 와 <code>%i</code> 의 문자열: 인수의 <code>parseInt</code> 결과이며 객체나 심볼이면 <code>NaN</code>.
     *
     * @param a the argument / 인수
     * @return the text / 문자열
     */
    private static String integerText(JsValue a) {
        // 객체와 심볼은 숫자로 바꿀 수 없으므로 NaN 이다.
        if (a instanceof JsObject || a instanceof JsSymbol) {
            return "NaN";
        }
        double d = a instanceof JsNumber ? ((JsNumber) a).getValue() : prefixNumber(Ops.toString(a), false);
        // parseInt 는 숫자를 문자열로 바꾼 뒤 읽으므로 소수 부분을 버리고 무한대와 NaN 은 NaN 으로 한다.
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            return "NaN";
        }
        d = d < 0 ? Math.ceil(d) : Math.floor(d);
        return JsNumber.of(d == 0 ? 0 : d).toString();
    }

    /**
     * The text of <code>%f</code>: <code>parseFloat</code> of the argument; an object or a symbol gives <code>NaN</code>.
     * <p>
     * <code>%f</code> 의 문자열: 인수의 <code>parseFloat</code> 결과이며 객체나 심볼이면 <code>NaN</code>.
     *
     * @param a the argument / 인수
     * @return the text / 문자열
     */
    private static String floatText(JsValue a) {
        // 객체와 심볼은 숫자로 바꿀 수 없으므로 NaN 이다.
        if (a instanceof JsObject || a instanceof JsSymbol) {
            return "NaN";
        }
        double d = a instanceof JsNumber ? ((JsNumber) a).getValue() : prefixNumber(Ops.toString(a), true);
        return JsNumber.of(d).toString();
    }

    /**
     * Reads the number at the start of a string like <code>parseInt</code> (decimal) or <code>parseFloat</code>, or returns NaN.
     * <p>
     * 문자열 앞쪽의 수를 <code>parseInt</code>(10진수)나 <code>parseFloat</code> 처럼 읽으며, 없으면 NaN 을 반환한다.
     *
     * @param s     the string / 문자열
     * @param fract true for <code>parseFloat</code> (fraction and exponent are read) / <code>parseFloat</code> 이면 true(소수와 지수도 읽음)
     * @return the number / 수
     */
    private static double prefixNumber(String s, boolean fract) {
        String t = s.trim();
        int i = 0;
        int n = t.length();
        // 부호는 한 글자만 허용한다.
        if (i < n && (t.charAt(i) == '+' || t.charAt(i) == '-')) {
            i++;
        }
        if (fract && t.startsWith("Infinity", i)) {
            return t.charAt(0) == '-' ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        }
        int digits = i;
        // 정수부의 숫자를 모두 읽는다.
        while (i < n && t.charAt(i) >= '0' && t.charAt(i) <= '9') {
            i++;
        }
        boolean any = i > digits;
        if (fract && i < n && t.charAt(i) == '.') {
            int dot = ++i;
            // 소수부의 숫자를 읽는다.
            while (i < n && t.charAt(i) >= '0' && t.charAt(i) <= '9') {
                i++;
            }
            any = any || i > dot;
        }
        if (!any) {
            return Double.NaN;
        }
        // 지수는 부호와 숫자가 뒤따를 때만 읽는다.
        if (fract && i < n && (t.charAt(i) == 'e' || t.charAt(i) == 'E')) {
            int e = i + 1;
            if (e < n && (t.charAt(e) == '+' || t.charAt(e) == '-')) {
                e++;
            }
            int ed = e;
            // 지수부의 숫자를 읽는다.
            while (e < n && t.charAt(e) >= '0' && t.charAt(e) <= '9') {
                e++;
            }
            if (e > ed) {
                i = e;
            }
        }
        try {
            return Double.parseDouble(t.substring(0, i));
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }

    /**
     * Builds the text of <code>console.table</code>: the first column is the index (array index or property name), the following columns are
     * the properties of the rows (or the ones listed in the second argument); rows that are not objects are put in a <code>Values</code>
     * column.
     * <p>
     * <code>console.table</code> 의 문자열을 만든다: 첫 열은 색인(배열 색인이나 프로퍼티 이름), 다음 열은 행의 프로퍼티(또는 두 번째 인수가 나열한 것)이고
     * 객체가 아닌 행은 <code>Values</code> 열에 둔다.
     *
     * @param data       the tabular data / 표 데이터
     * @param properties the second argument (an array of column names or undefined) / 두 번째 인수(열 이름 배열 또는 undefined)
     * @return the text / 문자열
     */
    private String table(JsObject data, JsValue properties) {
        List<String> only = null;
        // 열 이름 목록이 주어지면 그 열만 쓴다.
        if (properties instanceof JsObject && "Array".equals(((JsObject) properties).className())) {
            only = new ArrayList<String>();
            JsObject list = (JsObject) properties;
            int len = (int) Ops.toNumber(list.get("length"));
            // 배열의 요소를 열 이름으로 모은다.
            for (int i = 0; i < len; i++) {
                only.add(Ops.toString(list.get(String.valueOf(i))));
            }
        }
        List<String> index = new ArrayList<String>();
        List<Map<String, String>> rows = new ArrayList<Map<String, String>>();
        List<String> columns = new ArrayList<String>();
        boolean values = false;
        // 데이터의 열거 가능한 프로퍼티마다 행 하나를 만든다.
        for (Object key : data.ownKeys()) {
            PropertyDescriptor d = data.getOwnProperty(key);
            if (d == null || !d.isEnumerableTrue() || key instanceof JsSymbol) {
                continue;
            }
            index.add(String.valueOf(key));
            Map<String, String> row = new LinkedHashMap<String, String>();
            JsValue cell = d.isAccessor() ? Js.UNDEFINED : d.value;
            if (cell instanceof JsObject && !((JsObject) cell).isCallable()) {
                JsObject o = (JsObject) cell;
                // 행이 객체이면 그 프로퍼티가 열이 된다.
                for (Object ck : o.ownKeys()) {
                    PropertyDescriptor cd = o.getOwnProperty(ck);
                    if (cd == null || !cd.isEnumerableTrue() || ck instanceof JsSymbol) {
                        continue;
                    }
                    String name = String.valueOf(ck);
                    if (only != null && !only.contains(name)) {
                        continue;
                    }
                    row.put(name, cd.isAccessor() ? "[Getter/Setter]" : cellText(cd.value));
                    if (only == null && !columns.contains(name)) {
                        columns.add(name);
                    }
                }
            } else {
                row.put(null, cellText(cell));
                values = true;
            }
            rows.add(row);
        }
        if (only != null) {
            columns = only;
        }
        List<String> header = new ArrayList<String>();
        header.add("(index)");
        header.addAll(columns);
        if (values) {
            header.add("Values");
        }
        List<String[]> lines = new ArrayList<String[]>();
        // 행마다 색인, 열 값, 단순 값 순서로 칸을 채운다.
        for (int r = 0; r < rows.size(); r++) {
            String[] line = new String[header.size()];
            line[0] = index.get(r);
            // 열마다 그 행의 값을 채우고 없으면 빈칸으로 둔다.
            for (int c = 0; c < columns.size(); c++) {
                String v = rows.get(r).get(columns.get(c));
                line[c + 1] = v == null ? "" : v;
            }
            if (values) {
                String v = rows.get(r).get(null);
                line[line.length - 1] = v == null ? "" : v;
            }
            lines.add(line);
        }
        return render(header.toArray(new String[header.size()]), lines);
    }

    /**
     * The text of a table cell: strings as they are, other values in the compact form of the console.
     * <p>
     * 표 칸의 문자열: 문자열은 그대로, 다른 값은 콘솔의 간결한 형태.
     *
     * @param v the value / 값
     * @return the text / 문자열
     */
    private static String cellText(JsValue v) {
        if (v instanceof JsString) {
            return ((JsString) v).getValue();
        }
        StringBuilder sb = new StringBuilder();
        GlobalBuiltins.inspect(v, 1, sb, new java.util.IdentityHashMap<JsObject, Boolean>());
        return sb.toString();
    }

    /**
     * Draws a text table with borders made of <code>+ - |</code>.
     * <p>
     * <code>+ - |</code> 로 테두리를 그린 문자 표를 그린다.
     *
     * @param header the column titles / 열 제목
     * @param lines  the rows / 행
     * @return the text / 문자열
     */
    private static String render(String[] header, List<String[]> lines) {
        int[] w = new int[header.length];
        // 열 너비는 제목과 모든 칸 중 가장 긴 글자 수이다.
        for (int c = 0; c < header.length; c++) {
            w[c] = header[c].length();
            // 그 열의 모든 행을 보며 가장 긴 칸을 찾는다.
            for (int r = 0; r < lines.size(); r++) {
                w[c] = Math.max(w[c], lines.get(r)[c].length());
            }
        }
        StringBuilder bar = new StringBuilder("+");
        // 구분선은 열마다 너비에 양쪽 여백 2를 더한 만큼의 '-' 와 '+' 로 이어진다.
        for (int c = 0; c < w.length; c++) {
            // 이 열의 '-' 를 채운다.
            for (int k = 0; k < w[c] + 2; k++) {
                bar.append('-');
            }
            bar.append('+');
        }
        StringBuilder sb = new StringBuilder(bar).append('\n');
        appendLine(sb, header, w);
        sb.append(bar).append('\n');
        // 행을 한 줄씩 그린다.
        for (int r = 0; r < lines.size(); r++) {
            appendLine(sb, lines.get(r), w);
        }
        sb.append(bar);
        return sb.toString();
    }

    /**
     * Appends one row of the text table.
     * <p>
     * 문자 표의 한 줄을 덧붙인다.
     *
     * @param sb    the output / 출력
     * @param cells the cells / 칸
     * @param w     the column widths / 열 너비
     */
    private static void appendLine(StringBuilder sb, String[] cells, int[] w) {
        sb.append('|');
        // 칸마다 왼쪽 정렬로 너비를 채운다.
        for (int c = 0; c < cells.length; c++) {
            sb.append(' ').append(cells[c]);
            // 칸의 글자 수가 열 너비에 못 미치면 공백으로 채운다.
            for (int k = cells[c].length(); k < w[c]; k++) {
                sb.append(' ');
            }
            sb.append(" |");
        }
        sb.append('\n');
    }
}
