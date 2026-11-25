package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.js.ErrorKind;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Verifies Annex B behavior in the seventeenth edition of ECMA-262.
 * <p>
 * ECMA-262 제17판 부속서 B의 동작을 검증한다.
 */
public class AnnexB17ConformanceTest {

    /**
     * Checks that an independent realm has ordinary objects and no HTML collection.
     * <p>
     * 독립 렐름의 객체가 일반 객체이며 HTML 컬렉션이 없는지 확인한다.
     */
    public void testIndependentRealmHasNoHtmlDda() {
        Check.equal("undefined,undefined,object,true,false,false", Scripts.eval(
                "var o={}; [typeof document,typeof HTMLAllCollection,typeof o,Boolean(o),o==null,o==undefined].join()"),
                "the standalone engine creates no HTMLDDA objects");
    }

    /**
     * Checks HTML comments and old string escapes in scripts, including the module and strict grammar boundaries.
     * <p>
     * 스크립트의 HTML 주석과 문자열 레거시 이스케이프, 모듈과 엄격 문법의 경계를 확인한다.
     */
    public void testHtmlCommentsAndLegacyEscapes() {
        Check.equal("13|89a|SyntaxError", Scripts.eval(
                "var total=6; <!-- ignored\n"
                + "total+=7;\n --> total=99;\n"
                + "var escaped='\\8\\9\\141', syntax;var bs=String.fromCharCode(92);"
                + "try{Function(\"'use strict';return '\"+bs+\"8'\");}catch(e){syntax=e.name;}"
                + "[total,escaped,syntax].join('|')"), "script grammar accepts only the applicable legacy forms");
        final JsRealm realm = JsRealm.create();
        JsException failure = (JsException) Check.throwsException(JsException.class, new Block() {
            /**
             * Parses a module containing an HTML-style comment opener.
             * <p>
             * HTML 주석 시작 표기가 있는 모듈을 파싱한다.
             */
            public void run() {
                realm.evaluateModule("module.js", "<!-- legacy\nexport const result=1;");
            }
        }, "module rejects HTML comments");
        Check.equal(ErrorKind.SYNTAX_ERROR, failure.getKind(), "module grammar error");
    }

    /**
     * Checks Annex B function declarations in blocks, labels, if branches and switch cases.
     * <p>
     * 블록, 라벨, if 분기와 switch 절의 부속서 B 함수 선언을 확인한다.
     */
    public void testLegacyDeclarationScope() {
        Check.equal("undefined:function:undefined|function:undefined:function|9|then:else|SyntaxError", Scripts.eval(
                "function choose(flag){if(flag){label: function labeled(){return 'ok';}}"
                + "switch(flag){case 0:function zero(){return 0;}break;case 1:function one(){return 1;}}"
                + "return [typeof labeled,typeof zero,typeof one].join(':');}"
                + "function collide(f){if(true){function f(){return 'nested';}}return f;}"
                + "function branch(flag){if(flag)function picked(){return 'then';}else function picked(){return 'else';}"
                + "return picked();}"
                + "var strict;try{Function(\"'use strict';label:function disallowed(){}\");}catch(e){strict=e.name;}"
                + "[choose(0),choose(1),collide(9),branch(true)+':'+branch(false),strict].join('|')"),
                "legacy copies occur only on executed declarations and respect parameter collisions");
    }

    /**
     * Checks which catch declarations accept a redeclaring var and where its assignment goes.
     * <p>
     * catch를 다시 선언하는 var의 허용 여부와 대입 위치를 확인한다.
     */
    public void testCatchVarRedeclaration() {
        Check.equal("changed|outer|2:undefined|SyntaxError", Scripts.eval(
                "var e='outer', seen, syntax;"
                + "function update(){var e='outer';try{throw 'caught';}catch(e){var e='changed';seen=e;}return e;}"
                + "var after=update();"
                + "function strict(){'use strict';var v;try{throw 1;}catch(v){var v=2;return v;}}"
                + "try{Function('try{}catch({e}){var e;}');}catch(ex){syntax=ex.name;}"
                + "[seen,after,strict()+':'+typeof v,syntax].join('|')"),
                "a simple catch binding may share a var name, a destructured binding may not");
    }

