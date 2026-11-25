package org.duckdns.hjow.swingwebpanel.js.embed;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the value conversion rules, the live handles, host functions and Java interface proxies of {@link JsEngine} (plan item P26-02):
 * what crosses between Java and script in both directions, and what must never cross.
 * <p>
 * {@link JsEngine} 의 값 변환 규칙, 살아 있는 핸들, 호스트 함수, Java 인터페이스 프록시의 시험(계획 항목 P26-02): Java 와 스크립트 사이를 양방향으로 넘는 것과
 * 절대 넘으면 안 되는 것.
 */
public class JsEngineValuesTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public JsEngineValuesTest() {
    }

    /**
     * Java values given to a script: primitives, numbers of every Java type, strings, containers (copied).
     * <p>
     * 스크립트에 주는 Java 값: 기본값, 모든 Java 타입의 숫자, 문자열, 컨테이너(복사).
     */
    public void testJavaToScript() {
        JsEngine e = new JsEngine();
        e.put("i", Integer.valueOf(7));
        e.put("l", Long.valueOf(9L));
        e.put("d", Double.valueOf(1.5));
        e.put("f", Float.valueOf(0.5f));
        e.put("bi", new BigInteger("123"));
        e.put("bd", new BigDecimal("2.25"));
        e.put("by", Byte.valueOf((byte) -3));
        e.put("c", Character.valueOf('x'));
        e.put("sb", new StringBuilder("sb"));
        e.put("en", Thread.State.RUNNABLE);
        e.put("nan", Double.valueOf(Double.NaN));
        e.put("neg0", Double.valueOf(-0.0));
        Check.equal("7,9,1.5,0.5,123,2.25,-3", e.eval("[i, l, d, f, bi, bd, by].join()"), "numbers");
        Check.equal("number,number,number,number,bigint,number,number",
                e.eval("[i, l, d, f, bi, bd, by].map(function (v) { return typeof v; }).join()"), "BigInteger is a BigInt and other Numbers are numbers");
        Check.equal("x|string", e.eval("c + '|' + typeof c"), "Character is a string");
        Check.equal("sb|string", e.eval("sb + '|' + typeof sb"), "CharSequence is a string");
        Check.equal("RUNNABLE", e.eval("en"), "Enum is its name");
        Check.equal(Boolean.TRUE, e.eval("nan !== nan && 1 / neg0 === -Infinity"), "NaN and -0 survive");
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("a", Integer.valueOf(1));
        map.put("list", Arrays.asList("p", "q"));
        Map<String, Object> inner = new LinkedHashMap<String, Object>();
        inner.put("deep", Boolean.TRUE);
        map.put("inner", inner);
        map.put("arr", new int[] { 4, 5 });
        e.put("m", map);
        Check.equal("{\"a\":1,\"list\":[\"p\",\"q\"],\"inner\":{\"deep\":true},\"arr\":[4,5]}", e.eval("JSON.stringify(m)"), "a Map becomes an object, collections and arrays become arrays");
        e.eval("m.a = 99");
        Check.equal(Integer.valueOf(1), map.get("a"), "the conversion of a Map is a copy");
        Check.equal(Boolean.TRUE, e.eval("Array.isArray(m.list) && Array.isArray(m.arr) && m.list instanceof Array"), "real arrays");
        e.put("objs", new Object[] { "s", null, Boolean.FALSE, JsEngine.UNDEFINED });
        Check.equal("s|null|false|undefined", e.eval("objs.map(function (v) { return v === undefined ? 'undefined' : String(v); }).join('|')"), "array of mixed values");
    }

    /**
     * Cyclic Java structures keep their shape instead of looping.
     * <p>
     * 순환하는 Java 구조는 반복하지 않고 모양을 유지한다.
     */
    public void testCyclicJavaStructureKeepsItsShape() {
        JsEngine e = new JsEngine();
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("self", m);
        e.put("loop", m);
        Check.equal(Boolean.TRUE, e.eval("loop.self === loop"), "the cycle is preserved");
        List<Object> list = new ArrayList<Object>();
        list.add(list);
        e.put("ring", list);
        Check.equal(Boolean.TRUE, e.eval("ring[0] === ring"), "an array that contains itself");
    }

    /**
     * Java objects that are not on the list of convertible types are refused, so nothing of Java leaks into a script.
     * <p>
     * 변환 가능한 타입 목록에 없는 Java 객체는 거부되므로 Java 의 어떤 것도 스크립트로 새지 않는다.
     */
    public void testOtherJavaObjectsAreRefused() {
        final JsEngine e = new JsEngine();
        Object[] refused = { new Object(), new Date(), new File("x"), System.out, Thread.currentThread(), Runtime.getRuntime(), String.class };
        // 거부되어야 하는 객체마다 put 이 IllegalArgumentException 을 던지는지 확인한다.
        for (int i = 0; i < refused.length; i++) {
            final Object o = refused[i];
            Throwable t = Check.throwsException(IllegalArgumentException.class, new Block() {
                public void run() {
                    e.put("leak", o);
                }
            }, "refused: " + o.getClass().getName());
            Check.contains(String.valueOf(t.getMessage()), o.getClass().getName(), "the message names the type");
        }
        Check.isFalse(e.has("leak"), "nothing was assigned");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            public void run() {
                e.eval("1");
                e.call("eval", new Object());
            }
        }, "arguments of a call are checked too");
        Map<String, Object> bad = new LinkedHashMap<String, Object>();
        bad.put("inside", new Object());
        final Map<String, Object> badMap = bad;
        Check.throwsException(IllegalArgumentException.class, new Block() {
            public void run() {
                e.put("m", badMap);
            }
        }, "a refused value inside a Map");
        Check.equal("undefinedundefinedundefinedundefined", e.eval("typeof java + typeof Packages + typeof JavaImporter + typeof Java"), "no Java gateway names exist in the script");
        Check.equal(Boolean.TRUE, e.eval("typeof Object.getOwnPropertyNames(globalThis).filter(function (n) { return /java|jdk|sun/i.test(n); })[0] === 'undefined'"),
                "no global name mentions Java");
    }

    /**
     * Script values returned to Java: primitives, undefined, symbols, and the three kinds of handles.
     * <p>
     * Java 로 돌아오는 스크립트 값: 기본값, undefined, 심볼, 세 종류의 핸들.
     */
    public void testScriptToJava() {
        JsEngine e = new JsEngine();
        Check.equal(Double.valueOf(3), e.eval("3"), "an integral number is a Double");
        Check.equal(new BigInteger("123456789012345678901234567890"),
                e.eval("123456789012345678901234567890n"), "a BigInt is a BigInteger without precision loss");
        Check.equal(Double.valueOf(0.5), e.eval("0.5"), "fraction");
        Check.isTrue(e.eval("NaN") instanceof Double && ((Double) e.eval("NaN")).isNaN(), "NaN");
        Check.equal(Double.valueOf(Double.POSITIVE_INFINITY), e.eval("1/0"), "infinity");
        Check.equal(Double.valueOf(-0.0), e.eval("-0"), "negative zero is kept");
        Check.equal(Boolean.FALSE, e.eval("false"), "boolean");
        Check.equal("a😀b", e.eval("'a\\uD83D\\uDE00b'"), "a string keeps its UTF-16 code units");
        Check.same(JsEngine.UNDEFINED, e.eval("undefined"), "undefined");
        Check.equal("Symbol(tag)", e.eval("Symbol('tag')"), "a symbol is its description");
        Check.isTrue(e.eval("[1, 2]") instanceof JsArrayHandle, "array handle");
        Check.isTrue(e.eval("({a: 1})") instanceof JsObjectHandle && !(e.eval("({a: 1})") instanceof JsArrayHandle), "object handle");
        Check.isTrue(e.eval("(function () {})") instanceof JsFunctionHandle, "function handle");
        Check.isTrue(e.eval("(x => x)") instanceof JsCallable, "an arrow function is callable");
        Check.isTrue(e.eval("new Date(0)") instanceof JsObjectHandle, "a Date is an object handle");
        Check.isTrue(e.eval("/a/g") instanceof JsObjectHandle, "a RegExp is an object handle");
        Check.isTrue(e.eval("new Map()") instanceof JsObjectHandle, "a Map is an object handle");
        Check.isTrue(e.eval("new Error('x')") instanceof JsObjectHandle, "an error object is an object handle");
        Check.equal(Double.valueOf(0), ((JsObjectHandle) e.eval("new Date(0)")).callMember("getTime"), "its methods can be called");
    }

    /**
     * An object handle is a live <code>Map</code> of the object: reading, writing, deleting, iterating, nested objects, functions as values.
     * <p>
     * 객체 핸들은 객체의 살아 있는 <code>Map</code> 이다: 읽기, 쓰기, 삭제, 순회, 중첩 객체, 값으로서의 함수.
     */
    public void testObjectHandleIsALiveMap() {
        JsEngine e = new JsEngine();
        e.eval("var cfg = {name: 'n', size: 3, nested: {deep: [1, 2]}, f: function () { return 'called'; }};");
        JsObjectHandle cfg = (JsObjectHandle) e.get("cfg");
        Check.equal("n", cfg.get("name"), "get");
        Check.equal(Double.valueOf(3), cfg.get("size"), "get number");
        Check.isNull(cfg.get("absent"), "a missing property is null");
        Check.isTrue(cfg.containsKey("name") && !cfg.containsKey("absent"), "containsKey");
        Check.equal(4, cfg.size(), "size");
        Check.equal("[name, size, nested, f]", cfg.keySet().toString(), "the keys in script order");
        cfg.put("name", "changed");
        Check.equal("changed", e.eval("cfg.name"), "a write through the handle is seen by the script");
        e.eval("cfg.size = 10");
        Check.equal(Double.valueOf(10), cfg.get("size"), "a write by the script is seen through the handle");
        cfg.put("added", Integer.valueOf(1));
        Check.equal(Double.valueOf(1), e.eval("cfg.added"), "a new property");
        Check.equal(Double.valueOf(1), cfg.remove("added"), "remove returns the old value");
        Check.equal("undefined", e.eval("typeof cfg.added"), "the property is gone");
        JsObjectHandle nested = (JsObjectHandle) cfg.get("nested");
        Check.equal("[1.0, 2.0]", e.toPlain(nested.get("deep")).toString(), "a nested array");
        Check.equal("called", ((JsFunctionHandle) cfg.get("f")).call(), "a function property");
        Check.equal("called", cfg.callMember("f"), "callMember");
        int visited = 0;
        // 항목을 순회하며 키와 값을 읽는다.
        for (Map.Entry<String, Object> entry : cfg.entrySet()) {
            Check.notNull(entry.getKey(), "key");
            visited++;
        }
        Check.equal(4, visited, "iteration");
        JsObjectHandle fresh = e.newObject();
        fresh.put("x", Integer.valueOf(1));
        fresh.put("hostFn", new JsHostFunction() {
            public Object call(Object thisValue, Object[] args) {
                return "from java";
            }
        });
        e.put("fresh", fresh);
        Check.equal("1|from java", e.eval("fresh.x + '|' + fresh.hostFn()"), "an object built in Java is a script object");
        Check.equal(cfg, e.get("cfg"), "two handles of the same object are equal");
        Check.isTrue(cfg.hashCode() == e.get("cfg").hashCode(), "and have the same hash code");
        Check.isFalse(cfg.equals(e.get("fresh")), "handles of different objects differ");
        e.eval("var cyc = {}; cyc.me = cyc;");
        Check.contains(String.valueOf(e.get("cyc")), "Object", "toString does not loop on a cyclic object: " + e.get("cyc"));
        cfg.clear();
        Check.equal(0, cfg.size(), "clear");
        Check.throwsException(JsScriptError.class, new Block() {
            public void run() {
                JsEngine other = new JsEngine();
                other.eval("var frozen = Object.freeze({a: 1});");
                ((JsObjectHandle) other.get("frozen")).put("a", Integer.valueOf(2));
            }
        }, "a write that the script would refuse (strict) throws");
    }

    /**
     * An array handle is a live <code>List</code>.
     * <p>
     * 배열 핸들은 살아 있는 <code>List</code> 이다.
     */
    public void testArrayHandleIsALiveList() {
        JsEngine e = new JsEngine();
        e.eval("var arr = [10, 20, 30];");
        final JsArrayHandle arr = (JsArrayHandle) e.get("arr");
        Check.equal(3, arr.size(), "size");
        Check.equal(Double.valueOf(20), arr.get(1), "get");
        Check.equal(Double.valueOf(20), arr.set(1, "x"), "set returns the old element");
        Check.equal("10,x,30", e.eval("arr.join()"), "a write through the handle");
        arr.add("end");
        arr.add(0, Integer.valueOf(5));
        Check.equal("5,10,x,30,end", e.eval("arr.join()"), "add and add at an index");
        Check.equal("10", String.valueOf(((Double) arr.remove(1)).intValue()), "remove returns the element");
        Check.equal("5,x,30,end", e.eval("arr.join()"), "remove");
        e.eval("arr.push('script')");
        Check.equal(5, arr.size(), "the size follows the script");
        Check.equal("script", arr.get(4), "the last element");
        Check.equal("x", arr.get(1), "string element");
        Check.throwsException(IndexOutOfBoundsException.class, new Block() {
            public void run() {
                arr.get(5);
            }
        }, "index out of range");
        Check.throwsException(IndexOutOfBoundsException.class, new Block() {
            public void run() {
                arr.add(9, "x");
            }
        }, "add beyond the end");
        Check.equal(Double.valueOf(4), arr.callMember("indexOf", "script"), "array methods can be called");
        Check.isTrue(arr.contains("x"), "List methods of AbstractList work");
        Check.equal("[5.0, x, 30.0, end, script]", e.toPlain(arr).toString(), "toPlain of an array");
        arr.clear();
        Check.equal(0, arr.size(), "clear");
        JsArrayHandle made = e.newArray();
        made.add("a");
        made.add(Integer.valueOf(2));
        e.put("made", made);
        Check.equal("a,2", e.eval("made.join()"), "an array built in Java");
        Check.equal(Boolean.TRUE, e.eval("Array.isArray(made)"), "it is a real array");
        e.eval("var sparse = []; sparse[3] = 'x';");
        Check.same(JsEngine.UNDEFINED, ((JsArrayHandle) e.get("sparse")).get(0), "a hole reads as undefined");
    }

    /**
     * {@link JsEngine#toPlain(Object)} copies objects and arrays deeply, keeps cycles, leaves functions as handles.
     * <p>
     * {@link JsEngine#toPlain(Object)} 는 객체와 배열을 깊게 복사하고, 순환을 유지하며, 함수는 핸들로 남긴다.
     */
    public void testToPlainCopiesDeeply() {
        JsEngine e = new JsEngine();
        Object plain = e.toPlain(e.eval("({a: 1, list: [true, 'x', {k: null}], f: function () {}})"));
        Check.isTrue(plain instanceof LinkedHashMap, "a LinkedHashMap");
        Map<?, ?> map = (Map<?, ?>) plain;
        Check.equal(Double.valueOf(1), map.get("a"), "number");
        Check.isTrue(map.get("list") instanceof ArrayList, "an ArrayList");
        Check.equal(Boolean.TRUE, ((List<?>) map.get("list")).get(0), "element");
        Check.isTrue(((Map<?, ?>) ((List<?>) map.get("list")).get(2)).containsKey("k"), "nested object");
        Check.isTrue(map.get("f") instanceof JsFunctionHandle, "a function stays a handle");
        Object cyc = e.toPlain(e.eval("var c = {}; c.self = c; c"));
        Check.same(cyc, ((Map<?, ?>) cyc).get("self"), "a cycle keeps its shape");
        Check.equal("plain", e.toPlain("plain"), "a value that is not a handle is returned as it is");
    }

    /**
     * A host function is the way for a script to reach Java: arguments are converted, results are converted, and errors become script errors.
     * <p>
     * 호스트 함수는 스크립트가 Java 에 닿는 방법이다. 인수와 결과가 변환되고 오류는 스크립트 오류가 된다.
     */
    public void testHostFunctions() {
        final JsEngine e = new JsEngine();
        final List<Object> seen = new ArrayList<Object>();
        e.putFunction("record", new JsHostFunction() {
            public Object call(Object thisValue, Object[] args) {
                seen.add(thisValue);
                seen.addAll(Arrays.asList(args));
                return Integer.valueOf(args.length);
            }
        });
        Check.equal(Double.valueOf(3), e.eval("record(1, 'two', [3])"), "the result of the host function");
        Check.same(JsEngine.UNDEFINED, seen.get(0), "this is undefined for a plain call");
        Check.equal(Double.valueOf(1), seen.get(1), "a number argument arrives as Double");
        Check.equal("two", seen.get(2), "a string argument");
        Check.isTrue(seen.get(3) instanceof JsArrayHandle, "an array argument is a handle");
        seen.clear();
        e.eval("var holder = {m: record}; holder.m(null, undefined)");
        Check.isTrue(seen.get(0) instanceof JsObjectHandle, "this is the object of a method call");
        Check.isNull(seen.get(1), "null");
        Check.same(JsEngine.UNDEFINED, seen.get(2), "undefined");
        Check.equal("record", e.eval("record.name"), "the function is named after the variable");
        e.putFunction("fail", new JsHostFunction() {
            public Object call(Object thisValue, Object[] args) throws Exception {
                throw new IllegalStateException("java failure");
            }
        });
        Check.equal("Error:java.lang.IllegalStateException: java failure", e.eval("try { fail(); } catch (err) { err.name + ':' + err.message }"),
                "an exception of the host function is a script Error that the script can catch");
        JsScriptError uncaught = (JsScriptError) Check.throwsException(JsScriptError.class, new Block() {
            public void run() {
                e.eval("fail()");
            }
        }, "uncaught");
        Check.isTrue(uncaught.getCause() instanceof IllegalStateException, "the Java exception is the cause");
        e.putFunction("rethrow", new JsHostFunction() {
            public Object call(Object thisValue, Object[] args) {
                return e.eval("throw new TypeError('inner')");
            }
        });
        Check.equal("TypeError:inner", e.eval("try { rethrow(); } catch (err) { err.name + ':' + err.message }"), "a JsScriptError of a nested call keeps its identity");
        e.putFunction("bad", new JsHostFunction() {
            public Object call(Object thisValue, Object[] args) {
                return new Object();
            }
        });
        Check.equal("TypeError", e.eval("try { bad(); } catch (err) { err.name }"), "a result that cannot cross is a TypeError in the script");
        e.putFunction("reenter", new JsHostFunction() {
            public Object call(Object thisValue, Object[] args) {
                return e.eval("40 + 2");
            }
        });
        Check.equal(Double.valueOf(42), e.eval("reenter()"), "a host function may call the engine again");
        JsObjectHandle api = e.newObject();
        api.put("version", "1.0");
        api.put("sum", new JsHostFunction() {
            public Object call(Object thisValue, Object[] args) {
                return Double.valueOf(((Number) args[0]).doubleValue() + ((Number) args[1]).doubleValue());
            }
        });
        e.put("api", api);
        Check.equal("1.0:5", e.eval("api.version + ':' + api.sum(2, 3)"), "an object of host functions");
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("fn", new JsHostFunction() {
            public Object call(Object thisValue, Object[] args) {
                return "mapped";
            }
        });
        e.put("viaMap", map);
        Check.equal("mapped", e.eval("viaMap.fn()"), "a host function inside a Map");
    }

    /**
     * A host function that is called while the script is under a limit still obeys it, and cancellation passes through host functions.
     * <p>
     * 제한을 받는 스크립트가 호출한 호스트 함수에도 제한이 적용되며, 취소는 호스트 함수를 통과한다.
     */
    public void testAbortPassesThroughHostFunctions() {
        final JsEngine e = new JsEngine();
        e.putFunction("callback", new JsHostFunction() {
            public Object call(Object thisValue, Object[] args) {
                return ((JsCallable) args[0]).call();
            }
        });
        e.getLimits().setMaxSteps(50000L);
        Check.throwsException(org.duckdns.hjow.swingwebpanel.js.ScriptAbortedException.class, new Block() {
            public void run() {
                e.eval("callback(function () { while (true) { } })");
            }
        }, "the abort is not turned into a script error");
    }

    /**
     * A script function can be called back from Java through its handle, with this and as a constructor.
     * <p>
     * 스크립트 함수는 핸들로 Java 에서 다시 호출할 수 있다. this 를 주거나 생성자로도 호출한다.
     */
    public void testFunctionHandles() {
        JsEngine e = new JsEngine();
        JsFunctionHandle add = (JsFunctionHandle) e.eval("(function (a, b) { return a + b; })");
        Check.equal(Double.valueOf(5), add.call(Integer.valueOf(2), Integer.valueOf(3)), "call");
        JsFunctionHandle who = (JsFunctionHandle) e.eval("(function () { return this.tag; })");
        JsObjectHandle self = e.newObject();
        self.put("tag", "T");
        Check.equal("T", who.callWithThis(self), "callWithThis");
        JsFunctionHandle ctor = (JsFunctionHandle) e.eval("(function P(n) { this.n = n; })");
        JsObjectHandle made = (JsObjectHandle) ctor.construct("made");
        Check.equal("made", made.get("n"), "construct");
        Check.isTrue(add.isCallable() && !self.isCallable(), "isCallable");
        Check.equal("add", ((JsFunctionHandle) e.eval("(function add() {})")).get("name"), "a function is an object with properties");
    }

    /**
     * A Java interface can be implemented by a script object, by global functions and by a single function.
     * <p>
     * Java 인터페이스는 스크립트 객체, 전역 함수, 함수 하나로 구현할 수 있다.
     */
    public void testInterfaceProxies() {
        final JsEngine e = new JsEngine();
        e.eval("var calc = { add: function (a, b) { return a + b; }, name: function () { return 'calc'; }, flag: function () { return 1; } };"
                + " function add(a, b) { return a + b + 1; } function name() { return 'global'; } function flag() { return 0; }");
        Calc viaObject = e.getInterface(e.get("calc"), Calc.class);
        Check.notNull(viaObject, "an implementation");
        Check.equal(5, viaObject.add(2, 3), "int result of an object method");
        Check.equal("calc", viaObject.name(), "String result");
        Check.equal(true, viaObject.flag(), "boolean result from a truthy number");
        Calc viaGlobals = e.getInterface(null, Calc.class);
        Check.equal(6, viaGlobals.add(2, 3), "global functions implement the interface");
        Check.equal("global", viaGlobals.name(), "global String");
        Check.isNull(e.getInterface(e.eval("({add: function () {}})"), Calc.class), "null if a method is missing");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            public void run() {
                e.getInterface(null, String.class);
            }
        }, "not an interface");
        Runnable r = e.getInterface(e.eval("(function () { ran = true; })"), Runnable.class);
        r.run();
        Check.equal(Boolean.TRUE, e.get("ran"), "a function implements an interface with one method");
        final Calc failing = e.getInterface(e.eval("({add: function () { throw new RangeError('no'); }, name: function () {}, flag: function () {}})"), Calc.class);
        Check.throwsException(JsScriptError.class, new Block() {
            public void run() {
                failing.add(1, 2);
            }
        }, "a script error of an interface call");
        Check.equal(true, viaObject.equals(viaObject) && viaObject.hashCode() == viaObject.hashCode() && viaObject.toString().indexOf("Calc") > 0, "Object methods of the proxy");
        Check.equal(Integer.valueOf(7), e.convert(e.eval("7.9"), Integer.class), "convert to Integer");
        Check.equal("7.5", e.convert(e.eval("7.5"), String.class), "convert to String");
        Check.equal(Boolean.TRUE, e.convert("text", Boolean.class), "convert to Boolean");
        Check.equal(Long.valueOf(3), e.convert(Double.valueOf(3.5), Long.class), "convert to Long");
        Check.equal(4, e.convert(e.eval("({add: function (a, b) { return a + b; }, name: function () { return ''; }, flag: function () { return 0; }})"), Calc.class).add(1, 3),
                "convert a script object to an interface");
        Check.throwsException(ClassCastException.class, new Block() {
            public void run() {
                e.convert(Boolean.TRUE, Calc.class);
            }
        }, "a boolean is not a Calc");
    }

    /**
     * An interface that a script object implements, with primitive and boxed method signatures.
     * <p>
     * 스크립트 객체가 구현하는 인터페이스. 기본형과 박싱 타입 메서드 시그니처를 가진다.
     */
    public interface Calc {

        /**
         * Adds two numbers.
         * <p>
         * 두 수를 더한다.
         *
         * @param a first / 첫째
         * @param b second / 둘째
         * @return the sum / 합
         */
        int add(int a, int b);

        /**
         * Returns a name.
         * <p>
         * 이름을 반환한다.
         *
         * @return the name / 이름
         */
        String name();

        /**
         * Returns a flag.
         * <p>
         * 플래그를 반환한다.
         *
         * @return the flag / 플래그
         */
        boolean flag();
    }
}
