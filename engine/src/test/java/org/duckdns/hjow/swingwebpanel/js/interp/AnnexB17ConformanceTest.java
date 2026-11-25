package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Verifies Annex B behavior in the seventeenth edition of ECMA-262.
 * <p>
 * ECMA-262 제17판 부속서 B의 동작을 검증한다.
 */
public class AnnexB17ConformanceTest {

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
     * Checks legacy loop call targets, iterator closing, and resumption without fetching another value.
     * <p>
     * 레거시 반복문 호출 대상, 이터레이터 닫기 및 다음 값을 읽지 않는 재개를 확인한다.
     */
    public void testCallTargetsInForLoops() {
        String script = "const log=[];function f(x){log.push('call:'+x);}"
                + "const iterable={[Symbol.iterator](){return this;},next(){log.push('next');return {get value(){log.push('value');return 1;}};},"
                + "return(){log.push('close');return {};}};"
                + "for(f('empty') in {}){}"
                + "let err;try{for(f('in') in {x:1}){} }catch(e){err=e.name;}"
                + "const g=(function*(){try{for(f(yield 'pause') of iterable){} }catch(e){return e.name;}})();"
                + "const a=g.next(),b=g.next('resume');"
                + "let original;try{for((()=>{throw 'call error';})() of iterable){} }catch(e){original=e;}"
                + "[err,a.value,a.done,b.value,b.done,original,log.join()].join('|')";
        Check.equal("ReferenceError|pause|false|ReferenceError|true|call error|call:in,next,value,call:resume,close,next,value,close",
                Scripts.eval(script), "loop call targets keep iteration state and close the iterator on failure");
    }

    /**
     * Verifies close ordering when an asynchronous loop's call target awaits an argument.
     * <p>
     * 비동기 반복문의 호출 대상이 인수를 await할 때 닫는 순서를 검증한다.
     */
    public void testCallTargetInForAwait() {
        String script = "const log=[];function f(x){log.push('call:'+x);}"
                + "const input={[Symbol.asyncIterator](){return this;},next(){log.push('next');return Promise.resolve({value:0});},"
                + "return(){log.push('close');return Promise.resolve({});}};"
                + "async function run(){try{for await(f(await Promise.resolve('argument')) of input){} }"
                + "catch(e){print(e.name+':'+log.join());}}run();";
        Check.equal("ReferenceError:next,call:argument,close", Scripts.output(script),
                "await resumes the call and IteratorClose precedes the catch");
    }
}
