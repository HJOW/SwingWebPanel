package org.duckdns.hjow.swingwebpanel.js.host.structured;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.ArrayBufferObject;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the structured clone algorithm (plan item P20-09, first part) without any DOM: two independent realms, values made by script
 * in the first one and copied to the second one, which is then inspected by script. Covers primitives, wrapper objects, Date, RegExp,
 * arrays with holes, ordinary objects, Map, Set, cycles and shared references, ArrayBuffer sharing, typed arrays, DataView, errors, the values
 * that cannot be cloned, the limits and the transfer list.
 * <p>
 * 구조화 복제 알고리즘 시험(계획 P20-09 의 첫 부분). DOM 없이, 독립된 두 realm 에서 첫 realm 의 스크립트가 만든 값을 둘째 realm 으로 복사하고 스크립트로
 * 검사한다. 원시 값, 래퍼 객체, Date, RegExp, 빈 칸이 있는 배열, 일반 객체, Map, Set, 순환과 공유 참조, ArrayBuffer 공유, 형식화 배열, DataView, 오류,
 * 복제할 수 없는 값, 한도, 전달 목록을 다룬다.
 */
public class StructuredCloneTest {

    /**
     * The realm that makes the values.
     * <p>
     * 값을 만드는 realm.
     */
    private JsRealm from;

    /**
     * The realm that receives the copies.
     * <p>
     * 복사본을 받는 realm.
     */
    private JsRealm to;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public StructuredCloneTest() {
    }

    /**
     * Creates two fresh realms before each case.
     * <p>
     * 사례마다 새 realm 두 개를 만든다.
     */
    public void setUp() {
        from = JsRealm.create();
        to = JsRealm.create();
    }

    /**
     * Evaluates a script in a realm and returns the text of its completion value.
     * <p>
     * realm 에서 스크립트를 실행하고 완료 값의 문자열을 반환한다.
     *
     * @param realm  the realm / realm
     * @param script the script / 스크립트
     * @return the completion value as text / 완료 값의 문자열
     */
    private static String run(JsRealm realm, String script) {
        return Ops.toString(realm.evaluateScript(script, "test"));
    }

    /**
     * Makes a value in the first realm with an expression and clones it into the second realm as global <code>v</code>.
     * <p>
     * 첫 realm 에서 식으로 값을 만들어 둘째 realm 에 전역 <code>v</code> 로 복제한다.
     *
     * @param expression the expression / 식
     * @return the copy / 복사본
     */
    private JsValue clone(String expression) {
        JsValue src = from.evaluateScript("(" + expression + ")", "make");
        JsValue copy = StructuredClone.copy(from, to, src);
        to.getGlobalObject().set("v", copy);
        return copy;
    }

    /**
     * Checks the text of a script that runs in the second realm after {@link #clone}.
     * <p>
     * {@link #clone} 뒤에 둘째 realm 에서 실행한 스크립트의 문자열을 확인한다.
     *
     * @param script   the script / 스크립트
     * @param expected the expected text / 기대하는 문자열
     */
    private void expect(String script, String expected) {
        Check.equal(expected, run(to, script), script);
    }

    /**
     * Checks that cloning a resizable buffer preserves aliasing, a Float16 view and its length-tracking behavior.
     * <p>
     * 가변 버퍼 복제에서 참조 공유, Float16 뷰와 길이 추적 동작을 보존하는지 확인한다.
     */
    public void testResizableBufferAndViews() {
        clone("(()=>{const b=new ArrayBuffer(6,{maxByteLength:12}),a=new Float16Array(b,2),"
                + "fixed=new Float16Array(b,2,1),data=new DataView(b,2);a[0]=1.5;return {b,a,fixed,data};})()");
        expect("[v.b.resizable,v.b.maxByteLength,v.a instanceof Float16Array,v.a.buffer===v.b,"
                + "v.data.buffer===v.b,v.fixed.buffer===v.b,v.a[0]].join()", "true,12,true,true,true,true,1.5");
        expect("v.b.resize(10);[v.a.length,v.fixed.length,v.data.byteLength,v.a[0]].join()", "4,1,8,1.5");
        expect("v.b.resize(0);v.b.resize(2);[v.a.length,v.data.byteLength].join()", "0,0");
    }

