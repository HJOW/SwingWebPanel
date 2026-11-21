package org.duckdns.hjow.swingwebpanel.js.builtins.typed;

import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Script level tests: JavaScript source text runs in a complete standard realm (<code>JsRealm.create()</code>) so that the binary data library is
 * tested together with the interpreter, for-of, destructuring, classes and the other built-in objects (plan item P14-06).
 * <p>
 * 스크립트 수준 시험: JavaScript 소스를 완전한 표준 realm(<code>JsRealm.create()</code>)에서 실행하여 이진 데이터 라이브러리를 인터프리터, for-of,
 * 구조 분해, 클래스, 다른 내장 객체와 함께 시험한다(계획 P14-06).
 */
public class TypedArrayScriptTest {

    /**
     * The realm used by a case.
     * <p>
     * 사례가 쓰는 realm.
     */
    private JsRealm realm;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public TypedArrayScriptTest() {
    }

    /**
     * Creates a fresh complete realm before each case.
     * <p>
     * 사례마다 새 완전한 realm 을 만든다.
     */
    public void setUp() {
        realm = JsRealm.create();
    }

    /**
     * Runs a script and returns the completion value as text; a thrown JavaScript error fails the case with its message.
     * <p>
     * 스크립트를 실행하고 완료 값을 문자열로 돌려준다. JavaScript 오류가 던져지면 그 메시지와 함께 사례가 실패한다.
     *
     * @param source the script / 스크립트
     * @return the result text / 결과 문자열
     */
    private String run(String source) {
        try {
            return Ops.toString(realm.evaluateScript(source, "typed-script-test"));
        } catch (JsException e) {
            Check.fail("script failed: " + e.getMessage() + " in: " + source);
            return null;
        }
    }

    /**
     * Checks the text result of a script.
     * <p>
     * 스크립트의 문자열 결과를 확인한다.
     *
     * @param expected the expected text / 기대하는 문자열
     * @param source   the script / 스크립트
     */
    private void check(String expected, String source) {
        Check.equal(expected, run(source), source);
    }

    /**
     * The constructors exist, convert values and print as expected.
     * <p>
     * 생성자가 있고 값을 변환하며 기대대로 출력된다.
     */
    public void testConstructionAndConversion() {
        check("1,2,44", "new Uint8Array([1, 2, 300]).join()");
        check("127,-128,-1", "new Int8Array([127, 128, 255]).join()");
        check("0,255,128", "new Uint8ClampedArray([-5, 1000, 127.5]).join()");
        check("1.5,0.10000000149011612", "new Float32Array([1.5, 0.1]).join()");
        check("4", "new Uint8Array(4).length");
        check("2,3", "new Uint8Array(new Uint16Array([2, 259])).join()");
        check("[object Int8Array]", "Object.prototype.toString.call(new Int8Array(1))");
        check("[object ArrayBuffer]", "Object.prototype.toString.call(new ArrayBuffer(1))");
        check("[object DataView]", "Object.prototype.toString.call(new DataView(new ArrayBuffer(1)))");
        check("1,8,2,4,8", "[Uint8Array.BYTES_PER_ELEMENT, Float64Array.BYTES_PER_ELEMENT, Int16Array.BYTES_PER_ELEMENT, Float32Array.BYTES_PER_ELEMENT, new Float64Array(1).BYTES_PER_ELEMENT].join()");
    }

    /**
     * Element access through brackets, canonical numeric keys and enumeration.
     * <p>
     * 대괄호를 통한 요소 접근, 정규 숫자 키, 열거.
     */
    public void testElementAccessAndEnumeration() {
        check("4||||0,1,2,3", "var a = new Uint8Array(4); a[0] = 1; a[5] = 9; a[-1] = 5; a['1.5'] = 2; "
                + "[a.length, a[5], a[-1], a['1.5'], Object.keys(a).join()].join('|')");
        check("01", "var s = ''; for (var i in new Uint8Array(2)) { s += i; } s");
        check("true,false", "var t = new Uint8Array(2); ['1' in t, '2' in t].join()");
        check("5,true,true,false", "var d = Object.getOwnPropertyDescriptor(new Uint8Array([5]), 0); [d.value, d.writable, d.enumerable, d.configurable].join()");
        check("abc", "var u = new Uint8Array(1); u['01'] = 'abc'; u['01']");
        check("false", "Array.isArray(new Uint8Array(1))");
    }

