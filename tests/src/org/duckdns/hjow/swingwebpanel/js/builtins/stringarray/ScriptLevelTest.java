package org.duckdns.hjow.swingwebpanel.js.builtins.stringarray;

import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Script level tests: JavaScript source evaluated by the project's own interpreter in a standard realm (<code>JsRealm.create()</code>), checking
 * the String and Array built-ins as a page would use them. The ES5 style cases must pass; the ES2015 syntax cases are skipped with an explicit reason
 * if the interpreter of the build in use cannot parse the syntax yet.
 * <p>
 * 스크립트 수준 시험: 프로젝트 자체 인터프리터가 표준 realm(<code>JsRealm.create()</code>)에서 JavaScript 소스를 평가하며, 페이지가 쓰는 방식 그대로
 * String 과 Array 내장 객체를 확인한다. ES5 스타일 사례는 반드시 통과해야 하고, ES2015 구문 사례는 사용 중인 빌드의 인터프리터가 아직 그 구문을
 * 해석하지 못하면 이유를 밝히고 건너뛴다.
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
     * Creates a fresh standard realm before each case.
     * <p>
     * 사례마다 새 표준 realm 을 만든다.
     */
    public void setUp() {
        realm = JsRealm.create();
        if (!js("typeof Object.keys + typeof Symbol + typeof Object.prototype.toString").equals("functionfunctionfunction")) {
            Check.skip("the standard realm of this run is incomplete (the corelib installer is not compiled or not installed): " + js("typeof Object.keys"));
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
     * In the standard realm (all built-in groups installed) String.prototype and Array.prototype still have exactly the documented property
     * names: no other group adds or leaves behind unexpected properties.
     * <p>
     * 표준 realm(모든 내장 묶음이 설치됨)에서도 String.prototype 과 Array.prototype 은 문서화한 프로퍼티 이름만 정확히 가진다. 다른 묶음이 예상 밖
     * 프로퍼티를 더하거나 남기지 않는다.
     */
    public void testStandardRealmKeepsTheDocumentedPropertySets() {
        java.util.List<String> string = new java.util.ArrayList<String>(java.util.Arrays.asList("constructor", "length", "trimLeft", "trimRight"));
        java.util.List<String> array = new java.util.ArrayList<String>(java.util.Arrays.asList("constructor", "length"));
        String[][][] stringTables = { PropertyTableTest.STRING_PROTOTYPE, PropertyTableTest.STRING_ANNEX_B, PropertyTableTest.STRING_EXTENSIONS };
        String[][][] arrayTables = { PropertyTableTest.ARRAY_PROTOTYPE, PropertyTableTest.ARRAY_EXTENSIONS };
        // 표의 이름을 모아 정렬한 목록을 만든다.
        for (int t = 0; t < stringTables.length; t++) {
            for (int i = 0; i < stringTables[t].length; i++) {
                string.add(stringTables[t][i][0]);
            }
        }
        // 배열 표를 차례로 훑는다.
        for (int t = 0; t < arrayTables.length; t++) {
            // 배열 표의 각 이름을 모은다.
            for (int i = 0; i < arrayTables[t].length; i++) {
                array.add(arrayTables[t][i][0]);
            }
        }
        java.util.Collections.sort(string);
        java.util.Collections.sort(array);
        is(join(string), "Object.getOwnPropertyNames(String.prototype).sort().join()");
        is(join(array), "Object.getOwnPropertyNames(Array.prototype).sort().join()");
    }

    /**
     * Joins strings with commas.
     * <p>
     * 문자열을 쉼표로 잇는다.
     *
     * @param list the strings / 문자열들
     * @return the joined text / 이은 문자열
     */
    private static String join(java.util.List<String> list) {
        StringBuilder sb = new StringBuilder();
        // 목록의 문자열을 쉼표로 구분해 잇는다.
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    /**
     * Basic String methods as pages call them.
     * <p>
     * 페이지가 호출하는 방식의 기본 String 메서드.
     */
    public void testStringBasics() {
        is("b", "'abc'.charAt(1)");
        is("98", "'abc'.charCodeAt(1)");
        is("a+b+c", "'a-b-c'.split('-').join('+')");
        is("bc", "'abc'.slice(-2)");
        is("x", "'  x\\n'.trim()");
        is("2", "'abc'.indexOf('c')");
        is("abc1", "'ab'.concat('c', 1)");
        is("x", "'%s'.replace('%s', 'x')");
        is("aXXbX", "'aXbX'.replace('X', '$&$&')");
        is("aB13c", "'abc'.replace('b', function (m, p, s) { return m.toUpperCase() + p + s.length; })");
        is("xxx", "'x'.repeat(3)");
        is("b", "'abc'.substr(-2, 1)");
        is("<a name=\"n\">abc</a>", "'abc'.anchor('n')");
        is("Hi", "String.fromCharCode(72, 105)");
        is("123", "String(123)");
        is("object", "typeof new String('a')");
        is("string", "typeof String('a')");
        is("2", "new String('ab').length");
        is("3", "'abc'.length");
        is("-1", "'ab'.localeCompare('ac')");
        is("0,1", "Object.keys(new String('ab')).join()");
        is("ABC", "'abc'.toUpperCase()");
        is("true", "'abc'.startsWith('ab') && 'abc'.endsWith('bc') && 'abc'.includes('b')");
        is("a,b", "'a1b'.split(1).join()");
    }

    /**
     * Unicode: surrogate pairs, code points, normalization and case mapping from script source.
     * <p>
     * 유니코드: 서로게이트 쌍, 코드 포인트, 정규화, 대소문자 변환(스크립트 소스에서).
     */
    public void testStringUnicode() {
        is("1f600", "'\\uD83D\\uDE00'.codePointAt(0).toString(16)");
        is("2", "String.fromCodePoint(0x1F600).length");
        is("4", "'a\\uD83D\\uDE00b'.split('').length");
        is("2", "'\\u00e9'.normalize('NFD').length");
        is("1", "'e\\u0301'.normalize().length");
        is("SS", "'\\u00df'.toUpperCase()");
        is("3", "'가나다'.length");
        is("나", "'가나다'.charAt(1)");
        is("가나다", "'가나다'.toLowerCase()");
        is("true", "'\\u00a0\\ufeffx'.trim() === 'x'");
        is("false", "'\\u200bx'.trim() === 'x'");
    }

    /**
     * Errors thrown by String methods are JavaScript errors of the right type that scripts can catch.
     * <p>
     * String 메서드가 던지는 오류는 스크립트가 잡을 수 있는 올바른 종류의 JavaScript 오류이다.
     */
    public void testStringErrors() {
        is("RangeError", "try { 'x'.repeat(-1); } catch (e) { e.name; }");
        is("RangeError", "try { String.fromCodePoint(-1); } catch (e) { e.name; }");
        is("RangeError", "try { 'x'.normalize('bad'); } catch (e) { e.name; }");
        is("TypeError", "try { String.prototype.trim.call(null); } catch (e) { e.name; }");
        is("TypeError", "try { String.prototype.toString.call(5); } catch (e) { e.name; }");
        is("TypeError", "try { new String(Symbol('s')); } catch (e) { e.name; }");
        is("Symbol(s)", "String(Symbol('s'))");
    }

    /**
     * String methods with regular expressions (the RegExp built-in must be installed in the standard realm).
     * <p>
     * 정규식을 쓰는 String 메서드(표준 realm 에 RegExp 내장 객체가 설치되어 있어야 한다).
     */
    public void testStringWithRegExp() {
        if (js("typeof RegExp").equals("undefined")) {
            Check.skip("the standard realm of this build has no RegExp built-in yet");
        }
        is("a-b-c", "'a1b2c'.replace(/\\d/g, '-')");
        is("1,2", "'a1b2c'.match(/\\d/g).join()");
        is("1", "'a1b'.search(/\\d/)");
        is("a,b,c", "'a1b2c'.split(/\\d/).join()");
        is("b-a", "'a b'.replace(/(\\w) (\\w)/, '$2-$1')");
        is("1", "'xay'.match('a').index");
        is("true", "'abc'.match(/b/) !== null");
        is("TypeError", "try { 'abc'.startsWith(/a/); } catch (e) { e.name; }");
    }

    /**
     * Basic Array methods as pages call them.
     * <p>
     * 페이지가 호출하는 방식의 기본 Array 메서드.
     */
    public void testArrayBasics() {
        is("1,2,3", "[3, 1, 2].sort().join()");
        is("1,10,9", "[10, 9, 1].sort().join()");
        is("1,9,10", "[10, 9, 1].sort(function (a, b) { return a - b; }).join()");
        is("3,2,1", "[1, 2, 3].reverse().join()");
        is("1", "[1, 2, 3].indexOf(2)");
        is("2,3", "[1, 2, 3].slice(1).join()");
        is("1,x,4", "var a = [1, 2, 3, 4]; a.splice(1, 2, 'x'); a.join()");
        is("3", "[].concat([1], [2, [3]]).length");
        is("6", "[1, 2, 3].reduce(function (a, b) { return a + b; })");
        is("1,2,3", "[1, [2, [3]]].toString()");
        is("--", "Array(3).join('-')");
        is("true", "Array.isArray([])");
        is("false", "Array.isArray({ length: 0 })");
        is("a,b", "Array.prototype.slice.call({ 0: 'a', 1: 'b', length: 2 }).join()");
        is("2", "var n = 0; [1, , 3].forEach(function () { n++; }); n");
        is("1,2,3", "[1, 2, 3].map(String).join()");
        is("2", "[1, 2, 3].filter(function (x) { return x > 1; }).length");
        is("0,2,4", "Array.from({ length: 3 }, function (v, i) { return i * 2; }).join()");
        is("7,8", "Array.of(7, 8).join()");
        is("0,0,0", "[1, 2, 3].fill(0).join()");
        is("4,5,3,4,5", "[1, 2, 3, 4, 5].copyWithin(0, 3).join()");
        is("true", "[NaN].includes(NaN)");
        is("-1", "[NaN].indexOf(NaN)");
        is("3", "[1, 2, 3].find(function (x) { return x > 2; })");
        is("-1", "[1, 2, 3].findIndex(function (x) { return x > 5; })");
        is("1", "Array.length");
        is("1", "Array.prototype.push.length");
        is("4294967295", "var a = []; a[4294967294] = 1; a.length");
        is("4294967295", "var a = []; a[4294967294] = 1; a[4294967295] = 2; a.length");
        is("1,2,3", "[1, 2, 3].join(undefined)");
        is("4", "var a = [1, 2]; a.push(3, 4)");
        is("1,2", "var a = [1, 2, 3]; a.length = 2; a.join()");
    }

    /**
     * Iteration objects: entries, keys, values, the toString tags and the [Symbol.iterator] identity.
     * <p>
     * 이터레이션 객체: entries, keys, values, toString 태그, [Symbol.iterator] 동일성.
     */
    public void testIterationObjects() {
        is("0,1", "[1, 2, 3].entries().next().value.join()");
        is("0,1,2", "var r = [], it = [7, 8, 9].keys(), s; while (!(s = it.next()).done) r.push(s.value); r.join()");
        is("[object Array Iterator]", "Object.prototype.toString.call([].values())");
        is("[object String Iterator]", "Object.prototype.toString.call(''[Symbol.iterator]())");
        is("true", "Array.prototype[Symbol.iterator] === Array.prototype.values");
        is("a", "''.concat('abc')[Symbol.iterator]().next().value");
        is("true", "Object.getPrototypeOf(Object.getPrototypeOf([].values())) === Object.getPrototypeOf(Object.getPrototypeOf(''[Symbol.iterator]()))");
    }

    /**
     * Sorting as a script observes it: stability, comparator errors, undefined and holes.
     * <p>
     * 스크립트가 관찰하는 정렬: 안정성, 비교 함수 오류, undefined 와 구멍.
     */
    public void testSortObservations() {
        is("a1,a3,b0,b2", "var x = [{k:'b',i:0},{k:'a',i:1},{k:'b',i:2},{k:'a',i:3}]; x.sort(function (p, q) { return p.k < q.k ? -1 : p.k > q.k ? 1 : 0; }); "
                + "x.map(function (o) { return o.k + o.i; }).join()");
        is("TypeError", "try { [1, 2].sort('x'); } catch (e) { e.name; }");
        is("3,1,2", "var a = [3, 1, 2]; try { a.sort(function () { throw new Error('stop'); }); } catch (e) {} a.join()");
        is("a,b,,", "var a = ['b', undefined, 'a', , ]; a.sort(); a.join()");
        is("true", "var a = [3, undefined, 1]; a.sort(); a[2] === undefined && a.length === 3");
    }

    /**
     * Species and subclassing seen from scripts (constructor functions).
     * <p>
     * 스크립트에서 보는 species 와 하위 클래스(생성자 함수).
     */
    public void testSpeciesFromScript() {
        is("3", "var a = [1, 2, 3]; a.constructor = {}; a.constructor[Symbol.species] = function (n) { this.made = n; }; "
                + "var r = a.map(function (x) { return x; }); r.made");
        is("true", "Array[Symbol.species] === Array");
        is("true", "var a = [1]; a.constructor = undefined; Array.isArray(a.slice())");
    }

    /**
     * ES2015 syntax in combination with the built-ins: spread, for-of, destructuring, arrows, classes, templates.
     * <p>
     * 내장 객체와 함께 쓰는 ES2015 구문: spread, for-of, 구조 분해, 화살표 함수, 클래스, 템플릿.
     */
    public void testEs2015Syntax() {
        requireSyntax("(function () { var f = x => x; return f(1); })()", "arrow functions");
        is("3", "[...'a" + new String(Character.toChars(0x1F600)) + "b'].length");
        is("a,b", "[...'ab'].join()");
        is("2", "var n = 0; for (var c of 'a" + new String(Character.toChars(0x1F600)) + "') n++; n");
        is("2,4,6", "[1, 2, 3].map(x => x * 2).join()");
        is("3", "var [p, q] = [1, 2]; p + q");
        is("3", "(function () { return arguments.length; })(...[1, 5, 2])");
        is("a\\n1b", "String.raw`a\\n${1}b`");
        is("2", "[1, 2, 3].find(x => x > 1)");
        is("0,1,2", "Array.from({ length: 3 }, (v, i) => i).join()");
        is("x1", "var o = { m() { return 'x'; } }; o.m() + [1].join()");
        is("6", "var s = 0; for (var v of [1, 2, 3]) s += v; s");        is("3", "[...[1, 2], 3].length");        is("4", "var [a1, , b1] = [1, 2, 3]; a1 + b1");        is("3", "(function (...r) { return r.length; })(1, 2, 3)");        is("a,b", "var m = []; for (var [i, ch] of ['a', 'b'].entries()) m.push(ch); m.join()");        is("0,1", "[...[7, 8].keys()].join()");        is("x,y", "var o = {}; o[Symbol.iterator] = Array.prototype[Symbol.iterator]; o.length = 2; o[0] = 'x'; o[1] = 'y'; Array.from(o).join()");
    }

    /**
     * Subclassing Array with a class (species, new.target, length behavior).
     * <p>
     * 클래스로 Array 를 상속한다(species, new.target, length 동작).
     */
    public void testClassExtendsArray() {
        requireSyntax("(function () { class A {} return typeof A; })()", "classes");
        is("true", "(function () { class A extends Array {} var a = new A(); a.push(1, 2); return a.map(x => x) instanceof A; })()");
        is("true", "(function () { class B extends Array {} return Array.isArray(new B()); })()");
        is("2", "(function () { class C extends Array {} var a = new C(); a.push(1, 2); return a.length; })()");
        is("true", "(function () { class D extends Array {} return D.from([1, 2]) instanceof D; })()");
        is("true", "(function () { class E extends Array {} return E.of(1) instanceof E; })()");
    }
}
