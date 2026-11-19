package org.duckdns.hjow.swingwebpanel.js.builtins.meta;

import java.util.ArrayList;
import java.util.List;

import org.duckdns.hjow.swingwebpanel.js.ConsoleHook;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * A case list of ES2015 language behavior that complements the library oriented tests of this package (task P14-10): each case is
 * a script with the output that ES2015 (and the web compatibility rules of Annex B that the browsers share) prescribe. The cases
 * mostly touch the border between the language core and the built-in library: well-known symbols used by operators, Annex B
 * syntax, <code>with</code> and <code>Symbol.unscopables</code>, strict mode differences, <code>eval</code> and the
 * <code>Function</code> constructor.
 * <p>
 * Cases that the interpreter does not handle yet are listed in {@link #PENDING} with the owner and the reason; they are reported but
 * do not fail the test. Every other mismatch fails it.
 * <p>
 * 이 패키지의 라이브러리 중심 시험을 보완하는 ES2015 언어 동작의 사례 목록(작업 P14-10): 각 사례는 ES2015(와 브라우저들이 공유하는 부속서 B 의
 * 웹 호환 규칙)가 정한 출력을 가진 스크립트이다. 사례는 주로 언어 핵심과 내장 라이브러리의 경계를 다룬다: 연산자가 쓰는 잘 알려진 심볼,
 * 부속서 B 구문, <code>with</code> 와 <code>Symbol.unscopables</code>, strict 모드 차이, <code>eval</code> 과 <code>Function</code> 생성자.
 * <p>
 * 인터프리터가 아직 처리하지 못하는 사례는 소유자와 이유를 붙여 {@link #PENDING} 에 적으며, 보고만 하고 시험은 실패시키지 않는다. 그 밖의 모든
 * 어긋남은 시험을 실패시킨다.
 */
public class Es2015LanguageProbeTest {

    /**
     * The cases: <code>{ id, script, expected output }</code>. The id starts with the ES2015 clause number. The output is the printed
     * lines joined with a vertical bar.
     * <p>
     * 사례: <code>{ id, 스크립트, 기대 출력 }</code>. id 는 ES2015 절 번호로 시작한다. 출력은 출력한 줄을 세로 막대로 이은 것이다.
     */
    private static final String[][] CASES = {
        // ---- Annex B.1 syntax
        { "B.1.1/legacy-octal-literal", "print(010 + 0o10 + 08);", "24" },
        { "B.1.2/octal-escape", "print('\\101' === 'A'); print('\\0' === String.fromCharCode(0)); print('\\8');", "true|true|8" },
        { "B.1.3/html-like-comments", "print(1); <!-- hidden\nprint(2);\n--> hidden too\nprint(3);", "1|2|3" },
        { "B.1.4/regexp-extended-atoms", "print(/a{/.test('a{')); print(/\\8/.test('8')); print(/]/.test(']')); print(/[\\b]/.test(String.fromCharCode(8)));",
                "true|true|true|true" },

        // ---- Annex B.3 other web compatibility rules
        { "B.3.1/proto-in-object-literal", "var p={a:1}; var o={__proto__:p}; print(o.a); print(Object.getPrototypeOf(o)===p); var q={'__proto__':p}; print(q.a);"
                + " var r={['__proto__']:p}; print(r.a); print(Object.getPrototypeOf(r)===Object.prototype);", "1|true|1|undefined|true" },
        { "B.3.2/labelled-function", "lbl: function lf(){ return 1; } print(typeof lf);", "function" },
        { "B.3.3/function-in-block", "print(typeof fb); { function fb(){ return 1; } } print(typeof fb);", "undefined|function" },
        { "B.3.4/function-in-if", "if (true) function fi(){ return 5; } print(fi());", "5" },
        { "B.3.5/var-in-catch", "try { throw 1; } catch (e) { var e = 2; } print(e);", "undefined" },
        { "B.3/arguments-callee", "print((function(){ return typeof arguments.callee; })());", "function" },
        { "B.3/sloppy-this-and-implicit-global", "var g=(function(){ return this; })(); print(typeof g); (function(){ implicit1 = 5; })(); print(implicit1);"
                + " print((function(){ 'use strict'; return this; })());", "object|5|undefined" },
        { "B.3/strict-implicit-global-throws", "try { (function(){ 'use strict'; implicit2 = 5; })(); print('no'); } catch (e) { print(e.name); }", "ReferenceError" },

        // ---- 12.10 with and Symbol.unscopables (22.1.3.31)
        { "12.10/with-statement", "var o={a:1}; with (o) { a=2; var b=3; } print(o.a+','+b);", "2,3" },
        { "22.1.3.31/array-unscopables", "var arr=[]; with (arr) { print(typeof keys); print(typeof push); }", "undefined|function" },
        { "12.10/custom-unscopables", "var o={x:1,[Symbol.unscopables]:{x:true}}; var x='outer'; with (o) { print(x); }", "outer" },

        // ---- 7.1 and 12 operators that use well-known symbols and conversions
        { "7.1.1/symbol-toPrimitive", "var o={[Symbol.toPrimitive](h){ return h==='number'?42:'str'; }}; print(+o); print(`${o}`); print(o+'');", "42|str|str" },
        { "12.9.4/symbol-hasInstance", "class Even { static [Symbol.hasInstance](n){ return n%2===0; } } print(2 instanceof Even); print(3 instanceof Even);", "true|false" },
        { "22.1.3.1/isConcatSpreadable", "var s={length:2,0:'a',1:'b',[Symbol.isConcatSpreadable]:true}; print([1].concat(s).join());", "1,a,b" },
        { "19.1.3.6/toStringTag", "print(Object.prototype.toString.call({[Symbol.toStringTag]:'X'})); print(String(Symbol('d'))); print(typeof Symbol.iterator);",
                "[object X]|Symbol(d)|symbol" },
        { "12.1/typeof-symbol-and-conversions", "var s=Symbol('q'); print(typeof s); try { s+''; } catch (e) { print(e.name); } try { +s; } catch (e) { print(e.name); }",
                "symbol|TypeError|TypeError" },

        // ---- 12.2 and 13 core syntax shared with the library
        { "12.2.9/tagged-template-raw", "function tag(s){ return s.raw.join('|') + s.length; } print(tag`a\\n${1}b`);", "a\\n|b2" },
        { "11.8.6/unicode-escapes", "print('\\u{1F600}'.length); print('\\u{61}'); print(0b101+0o17+0xff);", "2|a|275" },
        { "12.2.6/computed-shorthand-accessor", "var k='z'; var o={[k+1]:1, k, m(){ return 2; }, get g(){ return 3; }}; print(o.z1+','+o.k+','+o.m()+','+o.g);", "1,z,2,3" },
        { "13.3.3/destructuring", "var [x,,y=3,...z]=[1,2,undefined,4,5]; print(x+','+y+','+z); var {p, q:{r}}={p:1,q:{r:2}}; print(p+r);", "1,3,4,5|3" },
        { "14.1/default-rest-spread", "function f(a,b=a+1,...r){ return [a,b,r.length].join(); } print(f(1)); print(f(1,5,6,7)); print(Math.max(...[1,5,3], 4));", "1,2,0|1,5,2|5" },
        { "14.2/arrow-lexical-this", "function F(){ this.v=1; this.f=()=>this.v; } print(new F().f());", "1" },
        { "14.5/classes", "class A { constructor(x){ this.x=x; } get d(){ return this.x*2; } static s(){ return 's'; } m(){ return 'A'; } }"
                + " class B extends A { m(){ return super.m()+'B'; } } var b=new B(4); print(b.d+','+B.s()+','+b.m()+','+(b instanceof A)+','+typeof A);", "8,s,AB,true,function" },
        { "14.5/class-call-and-tdz", "class A {} try { A(); } catch (e) { print(e.name); } try { new C(); } catch (e) { print(e.name); } class C {}", "TypeError|ReferenceError" },
        { "14.5/extends-builtins", "class MyArr extends Array {} var a=new MyArr(); a.push(1,2); print(a.length); print(a instanceof MyArr); print(Array.isArray(a));"
                + " print(a.map(function(x){ return x; }) instanceof MyArr);", "2|true|true|true" },
        { "14.5/extends-error", "class MyErr extends Error {} var e=new MyErr('m'); print(e.message); print(e instanceof Error); print(Object.prototype.toString.call(e));"
                + " print(e.name);", "m|true|[object Error]|Error" },
        { "14.4/generators", "function* g(){ var x=yield 1; try { yield x*2; } finally { print('fin'); } } var it=g(); print(it.next().value); print(it.next(5).value);"
                + " print(it.return(9).value); print(it.next().done);", "1|10|fin|9|true" },
        { "13.7.5/for-of-closes-iterator", "var closed=0; var it={[Symbol.iterator](){ return {next(){ return {value:1,done:false}; }, return(){ closed++; return {}; }}; }};"
                + " for (var v of it) { break; } print(closed);", "1" },
        { "13.7.5/for-of-map-destructuring", "for (var [k,v] of new Map([[1,2]])) { print(k+v); } print([...new Set([3,3,4])].join());", "3|3,4" },
        { "14.4/user-iterable-spread", "class R { *[Symbol.iterator](){ yield 1; yield 2; } } print([...new R()].join()); print(Array.from(new R()).length);", "1,2|2" },
        { "13.7.4/let-per-iteration", "var fs=[]; for (let i=0;i<3;i++) { fs.push(()=>i); } print(fs.map(f=>f()).join());", "0,1,2" },
        { "13.3.1/tdz-and-const", "(function(){ try { x1; } catch (e) { print(e.name); } let x1=1; })(); const c1=1; try { c1=2; } catch (e) { print(e.name); }", "ReferenceError|TypeError" },
        { "12.3.8/new-target", "function NT(){ this.w = new.target===NT; } print(new NT().w); print((function(){ return new.target; })());", "true|undefined" },

        // ---- 18.2.1 eval, 19.2.1 Function constructor, strict mode (P13-11)
        { "18.2.1/eval-var-scoping", "function e1(){ eval('var q=1'); return q; } print(e1()); function e2(){ 'use strict'; eval('var q2=1'); return typeof q2; } print(e2());", "1|undefined" },
        { "18.2.1/eval-strict-octal", "try { eval(\"'use strict'; 010\"); print('no'); } catch (e) { print(e.name); }", "SyntaxError" },
        { "18.2.1/indirect-eval-global", "var gv='global'; function ie(){ var gv='local'; return (0,eval)('gv'); } print(ie());", "global" },
        { "19.2.1/function-constructor", "print(new Function('a','b','return a+b')(1,2)); print(Function('return typeof this')()); print(Function('a, b', 'return a')(7));", "3|object|7" },

        // ---- 25.4 Promise with language features
        { "25.4/promise-and-arrow-chain", "Promise.resolve(2).then(v => v * 3).then(v => { print(v); });", "6" },

        // ---- 6.1.7.3 and 9 objects with the library
        { "9.4.2/array-length-exotic", "var a=[1,2,3]; a.length=1; print(a.join()); a[5]=6; print(a.length); print(Object.keys(a).join());", "1|6|0,5" },
        { "9.1/own-keys-order", "var o={b:1,2:1,a:1,1:1}; o[Symbol('s')]=1; print(Object.getOwnPropertyNames(o).join());", "1,2,b,a" },

        // ---- 26.2 Proxy objects used by other library functions and by the language
        { "26.2/proxy-concat-and-assign", "print([].concat(new Proxy([1,2],{})).length); print(Object.keys(Object.assign({}, new Proxy({a:1,b:2},{}))).join());", "2|a,b" },
        { "26.2/proxy-keys-filtered-by-descriptor", "var p=new Proxy({a:1},{ownKeys:function(){ return ['a','b']; },"
                + " getOwnPropertyDescriptor:function(t,k){ return k==='a'?{value:1,enumerable:true,configurable:true}:undefined; }}); print(Object.keys(p).join());", "a" },
        { "26.2/proxy-json-and-ownKeys", "print(JSON.stringify(new Proxy({a:1,b:[1]},{}))); print(Reflect.ownKeys(new Proxy([1,2],{})).join());", "{\"a\":1,\"b\":[1]}|0,1,length" },
        { "26.2/proxy-freeze", "var p=new Proxy({a:1},{}); Object.freeze(p); print(Object.isFrozen(p)); print(Object.isExtensible(p));", "true|false" },
        { "26.2/proxy-nested-and-spread", "print(new Proxy(new Proxy({a:1},{}),{}).a); print([...new Proxy([1,2,3],{})].join());", "1|1,2,3" },
        { "26.2/proxy-revoked-isArray", "var r=Proxy.revocable([],{}); r.revoke(); try { Array.isArray(r.proxy); print('no'); } catch (e) { print(e.name); }", "TypeError" },
        { "26.2/proxy-with-has-trap", "var o=new Proxy({},{has:function(t,k){ return k==='magic'; }, get:function(t,k){ return k==='magic'?42:undefined; }});"
                + " with (o) { print(magic); }", "42" },
        { "26.2/proxy-function-instanceof-bind", "var f=new Proxy(function(){},{}); print(f instanceof Function); print(typeof f.bind); var g=f.bind(null); print(typeof g);",
                "true|function|function" },

        // ---- 25.4 Promise with other library iterables
        { "25.4/promise-combinators-over-iterables", "Promise.all(new Set([Promise.resolve(1),2])).then(function(v){ print(v.join()); });"
                + " Promise.race(new Map([[1,2]]).values()).then(print);", "1,2|2" },
        { "25.4/thenable-then-read-once", "var count=0; var th={ get then(){ count++; return function(res){ res(1); }; } };"
                + " Promise.resolve(th).then(function(v){ print(v+','+count); });", "1,1" },

        // ---- 25.3 generators, iteration protocol and destructuring
        { "25.3/generator-return-runs-finally", "function* g(){ try { yield 1; yield 2; } finally { print('cleanup'); } } var it=g(); it.next();"
                + " print(JSON.stringify(it.return(7))); print(JSON.stringify(it.next()));", "cleanup|{\"value\":7,\"done\":true}|{\"done\":true}" },
        { "14.4/yield-star-return-propagates", "function* inner(){ try { yield 'a'; yield 'b'; } finally { print('inner-fin'); } }"
                + " function* outer(){ try { yield* inner(); } finally { print('outer-fin'); } } var o=outer(); o.next(); print(JSON.stringify(o.return(5)));",
                "inner-fin|outer-fin|{\"value\":5,\"done\":true}" },
        { "25.3/generator-throw-before-start", "function* g(){ yield 1; } var it=g(); try { it.throw(new RangeError('x')); } catch (e) { print(e.name); }"
                + " print(JSON.stringify(it.next()));", "RangeError|{\"done\":true}" },
        { "13.3.3/array-destructuring-closes-iterator", "var closed=0; var iter={[Symbol.iterator](){ return {next(){ return {value:1,done:false}; },"
                + " return(){ closed++; return {}; }}; }}; var [a1]=iter; print(a1+','+closed);", "1,1" },
        { "13.7/let-closures-for-of-for-in", "var f1=[]; for (let k of [1,2,3]) f1.push(()=>k); print(f1.map(f=>f()).join());"
                + " var f2=[]; for (let k in {a:1,b:2}) f2.push(()=>k); print(f2.map(f=>f()).join());", "1,2,3|a,b" },
        { "13.12/switch-case-tdz", "switch (1) { case 0: let z=1; break; case 1: try { z; } catch (e) { print(e.name); } }", "ReferenceError" },

        // ---- 14 functions and classes
        { "14.1/default-parameter-scope", "function f(a, b = () => a){ var a = 2; return b(); } print(f(1));", "1" },
        { "14.3/function-name-inference", "var f=()=>1; var o={m(){}, get x(){ return 1; }, ['c'+'k']: function(){}}; class K { static s(){} }"
                + " print(f.name+','+o.m.name+','+o.ck.name+','+K.name+','+K.s.name+','+Object.getOwnPropertyDescriptor(o,'x').get.name);", "f,m,ck,K,s,get x" },
        { "14.5/static-inheritance-and-super", "class A { static who(){ return 'A'; } hi(){ return 'hi-A'; } } class B extends A { hi(){ return super.hi()+'!'; } }"
                + " print(Object.getPrototypeOf(B)===A); print(B.who()); print(new B().hi());", "true|A|hi-A!" },
        { "12.3.5/super-in-object-literal", "var base={ m(){ return 'base'; } }; var d={ __proto__: base, m(){ return super.m()+'+d'; } }; print(d.m());", "base+d" },
        { "14.5/extends-null", "class N extends null {} print(Object.getPrototypeOf(N.prototype));", "null" },
        { "14.5/extends-proxy-of-class", "class PA {} class PB extends new Proxy(PA, {}) {} print(new PB() instanceof PA);", "true" },
        { "14.6/proper-tail-calls", "function f(n){ 'use strict'; return n===0?0:f(n-1); } print(f(20000));", "0" },

        // ---- 9 objects, 19.2.4 Function.prototype, 20 and 21 library values seen from scripts
        { "9.4.2/array-index-and-length", "var a=[]; a[4294967295]=1; print(a.length); try { a.length=-1; } catch (e) { print(e.name); }", "0|RangeError" },
        { "9.4.2/array-holes", "print([,1].hasOwnProperty(0)+','+[,1].length+','+(0 in [,1])+','+[1,,3].map(x=>x*2).join('-')+','+Object.keys([1,,3]).join());",
                "false,2,false,2--6,0,2" },
        { "9.1/set-prototype-cycle", "var o1={}; var o2=Object.create(o1); try { Object.setPrototypeOf(o1,o2); } catch (e) { print(e.name); }", "TypeError" },
        { "19.1.3.6/builtin-tags", "print(Object.prototype.toString.call(new Map)+Object.prototype.toString.call(new Set)+Object.prototype.toString.call(Symbol())"
                + "+Object.prototype.toString.call(function*(){})+Object.prototype.toString.call(new Promise(function(){})));",
                "[object Map][object Set][object Symbol][object GeneratorFunction][object Promise]" },
        { "19.2.4/function-prototype-shape", "print(Array.isArray(Array.prototype)+','+typeof Function.prototype+','+Function.prototype.name+'|'+Function.prototype.length"
                + "+','+Object.prototype.__proto__);", "true,function,|0,null" },
        { "19.2.4/function-caller-arguments", "print(Function.prototype.hasOwnProperty('caller')); print(Function.prototype.hasOwnProperty('arguments'));", "true|true" },
        { "20.3/date-set-month-rollover", "var d=new Date(2016,0,31); d.setMonth(1); print(d.getMonth()+','+d.getDate());", "2,2" },
        { "7.1.3.1/string-to-number-forms", "print(Number('0b11')+','+Number('0o17')+','+Number(' 12 ')+','+Number('1e3')+','+Number('0x1F')+','+Number('')+','+Number('1_0'));",
                "3,15,12,1000,31,0,NaN" },
        { "21.2/regexp-unicode-and-sticky", "print(/\\u{1F600}/u.test(String.fromCodePoint(0x1F600))); print(String.fromCodePoint(0x1F600).match(/^.$/u) !== null);"
                + " var re=/a/y; re.lastIndex=1; print(re.test('ba'));", "true|true|true" },
        { "12.2.9/template-object-identity", "function t(s){ return s; } var fn=()=>t`x`; print(fn()===fn());", "true" },
    };

    /**
     * Cases that are known not to work yet: <code>{ id prefix, owner, reason }</code>. Delete a row as soon as its cases pass.
     * <p>
     * 아직 동작하지 않는 것으로 알려진 사례: <code>{ id 앞부분, 소유자, 이유 }</code>. 사례가 통과하면 바로 그 행을 지운다.
     */
    private static final String[][] PENDING = {
    };

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public Es2015LanguageProbeTest() {
    }

    /**
     * Runs a script in a fresh standard realm, drains the promise jobs and returns the printed lines joined with a vertical bar.
     * <p>
     * 새 표준 realm 에서 스크립트를 실행하고 promise 작업을 모두 처리한 뒤 출력한 줄을 세로 막대로 이어 돌려준다.
     *
     * @param source the script / 스크립트
     * @return the printed lines / 출력한 줄
     */
    private static String run(String source) {
        final List<String> lines = new ArrayList<String>();
        JsRealm realm = JsRealm.create();
        realm.setConsole(new ConsoleHook() {
            public void message(String level, String text) {
                lines.add(text);
            }
        });
        realm.evaluateScript(source, "es2015-case.js");
        realm.runJobs();
        StringBuilder sb = new StringBuilder();
        // 출력한 줄을 세로 막대로 잇는다.
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                sb.append('|');
            }
            sb.append(lines.get(i));
        }
        return sb.toString();
    }

    /**
     * Returns the index of the pending row that covers a case id, or -1.
     * <p>
     * 사례 id 를 포함하는 보류 행의 번호를 돌려준다. 없으면 -1.
     *
     * @param id the case id / 사례 id
     * @return the row index or -1 / 행 번호 또는 -1
     */
    private static int pendingRow(String id) {
        // 보류 목록의 행을 차례로 비교한다.
        for (int i = 0; i < PENDING.length; i++) {
            if (id.startsWith(PENDING[i][0])) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Case ids are unique and the table rows are complete.
     * <p>
     * 사례 id 가 유일하고 표의 행이 완전하다.
     */
    public void testCaseListIsWellFormed() {
        List<String> seen = new ArrayList<String>();
        // 모든 사례의 형식과 id 중복을 검사한다.
        for (int i = 0; i < CASES.length; i++) {
            Check.equal(3, CASES[i].length, "case row " + i + " has id, script and expected output");
            Check.isFalse(seen.contains(CASES[i][0]), "case id is unique: " + CASES[i][0]);
            seen.add(CASES[i][0]);
        }
        // 보류 행의 모든 칸이 채워져 있는지 확인한다.
        for (int i = 0; i < PENDING.length; i++) {
            Check.equal(3, PENDING[i].length, "pending row " + i + " has prefix, owner and reason");
        }
    }

    /**
     * Runs every case. Mismatches that are not on the pending list fail the case; the others and the totals are printed.
     * <p>
     * 모든 사례를 실행한다. 보류 목록에 없는 어긋남은 사례를 실패시키며, 그 밖의 것과 합계는 출력한다.
     */
    public void testAllCases() {
        List<String> unexpected = new ArrayList<String>();
        int[] hits = new int[PENDING.length];
        int pass = 0;
        int pending = 0;
        // 모든 사례를 새 realm 에서 실행하고 기대 출력과 비교한다.
        for (int i = 0; i < CASES.length; i++) {
            String id = CASES[i][0];
            String actual;
            try {
                actual = run(CASES[i][1]);
            } catch (JsException e) {
                actual = "uncaught: " + e.getMessage();
            } catch (RuntimeException e) {
                actual = "java: " + e;
            }
            if (CASES[i][2].equals(actual)) {
                pass++;
                continue;
            }
            int row = pendingRow(id);
            if (row >= 0) {
                hits[row]++;
                pending++;
            } else {
                unexpected.add(id + ": expected [" + CASES[i][2] + "] but got [" + actual + "]");
            }
        }
        System.out.println("ES2015 language probe: " + CASES.length + " cases, " + pass + " pass, " + pending + " pending (allow-listed), "
                + unexpected.size() + " unexpected");
        // 보류 행마다 해당 사례 수와 이유를 출력하고 낡은 행을 표시한다.
        for (int i = 0; i < PENDING.length; i++) {
            if (hits[i] == 0) {
                System.out.println("  [stale pending row] " + PENDING[i][0] + " (" + PENDING[i][1] + "): no longer needed");
            } else {
                System.out.println("  [pending] " + PENDING[i][0] + " x" + hits[i] + " (" + PENDING[i][1] + "): " + PENDING[i][2]);
            }
        }
        if (!unexpected.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append(unexpected.size()).append(" case(s) differ from ES2015 and are not on the pending list:");
            // 어긋난 사례를 한 줄씩 메시지에 싣는다.
            for (int i = 0; i < unexpected.size(); i++) {
                sb.append("\n  ").append(unexpected.get(i));
            }
            Check.fail(sb.toString());
        }
    }
}