    /**
     * JSON.stringify sees a typed array as a plain object with index keys (it belongs to the data library of another worker; the case is skipped,
     * with its reason, while JSON is not part of the standard realm yet).
     * <p>
     * JSON.stringify 는 형식화 배열을 인덱스 키를 가진 일반 객체로 본다(다른 작업자의 데이터 라이브러리에 속하므로 JSON 이 표준 realm 에 아직 없으면 이유를
     * 밝히고 건너뛴다).
     */
    public void testJsonStringifyOfTypedArray() {
        if (!"object".equals(run("typeof JSON"))) {
            Check.skip("JSON is not installed in the standard realm yet (the data library installer of another worker is not connected)");
        }
        check("{\"0\":1,\"1\":2}", "JSON.stringify(new Uint8Array([1, 2]))");
    }

    /**
     * Typed arrays are iterable: for-of, spread, destructuring and Array.from work.
     * <p>
     * 형식화 배열은 반복 가능하다: for-of, 전개, 구조 분해, Array.from 이 동작한다.
     */
    public void testIteration() {
        check("34", "var s = ''; for (var x of new Uint8Array([3, 4])) { s += x; } s");
        check("1.5,2", "[...new Float32Array([1.5, 2])].join()");
        check("15", "var [p, q] = new Int8Array([7, 8]); p + q");
        check("0:5,1:6", "var r = []; for (var [i, v] of new Uint8Array([5, 6]).entries()) { r.push(i + ':' + v); } r.join()");
        check("[object Array Iterator]", "Object.prototype.toString.call(new Uint8Array(1).values())");
        check("true", "var ta = new Uint8Array(1); ta[Symbol.iterator] === ta.values");
        check("2,3", "Array.prototype.slice.call(new Uint8Array([1, 2, 3]), 1).join()");
    }

    /**
     * Array.from accepts typed arrays (it belongs to the Array library of another worker; the case is skipped, with its reason, while that
     * function does not exist yet).
     * <p>
     * Array.from 은 형식화 배열을 받아들인다(다른 작업자의 Array 라이브러리에 속하므로 그 함수가 아직 없으면 이유를 밝히고 건너뛴다).
     */
    public void testArrayFromAcceptsTypedArrays() {
        if (!"function".equals(run("typeof Array.from"))) {
            Check.skip("Array.from is not implemented yet by the Array built-ins (another worker); typed array iteration itself is covered by the for-of and spread cases");
        }
        check("1,2,3", "Array.from(new Uint8Array([1, 2, 3])).join()");
        check("2,4", "Array.from(new Uint8Array([1, 2]), function (x) { return x * 2; }).join()");
    }

    /**
     * The static methods, subclassing, and the relation between the intrinsic prototype objects.
     * <p>
     * 정적 메서드, 하위 클래스 만들기, 내장 프로토타입 객체 사이의 관계.
     */
    public void testStaticsAndSubclassing() {
        check("10,12", "Uint8Array.from({ length: 2, 0: 5, 1: 6 }, function (x) { return x * 2; }).join()");
        check("1,2,3", "Uint8Array.of(1, 2, 3).join()");
        check("1,2,3", "Uint8Array.from('123').join()");
        check("true", "Object.getPrototypeOf(Uint8Array) === Object.getPrototypeOf(Int8Array)");
        check("TypedArray", "Object.getPrototypeOf(Uint8Array).name");
        check("true", "Object.getPrototypeOf(Uint8Array.prototype) === Object.getPrototypeOf(Int8Array.prototype)");
        check("6,true,true,true", "class MyArr extends Uint8Array { sum() { var t = 0; for (var v of this) { t += v; } return t; } } "
                + "var m = new MyArr([1, 2, 3]); [m.sum(), m instanceof Uint8Array, m.map(function (x) { return x * 2; }) instanceof MyArr, m.constructor === MyArr].join()");
    }