    /**
     * Checks one-time initialization, ordering, and binding name inference in sloppy for-in.
     * <p>
     * 비엄격 for-in의 한 번만 하는 초기화, 평가 순서 및 바인딩 이름 추론을 확인한다.
     */
    public void testForInVarInitializerEvaluation() {
        String script = "const log=[];"
                + "for(var key=(log.push(typeof key),log.push('init'),42) in (log.push('rhs:'+key),{a:1,b:1})){"
                + "log.push(key);}"
                + "for(var named=function(){} in null)throw 'unreachable';"
                + "for(var number=(log.push('empty'),5) in undefined)throw 'unreachable';"
                + "try{for(var abrupt=(()=>{throw 'init error';})() in (log.push('unexpected'),{})){} }"
                + "catch(e){log.push(e);}"
                + "[log.join(),key,named.name,number].join('|')";
        Check.equal("undefined,init,rhs:42,a,b,empty,init error|b|named|5", Scripts.eval(script),
                "initializer runs once before enumeration even when the object is nullish");
    }

    /**
     * Checks that the rule is restricted to non-strict for-in with a single var binding identifier.
     * <p>
     * 비엄격 for-in의 단일 var 식별자에만 규칙이 적용되는지 확인한다.
     */
    public void testForInVarInitializerEarlyErrors() {
        String script = "function invalid(s){try{Function(s);return false;}catch(e){return e.name==='SyntaxError';}}"
                + "[\"'use strict';for(var x=0 in {}){}\",\"for(let x=0 in {}){}\",\"for(const x=0 in {}){}\","
                + "\"for(var x=0 of []){}\",\"for(var {x}={} in {}){}\",\"for(var [x]=[] in {}){}\","
                + "\"for(var x=0,y in {}){}\"].every(invalid)+':'"
                + "+Function('for(var x=0 in {}){}return x;')()";
        Check.equal("true:0", Scripts.eval(script), "initializer allowance applies only to the specified grammar");
    }

    /**
     * Verifies that suspension preserves the resolved binding and the initialized state.
     * <p>
     * 실행을 중단했다 재개해도 결정한 바인딩과 초기화 상태가 유지되는지 확인한다.
     */
    public void testForInVarInitializerSuspension() {
        String script = "const base={key:'base'},events=[];"
                + "function* run(){var key='outer';with(base){"
                + "for(var key=(events.push('init'),yield 'first') in (events.push('rhs:'+key),yield 'second')){events.push(key);}"
                + "}return key;}"
                + "const flow=run(),a=flow.next();delete base.key;const b=flow.next('assigned'),binding=base.key;"
                + "const c=flow.next({one:1});"
                + "[a.value,b.value,binding,c.value,c.done,base.key,events.join()].join('|')";
        Check.equal("first|second|assigned|outer|true|one|init,rhs:assigned,one", Scripts.eval(script),
                "binding resolution and initialization precede a resumable source expression");
    }

    /**
     * Checks that ordinary call targets throw after evaluating the call and before the right operand or numeric conversion.
     * <p>
     * 일반 호출 대상은 호출을 평가한 후 우변이나 숫자 변환을 실행하기 전에 오류를 던지는지 확인한다.
     */
    public void testCallTargetsInAssignmentsAndUpdates() {
        String script = "const order=[];"
                + "function f(){order.push('call');return {valueOf(){order.push('unexpected numeric');return 1;}};}"
                + "function rhs(){order.push('unexpected rhs');return 1;}"
                + "function err(code,callee=f){try{Function('f','rhs',code)(callee,rhs);return 'none';}"
                + "catch(e){return e.name===undefined?e:e.name;}}"
                + "const forms=['f()=rhs()','(f())=rhs()','f()+=rhs()','f()**=rhs()','++f()','f()--'];"
                + "const faults=forms.map(s=>err(s));"
                + "const before=err('f()=rhs()',()=>{order.push('abrupt');throw 'original';});"
                + "[faults.every(e=>e==='ReferenceError'),before,order.join()].join('|')";
        Check.equal("true|original|call,call,call,call,call,call,abrupt", Scripts.eval(script),
                "legacy targets evaluate the ordinary call and propagate its abrupt completion");
    }

