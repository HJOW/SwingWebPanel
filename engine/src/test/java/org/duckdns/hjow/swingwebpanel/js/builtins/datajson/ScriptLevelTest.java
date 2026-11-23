package org.duckdns.hjow.swingwebpanel.js.builtins.datajson;

import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Script level tests: JavaScript source evaluated by the project's own interpreter in a standard realm (<code>JsRealm.create()</code>), checking
 * Date, RegExp, JSON, Map, Set, WeakMap and WeakSet as a page would use them. ES5 style cases must pass; cases that use ES2015 syntax are skipped
 * with an explicit reason if the interpreter of the build in use cannot parse the syntax yet.
 * <p>
 * 스크립트 수준 시험: 프로젝트 자체 인터프리터가 표준 realm(<code>JsRealm.create()</code>)에서 JavaScript 소스를 평가하며, 페이지가 쓰는 방식 그대로
 * Date, RegExp, JSON, Map, Set, WeakMap, WeakSet 을 확인한다. ES5 스타일 사례는 반드시 통과해야 하고, ES2015 구문을 쓰는 사례는 사용 중인 빌드의
 * 인터프리터가 아직 그 구문을 해석하지 못하면 이유를 밝히고 건너뛴다.
 */
public class ScriptLevelTest {

    /**
     * The standard realm of the current case.
     * <p>
     * 현재 사례의 표준 realm.
     */
    private JsRealm realm;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ScriptLevelTest() {
    }

    /**
     * Creates a fresh standard realm before each case; skips with a reason if the data library is not part of it.
     * <p>
     * 사례마다 새 표준 realm 을 만든다. 데이터 라이브러리가 들어 있지 않으면 이유를 밝히고 건너뛴다.
     */
    public void setUp() {
        realm = JsRealm.create();
        String probe = js("typeof Date + typeof RegExp + typeof JSON + typeof Map + typeof WeakSet + typeof Object.keys");
        if (!probe.equals("functionfunctionobjectfunctionfunctionfunction")) {
            Check.skip("the standard realm of this run is incomplete (the data library or corelib is not installed): " + probe);
        }
    }

    /**
     * Evaluates a script and returns the result converted with ToString; a thrown JavaScript error is returned as "throws:" and the error text.
     * <p>
     * 스크립트를 평가해 결과를 ToString 한 문자열로 돌려준다. JavaScript 오류가 던져지면 "throws:" 와 오류 문자열을 돌려준다.
     *
     * @param source script text / 스크립트 문자열
     * @return the result text / 결과 문자열
     */
    private String js(String source) {
        try {
            return Ops.toString(realm.evaluateScript(source, "test"));
        } catch (JsException ex) {
            return "throws:" + ex.getMessage();
        }
    }

    /**
     * Asserts the result of a script.
     * <p>
     * 스크립트 결과를 단언한다.
     *
     * @param expected expected result text / 기대 결과 문자열
     * @param source   script / 스크립트
     */
    private void is(String expected, String source) {
        Check.equal(expected, js(source), source);
    }

    /**
     * Asserts that a script statement list throws a JavaScript error with the given name (the error is caught by the script itself).
     * <p>
     * 스크립트 문장들이 주어진 이름의 JavaScript 오류를 던지는지 단언한다(오류는 스크립트가 직접 잡는다).
     *
     * @param errorName expected error name such as "TypeError" / 기대하는 오류 이름. 예: "TypeError"
     * @param body      statements, the last one is evaluated / 문장들(마지막 문장까지 실행)
     */
    private void throwsError(String errorName, String body) {
        is(errorName, "(function () { try { " + body + "; return 'none'; } catch (e) { return e.name; } })()");
    }

    /**
     * Skips the case with a reason unless the interpreter supports the given syntax.
     * <p>
     * 인터프리터가 주어진 구문을 지원하지 않으면 이유를 밝히고 사례를 건너뛴다.
     *
     * @param probe  a script using the syntax / 그 구문을 쓰는 스크립트
     * @param syntax name of the syntax for the reason / 이유에 쓸 구문 이름
     */
    private void requireSyntax(String probe, String syntax) {
        String r = js(probe);
        if (r.startsWith("throws:")) {
            Check.skip("the interpreter of this build does not support " + syntax + " yet (" + r + ")");
        }
    }