    /**
     * Verifies the transfer reads the new backing store after serialization getters run.
     * <p>
     * 직렬화에서 getter를 실행한 뒤 전달할 저장소를 읽는지 확인한다.
     */
    public void testResizableTransferAfterGetter() {
        from.evaluateScript("var b=new ArrayBuffer(4,{maxByteLength:8});var a=new Float16Array(b,2,1);"
                + "var payload={b,a,get shrink(){b.resize(2);return true;}};", "make");
        List<JsObject> transfer = new ArrayList<JsObject>();
        transfer.add((JsObject) from.getGlobalObject().get("b"));
        ClonedValue clone = StructuredClone.serialize(from, from.getGlobalObject().get("payload"),
                StructuredClone.DEFAULT_MAX_BYTES, transfer);
        to.getGlobalObject().set("v", StructuredClone.deserialize(to, clone));
        Check.equal("true", run(from, "b.detached"), "the source is detached after transfer");
        expect("[v.b.resizable,v.b.byteLength,v.b.maxByteLength,v.a.length,v.a.buffer===v.b].join()", "true,2,8,0,true");
        expect("v.b.resize(4);[v.a.length,v.a.byteOffset,v.a[0]].join()", "1,2,0");
    }

    /**
     * Rejects views outside their buffers and revalidates transfer items after reentrant serialization.
     * <p>
     * 버퍼 범위를 벗어난 뷰를 거부하고 getter가 실행된 뒤 전달 항목을 다시 검증한다.
     */
    public void testResizableCloneInvalidBoundsAndTransfer() {
        from.evaluateScript("var b=new ArrayBuffer(8,{maxByteLength:16});"
                + "var a=new Float16Array(b,4,1), view=new DataView(b,4,1);b.resize(2);"
                + "var first=new ArrayBuffer(2),last=new ArrayBuffer(2,{maxByteLength:4});"
                + "var payload={first,last,get detach(){last.transfer();return 0;}};", "make");
        final JsObject globals = from.getGlobalObject();
        final String[] names = { "a", "view" };
        // 형식화 배열과 DataView가 범위를 벗어났을 때 모두 복제를 거부한다.
        for (final String name : names) {
            Check.throwsException(StructuredCloneException.class, new Block() {
                /**
                 * Attempts to serialize an out-of-bounds view.
                 * <p>
                 * 버퍼 범위를 벗어난 뷰를 직렬화한다.
                 */
                public void run() {
                    StructuredClone.serialize(from, globals.get(name));
                }
            }, "view out of bounds: " + name);
        }
        final List<JsObject> transfer = new ArrayList<JsObject>();
        transfer.add((JsObject) globals.get("first"));
        transfer.add((JsObject) globals.get("last"));
        Check.throwsException(StructuredCloneException.class, new Block() {
            /**
             * Attempts a transfer after a getter detached one item.
             * <p>
             * getter가 항목 하나를 분리한 뒤 전달을 시도한다.
             */
            public void run() {
                StructuredClone.serialize(from, globals.get("payload"), StructuredClone.DEFAULT_MAX_BYTES, transfer);
            }
        }, "detachment during traversal rejects the transfer");
        Check.equal("false,true", run(from, "[first.detached,last.detached].join()"), "the remaining buffer is preserved");
    }

    /**
     * Primitives keep their values, including negative zero, NaN, lone surrogates and Korean text.
     * <p>
     * 원시 값은 음의 0, NaN, 외톨이 서로게이트, 한글을 포함해 값을 유지한다.
     */
    public void testPrimitives() {
        clone("undefined");
        expect("typeof v", "undefined");
        clone("null");
        expect("String(v === null)", "true");
        clone("true");
        expect("v === true", "true");
        clone("-0");
        expect("1 / v", "-Infinity");
        clone("NaN");
        expect("String(v !== v)", "true");
        clone("12345.678");
        expect("v", "12345.678");
        clone("123456789012345678901234567890n");
        expect("typeof v + ':' + v", "bigint:123456789012345678901234567890");
        clone("'\\ud83d \\ud83d\\ude00 \\u0000 한글'");
        expect("v.length + ':' + v.charCodeAt(0) + ':' + v.charCodeAt(2) + ':' + v.charCodeAt(5)", "9:55357:55357:0");
        expect("v.slice(-2)", "한글");
    }