    /**
     * Checks the remaining early errors for strict code, logical assignments, optional calls, and destructuring.
     * <p>
     * 엄격 코드, 논리 대입, 선택적 호출, 구조 분해에는 계속 조기 오류가 적용되는지 확인한다.
     */
    public void testCallTargetEarlyErrors() {
        String script = "const invalid=[\"'use strict';f()=0\",\"'use strict';f()++\",\"'use strict';for(f() in {}){}\","
                + "'f() &&=0','f() ||=0','f() ??=0','f?.()=0','f()?.g()=0','f?.()++','[f()]=[]','for([f()] of []){}',"
                + "'new f()=0','import(\\\"test\\\")=0'];"
                + "invalid.every(s=>{try{Function(s);return false;}catch(e){return e.name==='SyntaxError';}})";
        Check.equal("true", Scripts.eval(script), "only the supported legacy target forms reach runtime evaluation");
    }

    /**
     * Checks legacy loop call targets and resumption without fetching another value or closing the iterator.
     * <p>
     * 레거시 반복문 호출 대상이 이터레이터를 닫거나 다음 값을 읽지 않고 재개되는지 확인한다.
     */
    public void testCallTargetsInForLoops() {
        String script = "const log=[];function f(x){log.push('call:'+x);}"
                + "const iterable={[Symbol.iterator](){return this;},next(){log.push('next');return {get value(){log.push('value');return 1;}};},"
                + "return(){log.push('close');return {};}};"
                + "for(f('empty') in {}){}"
                + "let err;try{for(f('in') in {x:1}){} }catch(e){err=e.name;}"
                + "const g=(function*(){try{for(f(yield 'pause') of iterable){} }catch(e){return e.name;}})();"
                + "const a=g.next(),b=g.next('resume');"
                + "let abrupt;try{for((()=>{throw 'call error';})() of iterable){} }catch(e){abrupt=e.name;}"
                + "let normal;const ordinary={set x(value){throw 'setter';}};"
                + "try{for(ordinary.x of iterable){} }catch(e){normal=e;}"
                + "[err,a.value,a.done,b.value,b.done,abrupt,normal,log.join()].join('|')";
        Check.equal("ReferenceError|pause|false|ReferenceError|true|ReferenceError|setter|"
                + "call:in,next,value,call:resume,next,value,next,value,close",
                Scripts.eval(script), "a loop call evaluation ends in ReferenceError before IteratorClose");
    }

    /**
     * Verifies early errors after an asynchronous loop's call target awaits an argument.
     * <p>
     * 비동기 반복문의 호출 대상이 인수를 await한 뒤 오류로 끝나는 순서를 검증한다.
     */
    public void testCallTargetInForAwait() {
        String script = "const log=[];function f(x){log.push('call:'+x);}"
                + "const input={[Symbol.asyncIterator](){return this;},next(){log.push('next');"
                + "return Promise.resolve({get value(){log.push('value');return 0;}});},"
                + "return(){log.push('close');return Promise.resolve({});}};"
                + "async function run(){try{for await(f(await Promise.resolve('argument')) of input){} }"
                + "catch(e){log.push(e.name);}"
                + "try{for await(f(await Promise.reject('argument')) of input){} }catch(e){log.push(e.name);}"
                + "const broken={...input,next(){return {get value(){throw 'value error';}};}};"
                + "try{for await(f('unreachable') of broken){} }catch(e){log.push(e);}"
                + "const ordinary={set x(value){throw 'setter';}};"
                + "try{for await(ordinary.x of input){} }catch(e){log.push(e);}"
                + "print(log.join());}run();";
        Check.equal("next,value,call:argument,ReferenceError,next,value,ReferenceError,value error,next,value,close,setter",
                Scripts.output(script), "the direct ReferenceError skips AsyncIteratorClose");
    }