    /**
     * Date construction, fields, rollover and arithmetic.
     * <p>
     * Date 생성, 필드, 넘침, 산술.
     */
    public void testDate() {
        is("2020", "new Date(2020, 0, 15, 10, 20, 30).getFullYear()");
        is("1970-01-01T00:00:00.000Z", "new Date(0).toISOString()");
        is("946684800000", "String(Date.UTC(2000, 0, 1))");
        is("number", "typeof Date.now()");
        is("946684800000", "String(new Date('2000-01-01T00:00:00Z').getTime())");
        is("\"1970-01-01T00:00:00.000Z\"", "JSON.stringify(new Date(0))");
        is("NaN", "String(new Date(NaN).getTime())");
        is("Invalid Date", "String(new Date(NaN))");
        is("-86400000", "String(new Date(2000, 0, 1) - new Date(2000, 0, 2))");
        is("string", "typeof (new Date(0) + 1)");
        is("1", "(new Date(0) + 1).slice(-1)");
        is("[object Date]", "Object.prototype.toString.call(new Date(0))");
        is("string", "typeof Date()");
        is("11", "String(new Date(2020, 11, 31, 23, 59, 59).getMonth())");
        is("1", "var d = new Date(2020, 0, 1); d.setDate(32); String(d.getMonth())");
        is("NaN", "String(Date.parse('garbage'))");
        is("NaN", "String(new Date(8.64e15 + 1).getTime())");
        is("true", "String(Date.prototype.toGMTString === Date.prototype.toUTCString)");
        throwsError("RangeError", "new Date(NaN).toISOString()");
        is("null", "String(new Date(NaN).toJSON())");
        is("{\"writable\":true,\"enumerable\":false,\"configurable\":true}", "JSON.stringify(Object.getOwnPropertyDescriptor(Date.prototype, 'getTime'))");
        is("7", "String(Date.length)");
    }

    /**
     * Date uses of Symbol.toPrimitive and subclassing, which need ES2015 syntax.
     * <p>
     * Symbol.toPrimitive 와 하위 클래스를 쓰는 Date 사례(ES2015 구문 필요).
     */
    public void testDateEs2015() {
        requireSyntax("class A extends Date {}; 1", "class declarations");
        is("0", "class D extends Date {}; String(new D(0).getTime())");
        is("true", "class D2 extends Date {}; String(new D2(0) instanceof Date)");
        is("0", "String(new Date(0)[Symbol.toPrimitive]('number'))");
        throwsError("TypeError", "new Date(0)[Symbol.toPrimitive]('bogus')");
    }

    /**
     * RegExp matching, replacement, flags and error reporting.
     * <p>
     * RegExp 일치, 치환, 플래그, 오류 보고.
     */
    public void testRegExp() {
        is("undefined", "String(/a(b)?c/.exec('ac')[1])");
        is("0", "String(/a(b)?c/.exec('ac').index)");
        is("a[B]c", "'aBc'.replace(/b/i, '[$&]')");
        is("1,22,333", "'a1b22c333'.match(/\\d+/g).join()");
        is("-x-", "'x'.replace(/(?:)/g, '-')");
        is("g", "/a/g.flags");
        is("gi", "new RegExp('a', 'gi').flags");
        is("true", "String(new RegExp('.', 's').test('\\n'))");
        is("false", "String(/./.test('\\n'))");
        is("1", "var r = /a/g; r.test('aa'); String(r.lastIndex)");
        is("true", "var r = /a/y; r.lastIndex = 1; String(r.test('ba'))");
        is("true", "String(/\\u{1F600}/u.test('\\uD83D\\uDE00'))");
        is("a|b|c", "'a1b2c'.split(/\\d/).join('|')");
        is("a|1|b", "'a1b'.split(/(\\d)/).join('|')");
        is("/a\\/b/g", "String(new RegExp('a/b', 'g'))");
        throwsError("SyntaxError", "new RegExp('(')");
        is("2", "String('abcabc'.search(/c/) )");
        is("a-b-c", "'a b c'.replace(/ /g, '-')");
        is("bX", "'ab'.replace(/(a)(b)/, '$2X').slice(0, 2)");
        is("[object RegExp]", "Object.prototype.toString.call(/x/)");
        is("{\"value\":0,\"writable\":true,\"enumerable\":false,\"configurable\":false}",
                "JSON.stringify(Object.getOwnPropertyDescriptor(/x/, 'lastIndex'))");
        is("2", "String(RegExp.length)");
    }

    /**
     * RegExp with ES2015 syntax: a custom Symbol.match object and arrow replacement functions.
     * <p>
     * ES2015 구문을 쓰는 RegExp 사례: 사용자 정의 Symbol.match 객체와 화살표 치환 함수.
     */
    public void testRegExpEs2015() {
        requireSyntax("var f = (x) => x; 1", "arrow functions");
        is("1", "String('abc'.match({ [Symbol.match](s) { return 1; } }))");
        is("a1b2", "'a1b2'.replace(/\\d/g, (d) => d)");
        is("A-B", "'a-b'.replace(/[a-z]/g, (c) => c.toUpperCase())");
    }