    /**
     * Wrapper objects of Boolean, Number and String are copied as wrapper objects of the target realm.
     * <p>
     * Boolean, Number, String 래퍼 객체는 대상 realm 의 래퍼 객체로 복사된다.
     */
    public void testWrapperObjects() {
        clone("new Boolean(false)");
        expect("typeof v + ':' + (v instanceof Boolean) + ':' + v.valueOf()", "object:true:false");
        clone("new Number(-0)");
        expect("(v instanceof Number) + ':' + (1 / v.valueOf())", "true:-Infinity");
        clone("Object(12345678901234567890n)");
        expect("(typeof v) + ':' + (v instanceof BigInt) + ':' + v.valueOf()",
                "object:true:12345678901234567890");
        clone("new String('ab')");
        expect("(v instanceof String) + ':' + v.length + ':' + v.valueOf() + ':' + v[1]", "true:2:ab:b");
    }

    /**
     * Dates keep their time value (also the invalid date); a RegExp keeps source and flags but loses lastIndex.
     * <p>
     * Date 는 시간 값(잘못된 날짜 포함)을 유지하고, RegExp 는 원문과 플래그를 유지하지만 lastIndex 를 잃는다.
     */
    public void testDateAndRegExp() {
        clone("new Date(1234567890123)");
        expect("(v instanceof Date) + ':' + v.getTime()", "true:1234567890123");
        clone("new Date(NaN)");
        expect("String(isNaN(v.getTime()))", "true");
        clone("(function () { var r = /a(b)c/gi; r.lastIndex = 3; return r; })()");
        expect("(v instanceof RegExp) + ':' + v.source + ':' + v.flags + ':' + v.lastIndex", "true:a(b)c:gi:0");
        expect("String(v.test('xxABC'))", "true");
    }

    /**
     * Arrays keep holes, length and extra properties; ordinary objects keep own enumerable string keyed properties only.
     * <p>
     * 배열은 빈 칸, length, 추가 프로퍼티를 유지하고, 일반 객체는 자체 열거 가능한 문자열 키 프로퍼티만 유지한다.
     */
    public void testArraysAndObjects() {
        clone("(function () { var a = [1, , 3]; a.length = 6; a.extra = 'e'; return a; })()");
        expect("(v instanceof Array) + ':' + v.length + ':' + (1 in v) + ':' + (3 in v) + ':' + v[2] + ':' + v.extra", "true:6:false:false:3:e");
        clone("(function () { var o = { a: 1, b: { c: [true, 'x'] } }; Object.defineProperty(o, 'hidden', { value: 1, enumerable: false });"
                + " o[Symbol('s')] = 2; return o; })()");
        expect("Object.keys(v).join() + ':' + v.b.c[1] + ':' + ('hidden' in v) + ':' + Object.getOwnPropertySymbols(v).length", "a,b:x:false:0");
        expect("String(Object.getPrototypeOf(v) === Object.prototype)", "true");
        expect("String(Object.getPrototypeOf(v.b.c) === Array.prototype)", "true");
    }

    /**
     * The prototype of an instance is dropped, getters are read as values and an own key <code>__proto__</code> stays an own property.
     * <p>
     * 인스턴스의 프로토타입은 버려지고, getter 는 값으로 읽히며, 자체 키 <code>__proto__</code> 는 자체 프로퍼티로 남는다.
     */
    public void testClassInstancesGettersAndProtoKey() {
        clone("(function () { class P { constructor() { this.x = 1; } get y() { return 2; } } return new P(); })()");
        expect("Object.keys(v).join() + ':' + (v.constructor === Object)", "x:true");
        clone("({ get g() { return 'got'; }, set s(x) {}, n: 1 })");
        expect("Object.keys(v).join() + ':' + v.g + ':' + String(Object.getOwnPropertyDescriptor(v, 'g').get)", "g,s,n:got:undefined");
        clone("JSON.parse('{\"__proto__\": {\"polluted\": true}, \"k\": 1}')");
        expect("Object.keys(v).join() + ':' + String(v.polluted) + ':' + Object.prototype.hasOwnProperty.call(v, '__proto__')", "__proto__,k:undefined:true");
    }