    /**
     * Checks Annex B regexp grammar and the named group restriction on identity escapes.
     * <p>
     * 부속서 B 정규식 문법과 이름 있는 그룹이 있는 identity escape의 제한을 확인한다.
     */
    public void testLegacyRegExpGrammar() {
        String script = "const bs=String.fromCharCode(92);"
                + "function invalid(p,flags){try{new RegExp(p,flags);return false;}catch(e){return e.name==='SyntaxError';}}"
                + "const named='(?<n>a)['+bs+'k]';"
                + "[/\\141/.test('a'),/[\\c0]/.test(String.fromCharCode(16)),/[\\d-a]/.test('-'),"
                + "/(?=a)?a/.test('a'),/\\p{Letter}/.test('p{Letter}'),/[\\k]/.test('k'),"
                + "invalid(bs+'141','u'),invalid('['+bs+'d-a]','u'),invalid('(?=a)?a','u'),"
                + "invalid('(?<=a)?',''),invalid(named,''),"
                + "(function(){try{Function('return /'+named+'/');return false;}catch(e){return e.name==='SyntaxError';}})()].join()";
        Check.equal("true,true,true,true,true,true,true,true,true,true,true,true", Scripts.eval(script),
                "legacy escapes and quantified lookaheads are restricted to their specified contexts");
    }

    /**
     * Checks that global and sticky matching update lastIndex, while ordinary matching preserves it.
     * <p>
     * 전역·고정 검색의 lastIndex 갱신과 일반 검색의 lastIndex 보존을 확인한다.
     */
    public void testRegExpLastIndexFailures() {
        String script = "let coerced=0;const ordinary=/x/, global=/x/g, sticky=/x/y, frozen=/x/;"
                + "const index={valueOf(){coerced++;return 9;}};ordinary.lastIndex=index;"
                + "global.lastIndex=2;sticky.lastIndex=1;Object.defineProperty(frozen,'lastIndex',{writable:false});"
                + "const a=ordinary.exec('a'),b=ordinary.exec('x'),c=global.exec('a'),d=sticky.exec('xa');"
                + "[a===null,b.index,ordinary.lastIndex===index,coerced,c===null,global.lastIndex,d===null,"
                + "sticky.lastIndex,frozen.exec('a')===null].join('|')";
        Check.equal("true|0|true|2|true|0|true|0|true", Scripts.eval(script),
                "lastIndex coercion does not force a write for ordinary matching");
    }

    /**
     * Checks match and capture offsets when lastIndex points into a supplementary character.
     * <p>
     * lastIndex가 보충 문자 내부를 가리킬 때의 일치·캡처 위치를 검증한다.
     */
    public void testRegExpUnicodeLastIndexBoundary() {
        String script = "const cp=String.fromCodePoint(0x1D11E), input=cp+'x';"
                + "function result(flags){const r=new RegExp('(.)',flags);r.lastIndex=1;const m=r.exec(input);"
                + "return [m.index,m[0].length,m[1].length,m.indices[0].join(),r.lastIndex].join(':');}"
                + "const r=/x/gu;r.lastIndex=1;const after=r.exec(input);"
                + "[result('dy'),result('dyu'),result('dyv'),result('dgu'),result('dgv'),after.index,r.lastIndex].join('|')";
        Check.equal("1:1:1:1,2:2|0:2:2:0,2:2|0:2:2:0,2:2|0:2:2:0,2:2|0:2:2:0,2:2|2|3",
                Scripts.eval(script), "Unicode matching starts at the containing code point");
    }

    /**
     * Verifies that Annex B compile uses the original source and flags of a real RegExp.
     * <p>
     * 부속서 B compile이 실제 RegExp의 원래 원문과 플래그를 쓰는지 확인한다.
     */
    public void testRegExpCompileReinitializes() {
        String script = "const r=/old/y,p=/[[ab]--b]/v;r.lastIndex=2;"
                + "Object.defineProperty(p,'source',{get(){throw 'source override';}});"
                + "const same=r.compile(p)===r;"
                + "function error(f){try{f();return 'none';}catch(e){return e.name;}}"
                + "[same,r.flags,r.source,r.lastIndex,r.test('a'),r.test('b'),"
                + "error(()=>r.compile(p,'v')),error(()=>RegExp.prototype.compile.call({})),"
                + "error(()=>r.compile('a','uv')),r.flags].join('|')";
        Check.equal("true|v|[[ab]--b]|0|true|false|TypeError|TypeError|SyntaxError|v", Scripts.eval(script),
                "compile reinitializes branded regular expressions with the validated original pattern");
    }
}