    /**
     * Methods behave as documented when called from script, with callbacks and chaining.
     * <p>
     * 메서드는 스크립트에서 콜백과 연쇄 호출로 쓸 때 문서대로 동작한다.
     */
    public void testMethodsFromScript() {
        check("2,4,6", "new Uint8Array([1, 2, 3]).map(function (x) { return x * 2; }).join()");
        check("2,4", "new Uint8Array([1, 2, 3, 4]).filter(function (x) { return x % 2 === 0; }).join()");
        check("10", "new Uint8Array([1, 2, 3, 4]).reduce(function (a, b) { return a + b; })");
        check("-1,0,0,3,NaN", "new Float64Array([3, NaN, -0, 0, -1]).sort().join()");
        check("9,10,100", "new Uint8Array([100, 10, 9]).sort().join()");
        check("4,3,2,1", "new Uint8Array([2, 4, 1, 3]).sort(function (a, b) { return b - a; }).join()");
        check("7,7,7", "new Uint8Array(3).fill(7).toString()");
        check("3,2,1", "new Uint8Array([1, 2, 3]).reverse().join()");
        check("2,3", "new Uint8Array([1, 2, 3]).subarray(1).join()");
        check("1", "new Uint8Array([1, 2, 3]).subarray(1).byteOffset");
        check("1,1,2,3,5", "new Uint8Array([1, 2, 3, 4, 5]).copyWithin(1, 0, 3).join()");
        check("1,2,3,1,2", "var t = new Uint8Array(5); t.set([1, 2, 3]); t.set(t.subarray(0, 2), 3); t.join()");
        check("6", "new ArrayBuffer(8).slice(2).byteLength");
        check("true,false", "[ArrayBuffer.isView(new Uint8Array(1)), ArrayBuffer.isView({})].join()");
    }

    /**
     * DataView reads and writes with both byte orders and shares memory with typed arrays.
     * <p>
     * DataView 는 두 바이트 순서로 읽고 쓰며 형식화 배열과 메모리를 공유한다.
     */
    public void testDataViewFromScript() {
        check("18,52,13330,4660", "var d = new DataView(new ArrayBuffer(8)); d.setUint16(0, 0x1234); [d.getUint8(0), d.getUint8(1), d.getUint16(0, true), d.getUint16(0)].join()");
        check("2,1", "var b = new ArrayBuffer(2); new Uint16Array(b)[0] = 0x0102; var u = new Uint8Array(b); [u[0], u[1]].join()");
        check("3.5", "var f = new DataView(new ArrayBuffer(8)); f.setFloat32(0, 3.5, true); f.getFloat32(0, true)");
        check("-2", "var g = new DataView(new ArrayBuffer(4)); g.setInt32(0, -2); g.getInt32(0)");
    }

    /**
     * Errors thrown by the library are real JavaScript errors of the right kind that scripts can catch.
     * <p>
     * 라이브러리가 던지는 오류는 스크립트가 잡을 수 있는 올바른 종류의 진짜 JavaScript 오류이다.
     */
    public void testErrorsFromScript() {
        check("RangeError", "try { new Uint8Array(-1); 'no error'; } catch (e) { e.name; }");
        check("TypeError", "try { Uint8Array(1); 'no error'; } catch (e) { e.name; }");
        check("RangeError", "try { new Int32Array(new ArrayBuffer(8), 1); 'no error'; } catch (e) { e.name; }");
        check("RangeError", "try { new Uint8Array(2).set([1, 2, 3]); 'no error'; } catch (e) { e.name; }");
        check("TypeError", "try { new Uint8Array(1).sort(5); 'no error'; } catch (e) { e.name; }");
        check("RangeError", "try { new DataView(new ArrayBuffer(2)).getInt32(0); 'no error'; } catch (e) { e.name; }");
        check("TypeError", "try { Object.getPrototypeOf(Uint8Array)(); 'no error'; } catch (e) { e.name; }");
        check("TypeError", "try { Object.freeze(new Uint8Array(1)); 'no error'; } catch (e) { e.name; }");
        check("frozen", "Object.freeze(new Uint8Array(0)); 'frozen'");
        check("true", "var e1; try { new Uint8Array(-1); } catch (e) { e1 = e; } e1 instanceof RangeError");
    }
}