    /**
     * A getter that deletes a later key makes the walk skip it; a getter that throws propagates its error.
     * <p>
     * 뒤쪽 키를 지우는 getter 가 있으면 탐색이 그 키를 건너뛰고, getter 가 던진 오류는 그대로 전파된다.
     */
    public void testGetterSideEffects() {
        clone("(function () { var o = { get a() { delete this.b; return 1; }, b: 2, c: 3 }; return o; })()");
        expect("Object.keys(v).join()", "a,c");
        final JsValue bad = from.evaluateScript("({ get boom() { throw new TypeError('no'); } })", "bad");
        JsException e = (JsException) Check.throwsException(JsException.class, new Block() {
            /**
             * Serializes the object with the throwing getter.
             * <p>
             * 던지는 getter 가 있는 객체를 직렬화한다.
             *
             * @throws Exception never / 던지지 않음
             */
            public void run() throws Exception {
                StructuredClone.serialize(from, bad);
            }
        }, "a getter error propagates");
        Check.equal("TypeError", Ops.toString(((JsObject) e.getValue(from)).get("name")), "the error of the getter");
    }

    /**
     * Map and Set keep insertion order, object keys, NaN and -0 handling of SameValueZero.
     * <p>
     * Map 과 Set 은 삽입 순서, 객체 키, SameValueZero 의 NaN 과 -0 처리를 유지한다.
     */
    public void testMapAndSet() {
        clone("(function () { var m = new Map(); var k = { id: 1 }; m.set('b', 1).set(k, { v: [k] }).set(NaN, 'nan').set(-0, 'zero');"
                + " m.set('self', m); return m; })()");
        expect("(v instanceof Map) + ':' + v.size + ':' + Array.from(v.keys()).map(function (x) { return typeof x === 'object' ? 'obj' : String(x); }).join()",
                "true:5:b,obj,NaN,0,self");
        expect("var ko = Array.from(v.keys())[1]; String(v.get(ko).v[0] === ko) + ':' + v.get(NaN) + ':' + v.get(0) + ':' + String(v.get('self') === v)",
                "true:nan:zero:true");
        clone("new Set([1, 'a', { z: 1 }, 1, NaN, NaN])");
        expect("(v instanceof Set) + ':' + v.size + ':' + Array.from(v).map(function (x) { return typeof x === 'object' ? 'obj' : String(x); }).join()",
                "true:4:1,a,obj,NaN");
    }

    /**
     * Cycles and shared references are kept.
     * <p>
     * 순환과 공유 참조가 유지된다.
     */
    public void testCyclesAndSharing() {
        clone("(function () { var shared = { s: 1 }; var o = { x: shared, y: shared, list: [shared] }; o.self = o; o.list.push(o.list); return o; })()");
        expect("String(v.x === v.y) + ':' + String(v.list[0] === v.x) + ':' + String(v.self === v) + ':' + String(v.list[1] === v.list)", "true:true:true:true");
        expect("String(v.x !== undefined && v.x.s === 1)", "true");
    }

    /**
     * Typed arrays that share an ArrayBuffer still share it after the copy, with their offsets, lengths and contents; DataView too.
     * <p>
     * ArrayBuffer 를 공유하던 형식화 배열은 복사 뒤에도 위치, 길이, 내용과 함께 계속 공유하며, DataView 도 그렇다.
     */
    public void testBuffersAndViews() {
        clone("(function () { var buf = new ArrayBuffer(16); var u8 = new Uint8Array(buf, 2, 4); u8.set([1, 2, 3, 4]);"
                + " var f32 = new Float32Array(buf, 8, 2); f32[0] = 1.5; f32[1] = -2;"
                + " var dv = new DataView(buf, 4, 8); dv.setInt16(0, -2, true);"
                + " return { buf: buf, u8: u8, f32: f32, dv: dv, whole: new Int8Array(buf) }; })()");
        expect("String(v.u8.buffer === v.buf) + ':' + String(v.f32.buffer === v.buf) + ':' + String(v.dv.buffer === v.buf) + ':' + String(v.whole.buffer === v.buf)",
                "true:true:true:true");
        expect("v.buf.byteLength + ':' + v.u8.byteOffset + ':' + v.u8.length + ':' + v.f32.byteOffset + ':' + v.f32.length + ':' + v.dv.byteOffset + ':' + v.dv.byteLength",
                "16:2:4:8:2:4:8");
        expect("(v.u8 instanceof Uint8Array) + ':' + (v.f32 instanceof Float32Array) + ':' + (v.dv instanceof DataView) + ':' + (v.buf instanceof ArrayBuffer)",
                "true:true:true:true");
        expect("v.f32[0] + ':' + v.f32[1] + ':' + v.dv.getInt16(0, true) + ':' + v.u8[0] + v.u8[1]", "1.5:-2:-2:12");
        // 한쪽 뷰의 변경이 같은 버퍼를 보는 다른 뷰에서도 보인다.
        expect("v.u8[0] = 9; String(v.whole[2])", "9");
    }