    /**
     * JSON parsing, serialization, reviver, replacers, indentation and errors.
     * <p>
     * JSON 파싱, 직렬화, reviver, replacer, 들여쓰기, 오류.
     */
    public void testJson() {
        is("{\"a\":[1,2,{\"c\":null}]}", "JSON.stringify({a: [1, 2, {b: undefined, c: null}], d: function () {}})");
        is("true", "String(JSON.parse('{\"a\":[1,2,{\"b\":true}]}').a[2].b)");
        is("true", "String(Object.prototype.hasOwnProperty.call(JSON.parse('{\"__proto__\":1}'), '__proto__'))");
        is("2,4,6", "JSON.parse('[1,2,3]', function (k, v) { return typeof v === 'number' ? v * 2 : v; }).join()");
        is("{\n  \"a\": 1,\n  \"b\": [\n    1\n  ]\n}", "JSON.stringify({a: 1, b: [1]}, null, 2)");
        is("5", "JSON.stringify({toJSON: function () { return 5; }})");
        throwsError("TypeError", "var o = {}; o.o = o; JSON.stringify(o)");
        throwsError("SyntaxError", "JSON.parse(\"{'a':1}\")");
        is("[object JSON]", "Object.prototype.toString.call(JSON)");
        is("{\"a\":1,\"c\":3}", "JSON.stringify({a: 1, b: 2, c: 3}, ['a', 'c'])");
        is("undefined", "String(JSON.stringify(undefined))");
        is("\"\\u2028\"", "JSON.stringify('\\u2028').replace('\\u2028', '\\\\u2028')");
        is("null", "JSON.stringify(NaN)");
        is("[1,null]", "JSON.stringify([1, function () {}])");
        is("2", "String(JSON.stringify.length - 1)");
    }

    /**
     * JSON with an arrow function replacer, which needs ES2015 syntax.
     * <p>
     * 화살표 함수 replacer 를 쓰는 JSON 사례(ES2015 구문 필요).
     */
    public void testJsonEs2015() {
        requireSyntax("var f = (x) => x; 1", "arrow functions");
        is("{\"a\":2}", "JSON.stringify({a: 1}, (k, v) => typeof v === 'number' ? v + 1 : v)");
        is("{\"x\":[1,2]}", "JSON.stringify({x: new Set([1, 2]).size === 2 ? [1, 2] : []})");
    }

    /**
     * Map and Set basics, ordering and iteration.
     * <p>
     * Map 과 Set 의 기본 동작, 순서, 순회.
     */
    public void testMapAndSet() {
        is("b", "var m = new Map([[1, 'a'], [2, 'b']]); m.get(2)");
        is("2", "String(new Map([[1, 'a'], [2, 'b']]).size)");
        is("1", "String(new Map().set(NaN, 1).get(NaN))");
        is("1", "String(new Set([0, -0]).size)");
        is("a,b", "var m = new Map(); m.set('a', 1); var it = m.keys(); m.set('b', 2); Array.from(it).join()");
        is("[object Map]", "Object.prototype.toString.call(new Map())");
        is("[object Map Iterator]", "Object.prototype.toString.call(new Map().entries())");
        is("[object Set Iterator]", "Object.prototype.toString.call(new Set().values())");
        throwsError("TypeError", "Map()");
        is("aa,bb", "var out = []; new Set(['a', 'b']).forEach(function (v, k, s) { out.push(v + k); }); out.join()");
        is("get size", "Object.getOwnPropertyDescriptor(Map.prototype, 'size').get.name");
        is("true", "String(Set.prototype.keys === Set.prototype.values)");
        is("true", "String(Map.prototype[Symbol.iterator] === Map.prototype.entries)");
        is("true,1", "var w = new WeakMap(); var k = {}; w.set(k, 1); w.has(k) + ',' + w.get(k)");
        throwsError("TypeError", "new WeakMap().set(1, 1)");
        throwsError("TypeError", "new WeakSet().add('x')");
        is("0,0", "String(Map.length) + ',' + String(Set.length)");
    }

    /**
     * Map and Set with ES2015 syntax: spread, for-of with destructuring and subclassing.
     * <p>
     * ES2015 구문을 쓰는 Map 과 Set 사례: 전개, 구조 분해가 있는 for-of, 하위 클래스.
     */
    public void testMapAndSetEs2015() {
        requireSyntax("for (var x of [1]) {} 1", "for-of loops");
        is("3,1,2", "[...new Set([3, 1, 3, 2])].join()");
        is("x1y2", "var s = ''; for (var [k, v] of new Map([['x', 1], ['y', 2]])) s += k + v; s");
        is("2", "class M2 extends Map { get2(k) { return this.get(k); } } var x = new M2([[1, 2]]); String(x.get2(1))");
        is("true", "class M3 extends Map {} String(new M3() instanceof Map)");
        is("a-1,b-2", "var r = []; for (var [k, v] of new Map([['a', 1], ['b', 2]])) r.push(k + '-' + v); r.join()");
        is("1,2", "var s = new Set(); s.add(1).add(2); Array.from(s).join()");
    }
}