    /**
     * Every kind of typed array is copied with its own element type.
     * <p>
     * 모든 종류의 형식화 배열은 자기 요소 형식으로 복사된다.
     */
    public void testAllTypedArrayKinds() {
        String[] kinds = { "Int8Array", "Uint8Array", "Uint8ClampedArray", "Int16Array", "Uint16Array", "Int32Array", "Uint32Array",
            "Float32Array", "Float64Array" };
        // 종류마다 같은 값을 복사하고 생성자와 내용을 확인한다.
        for (int i = 0; i < kinds.length; i++) {
            clone("new " + kinds[i] + "([1, 2, 3])");
            expect("(v instanceof " + kinds[i] + ") + ':' + v.join()", "true:1,2,3");
        }
    }

    /**
     * An error keeps its native name, message and stack; an unknown name becomes <code>Error</code>.
     * <p>
     * 오류는 기본 이름, 메시지, stack 을 유지하고 모르는 이름은 <code>Error</code> 가 된다.
     */
    public void testErrors() {
        clone("(function () { var e = new RangeError('too far'); e.stack = 'RangeError: too far\\n    at somewhere'; return e; })()");
        expect("(v instanceof RangeError) + ':' + v.name + ':' + v.message + ':' + v.stack.split('\\n').length", "true:RangeError:too far:2");
        expect("Object.keys(v).length + ':' + String(Object.getOwnPropertyDescriptor(v, 'message').enumerable)", "0:false");
        clone("(function () { var e = new TypeError('t'); e.name = 'MyError'; return e; })()");
        expect("(v instanceof Error) + ':' + String(v instanceof TypeError) + ':' + v.name + ':' + v.message", "true:false:Error:t");
        clone("new Error()");
        expect("v.message + ':' + v.hasOwnProperty('message')", ":false");
    }

    /**
     * Functions, symbols, Proxy objects, WeakMap, Promise and objects that contain them cannot be cloned.
     * <p>
     * 함수, 심볼, Proxy, WeakMap, Promise 와 그것을 담은 객체는 복제할 수 없다.
     */
    public void testNotCloneable() {
        String[] bad = { "function () {}", "(x) => x", "Symbol('s')", "new Proxy({}, {})", "new WeakMap()", "new WeakSet()", "Promise.resolve(1)",
            "({ f: function () {} })", "[1, [2, Symbol.iterator]]", "new Map([[1, function () {}]])", "(function () { return arguments; })()",
            "Object(Symbol('w'))", "[].values()", "new Map().entries()" };
        // 복제할 수 없는 값마다 DataCloneError 에 해당하는 예외가 나야 한다.
        for (int i = 0; i < bad.length; i++) {
            final JsValue v = from.evaluateScript("(" + bad[i] + ")", "bad");
            final String what = bad[i];
            StructuredCloneException e = (StructuredCloneException) Check.throwsException(StructuredCloneException.class, new Block() {
                /**
                 * Serializes the value that must be refused.
                 * <p>
                 * 거부해야 하는 값을 직렬화한다.
                 *
                 * @throws Exception never / 던지지 않음
                 */
                public void run() throws Exception {
                    StructuredClone.serialize(from, v);
                }
            }, what + " must not be cloneable");
            Check.equal("DataCloneError", e.getName(), "the error name for " + what);
            Check.contains(e.getMessage(), "could not be cloned", "the message for " + what);
        }
    }

    /**
     * A copy made by deserializing the same serialized form twice is independent, and changing the source afterwards does not change the form.
     * <p>
     * 같은 직렬화 형태를 두 번 역직렬화한 복사본은 서로 독립이며, 이후 원본을 바꿔도 직렬화한 형태는 바뀌지 않는다.
     */
    public void testIndependentCopies() {
        from.evaluateScript("var src = { n: [1, 2], buf: new Uint8Array([5, 6]) };", "src");
        ClonedValue c = StructuredClone.serialize(from, ((JsObject) from.getGlobalObject()).get("src"));
        from.evaluateScript("src.n.push(3); src.buf[0] = 99;", "mutate");
        JsValue one = StructuredClone.deserialize(to, c);
        JsValue two = StructuredClone.deserialize(to, c);
        to.getGlobalObject().set("one", one);
        to.getGlobalObject().set("two", two);
        Check.equal("2:5", run(to, "one.n.length + ':' + one.buf[0]"), "the form keeps the state at serialization time");
        Check.equal("false", run(to, "String(one === two || one.n === two.n || one.buf.buffer === two.buf.buffer)"), "copies are independent objects");
        run(to, "one.buf[0] = 1; one.n.push(7);");
        Check.equal("2:5", run(to, "two.n.length + ':' + two.buf[0]"), "changing one copy leaves the other");
        Check.isTrue(c.getSize() > 0 && c.getObjectCount() >= 3, "the form reports its size");
        Check.isFalse(c.isPrimitive(), "an object is not primitive");
    }

    /**
     * The copy uses the prototypes of the target realm.
     * <p>
     * 복사본은 대상 realm 의 프로토타입을 쓴다.
     */
    public void testPrototypesOfTargetRealm() {
        JsValue src = from.evaluateScript("({ a: [1], d: new Date(0), m: new Map() })", "src");
        JsValue copy = StructuredClone.copy(from, to, src);
        to.getGlobalObject().set("v", copy);
        expect("String(Object.getPrototypeOf(v) === Object.prototype) + ':' + String(Object.getPrototypeOf(v.a) === Array.prototype) + ':'"
                + " + String(Object.getPrototypeOf(v.d) === Date.prototype) + ':' + String(Object.getPrototypeOf(v.m) === Map.prototype)", "true:true:true:true");
        from.evaluateScript("var fa = [1];", "fa");
        JsValue fromArray = ((JsObject) from.getGlobalObject()).get("fa");
        JsObject toArrayProto = to.getArrayPrototype();
        Check.same(toArrayProto, ((JsObject) StructuredClone.copy(from, to, fromArray)).getPrototypeOf(), "the array of the copy uses the target Array.prototype");
        Check.isFalse(from.getArrayPrototype() == toArrayProto, "the realms are separate");
    }

    /**
     * Deep structures do not overflow the Java stack; absurd depth, too many bytes and a small byte limit are refused.
     * <p>
     * 깊은 구조도 Java 스택을 넘치게 하지 않으며, 터무니없는 깊이와 너무 많은 바이트, 작은 바이트 한도는 거부된다.
     */
    public void testLimits() {
        clone("(function () { var o = {}, cur = o; for (var i = 0; i < 5000; i++) { cur.n = {}; cur = cur.n; } return o; })()");
        expect("var d = 0, c = v; while (c.n) { c = c.n; d++; } d", "5000");
        final JsValue tooDeep = from.evaluateScript("(function () { var o = {}, cur = o; for (var i = 0; i < 20000; i++) { cur = cur.n = {}; } return o; })()", "deep");
        Check.throwsException(StructuredCloneException.class, new Block() {
            /**
             * Serializes the structure that is nested too deeply.
             * <p>
             * 너무 깊게 중첩된 구조를 직렬화한다.
             *
             * @throws Exception never / 던지지 않음
             */
            public void run() throws Exception {
                StructuredClone.serialize(from, tooDeep);
            }
        }, "nesting beyond MAX_DEPTH is refused");
        final JsValue big = from.evaluateScript("new Array(100001).join('x')", "big");
        Check.throwsException(StructuredCloneException.class, new Block() {
            /**
             * Serializes a string larger than the byte limit.
             * <p>
             * 바이트 한도보다 큰 문자열을 직렬화한다.
             *
             * @throws Exception never / 던지지 않음
             */
            public void run() throws Exception {
                StructuredClone.serialize(from, big, 1000, null);
            }
        }, "a value larger than the byte limit is refused");
        Check.isTrue(StructuredClone.serialize(from, big, 1000000, null).getSize() >= 200000, "the same value fits a larger limit");
    }

    /**
     * Buffers of the transfer list move instead of being copied: the source is detached and the bytes arrive; invalid lists are refused.
     * <p>
     * 전달 목록의 버퍼는 복사되지 않고 옮겨진다: 원본은 분리되고 바이트가 도착하며, 잘못된 목록은 거부된다.
     */
    public void testTransfer() {
        from.evaluateScript("var tb = new ArrayBuffer(8); new Uint8Array(tb).set([1, 2, 3]); var other = new ArrayBuffer(4); var holder = { b: tb, u: new Uint8Array(tb, 1, 2) };", "setup");
        JsObject g = from.getGlobalObject();
        List<JsObject> list = new ArrayList<JsObject>();
        list.add((JsObject) g.get("tb"));
        list.add((JsObject) g.get("other"));
        ClonedValue c = StructuredClone.serialize(from, g.get("holder"), StructuredClone.DEFAULT_MAX_BYTES, list);
        Check.equal("0:0:true", run(from, "tb.byteLength + ':' + other.byteLength + ':' + (holder.u.length === 0)"), "the sources are detached");
        Check.isTrue(((ArrayBufferObject) g.get("tb")).isDetached(), "the buffer object reports detachment");
        to.getGlobalObject().set("v", StructuredClone.deserialize(to, c));
        expect("v.b.byteLength + ':' + v.u.byteOffset + ':' + v.u[0] + v.u[1] + ':' + String(v.u.buffer === v.b)", "8:1:23:true");
        // 이미 분리된 버퍼, 중복 항목, ArrayBuffer 가 아닌 항목은 전달할 수 없다.
        final List<JsObject> again = new ArrayList<JsObject>();
        again.add((JsObject) g.get("tb"));
        Check.throwsException(StructuredCloneException.class, new Block() {
            /**
             * Transfers a detached buffer.
             * <p>
             * 분리된 버퍼를 전달한다.
             *
             * @throws Exception never / 던지지 않음
             */
            public void run() throws Exception {
                StructuredClone.serialize(from, JsValueHolder.UNDEFINED, StructuredClone.DEFAULT_MAX_BYTES, again);
            }
        }, "a detached buffer cannot be transferred");
        from.evaluateScript("var fresh = new ArrayBuffer(2);", "fresh");
        final List<JsObject> dup = new ArrayList<JsObject>();
        dup.add((JsObject) g.get("fresh"));
        dup.add((JsObject) g.get("fresh"));
        Check.throwsException(StructuredCloneException.class, new Block() {
            /**
             * Transfers the same buffer twice.
             * <p>
             * 같은 버퍼를 두 번 전달한다.
             *
             * @throws Exception never / 던지지 않음
             */
            public void run() throws Exception {
                StructuredClone.serialize(from, JsValueHolder.UNDEFINED, StructuredClone.DEFAULT_MAX_BYTES, dup);
            }
        }, "duplicate transfer entries are refused");
        Check.equal("2", run(from, "String(fresh.byteLength)"), "a refused transfer detaches nothing");
        final List<JsObject> notBuffer = new ArrayList<JsObject>();
        notBuffer.add((JsObject) from.evaluateScript("({})", "plain"));
        Check.throwsException(StructuredCloneException.class, new Block() {
            /**
             * Transfers an object that is not transferable.
             * <p>
             * 전달할 수 없는 객체를 전달한다.
             *
             * @throws Exception never / 던지지 않음
             */
            public void run() throws Exception {
                StructuredClone.serialize(from, JsValueHolder.UNDEFINED, StructuredClone.DEFAULT_MAX_BYTES, notBuffer);
            }
        }, "a plain object is not transferable");
    }

    /**
     * Holds shared constants of this test.
     * <p>
     * 이 시험이 공유하는 상수를 담는다.
     */
    private static final class JsValueHolder {
        /**
         * The value undefined.
         * <p>
         * 정의되지 않음(undefined)을 뜻하는 값.
         */
        static final JsValue UNDEFINED = org.duckdns.hjow.swingwebpanel.js.Js.UNDEFINED;
    }
}
