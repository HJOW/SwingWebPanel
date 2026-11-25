package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.ScriptAbortedException;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Checks the additional Promise, Error, and Math operations in current ECMAScript.
 * <p>
 * 최신 ECMAScript의 Promise, Error 및 Math 추가 연산을 검증한다.
 */
public class Es2026PromiseErrorMathTest {

    /**
     * Checks the new capability object, subclassing, and both settlement functions.
     * <p>
     * 새 capability 객체, Promise 하위 클래스 및 두 해결 함수를 검증한다.
     */
    public void testPromiseWithResolvers() {
        String script = "class Sub extends Promise{}"
                + "const a=Sub.withResolvers(), b=Promise.withResolvers(), reason={};"
                + "const d=Object.getOwnPropertyDescriptor(a,'resolve'), m=Object.getOwnPropertyDescriptor(Promise,'withResolvers');"
                + "print([Object.keys(a).join(),Object.getPrototypeOf(a)===Object.prototype,a.promise instanceof Sub,"
                + "a.promise!==b.promise,d.writable,d.enumerable,d.configurable,m.value.name,m.value.length,"
                + "m.writable,m.enumerable,m.configurable].join('|'));"
                + "a.promise.then(v=>print('fulfilled:'+v));b.promise.catch(e=>print('rejected:'+(e===reason)));"
                + "a.resolve(42); a.reject('ignored'); b.reject(reason); b.resolve('ignored');";
        Check.equal("promise,resolve,reject|true|true|true|true|true|true|withResolvers|0|true|false|true\n"
                + "fulfilled:42\nrejected:true", Scripts.output(script),
                "withResolvers exposes one promise capability with the standard property attributes");
    }

    /**
     * Checks that try calls synchronously, forwards its arguments, and settles through the promise job queue.
     * <p>
     * try의 동기 호출, 인수 전달, promise 작업 큐를 통한 이행과 거부를 확인한다.
     */
    public void testPromiseTryCompletions() {
        String script = "'use strict';const log=[], thrown={};"
                + "class Sub extends Promise{constructor(executor){log.push('construct');super(executor);}}"
                + "const thenable={get then(){log.push('get');return function(resolve){log.push('then');resolve(7);};}};"
                + "const p=Sub.try(function(a,b){log.push('call:'+(this===undefined)+':'+(a+b));return thenable;},2,3);"
                + "log.push('returned');print([p instanceof Sub,Promise.try.name,Promise.try.length,log.join()].join('|'));"
                + "p.then(v=>print('adopted:'+v+':'+log.join()));"
                + "Promise.try(()=>42).then(v=>print('value:'+v));"
                + "Promise.try(()=>{throw thrown;}).catch(reason=>print('throw:'+(reason===thrown)));"
                + "Promise.try(0).catch(reason=>print('call:'+reason.name));";
        Check.equal("true|try|1|construct,call:true:5,get,returned\n"
                + "value:42\nthrow:true\ncall:TypeError\nadopted:7:construct,call:true:5,get,returned,construct,then",
                Scripts.output(script), "try calls once synchronously and adopts the callback completion");
    }

    /**
     * Checks constructor and callback order and exceptions from user-defined resolving functions.
     * <p>
     * 생성자와 콜백의 호출 순서, 사용자 정의 해결 함수가 던진 예외를 검증한다.
     */
    public void testPromiseCapabilityConstructorProtocol() {
        String script = "'use strict'; const events=[], promise={};"
                + "const resolve=function(v){events.push('resolve:'+v+':'+(this===undefined));throw 'resolve failure';};"
                + "const reject=function(v){events.push('reject:'+v+':'+(this===undefined));throw 'reject failure';};"
                + "function C(executor){events.push('new');executor(resolve,reject);return promise;}"
                + "function thrown(f){try{f();return 'none';}catch(e){return e.name===undefined?e:e.name;}}"
                + "const exposed=Promise.withResolvers.call(C);"
                + "const r=thrown(()=>Promise.try.call(C,()=>{events.push('call');return 3;}));"
                + "const j=thrown(()=>Promise.try.call(C,()=>{events.push('throw');throw 'reason';}));"
                + "const bad=thrown(()=>Promise.try.call(()=>{},()=>events.push('wrong')));"
                + "const primitive=thrown(()=>Promise.withResolvers.call('x'));"
                + "[exposed.promise===promise,exposed.resolve===resolve,exposed.reject===reject,"
                + "r,j,bad,primitive,events.join()].join('|')";
        Check.equal("true|true|true|resolve failure|reject failure|TypeError|TypeError|"
                + "new,new,call,resolve:3:true,new,throw,reject:reason:true", Scripts.eval(script),
                "generic constructors are checked before calling the callback and cannot hide resolving errors");
    }

    /**
     * Checks ErrorData across realms and prototype changes without consulting proxies or emulated errors.
     * <p>
     * realm과 프로토타입 변경을 넘어 ErrorData가 유지되고 프록시나 모방 오류를 조회하지 않는지 검사한다.
     */
    public void testErrorIsErrorBrand() {
        JsRealm foreign = JsRealm.create();
        JsRealm realm = JsRealm.create();
        realm.getGlobalObject().defineBuiltin("foreign", foreign.evaluateScript("new RangeError('foreign')", "other realm"));
        String script = "let observed=0;const native=new TypeError('native'), tagged={[Symbol.toStringTag]:'Error'};"
                + "const fake=Object.create(Error.prototype), revoked=Proxy.revocable(native,{}); revoked.revoke();"
                + "const p=new Proxy(native,{get(){observed++;throw 'get';},getPrototypeOf(){observed++;throw 'prototype';}});"
                + "class Sub extends Error{}"
                + "const frame=new Sub();Object.setPrototypeOf(frame,null);"
                + "const descriptor=Object.getOwnPropertyDescriptor(Error,'isError');"
                + "const internal=(()=>{try{null.x;}catch(e){return e;}})();"
                + "[foreign instanceof Error, Error.isError(foreign),Error.isError(frame),"
                + "Error.isError(internal),Error.isError(new AggregateError([])),Error.isError(Error('called')),"
                + "[null,undefined,1,'error',true,1n,Symbol(),Error,Error.prototype,fake,tagged,p,revoked.proxy].every(x=>!Error.isError(x)),"
                + "observed,descriptor.value.name,descriptor.value.length,descriptor.writable,descriptor.enumerable,"
                + "descriptor.configurable,Error.isError.call(null,native)].join('|')";
        Check.equal("false|true|true|true|true|true|true|0|isError|1|true|false|true|true",
                Ops.toString(realm.evaluateScript(script, "error-brand.js")), "Error.isError observes only the internal error brand");
    }

    /**
     * Checks exact accumulation, subnormal values, and final rounding at both ties and overflow.
     * <p>
     * 정확한 누적, 비정규 수, 중간값과 오버플로우에서의 최종 반올림을 확인한다.
     */
    public void testSumPreciseRounding() {
        String script = "const sum=Math.sumPrecise,max=Number.MAX_VALUE,min=Number.MIN_VALUE,half=2**-53,ulp=2**-52;"
                + "[sum([1e30,1,-1e30])===1,sum([max,max,-max])===max,sum([max,min,-max])===min,"
                + "sum([-max,-min,max])===-min,sum([min,min])===2*min,sum([1,half])===1,"
                + "sum([1+ulp,half])===1+2*ulp,sum([1,half,min])===1+ulp,sum([1,half,-min])===1,"
                + "sum([max,2**970])===Infinity,sum([-max,-(2**970)])===-Infinity,"
                + "sum([max,2**969])===max,sum([2,4,-6])===0,Math.sumPrecise.length,Math.sumPrecise.name].join()";
        Check.equal("true,true,true,true,true,true,true,true,true,true,true,true,true,1,sumPrecise",
                Scripts.eval(script), "finite nonzero values are accumulated exactly and rounded once");
    }

    /**
     * Checks signed zero, nonfinite values, element validation, and iterator closing behavior.
     * <p>
     * 부호 있는 0, 비유한 값, 원소 검사 및 이터레이터 닫기 동작을 검증한다.
     */
    public void testSumPreciseSpecialValuesAndClosing() {
        String script = "const sum=Math.sumPrecise;let calls=0,closes=0,coercions=0,nextCloses=0;"
                + "const wrong={valueOf(){coercions++;return 3;}};"
                + "const bad={[Symbol.iterator](){return this;},next(){calls++;return {value:calls===1?NaN:wrong};},"
                + "return(){closes++;throw 'ignore this';}};"
                + "const nextError={[Symbol.iterator](){return this;},next(){return {get value(){throw 'value failure';}};},"
                + "return(){nextCloses++;return {};}};"
                + "function fails(items){try{sum(items);return 'none';}catch(e){return e.name===undefined?e:e.name;}}"
                + "[Object.is(sum([]),-0),Object.is(sum([-0,-0]),-0),Object.is(sum([-0,0]),0),Object.is(sum([1,-1]),0),"
                + "sum([Infinity,Infinity,1])===Infinity,sum([-Infinity,1])===-Infinity,"
                + "Number.isNaN(sum([Infinity,-Infinity])),Number.isNaN(sum([NaN,1])),"
                + "fails(bad),calls,closes,coercions,fails(nextError),nextCloses,"
                + "[null,undefined,36,[1n],[new Number(2)],[true],['3'],new Array(1)].every(x=>fails(x)==='TypeError'),"
                + "sum(new Float64Array([3,4]))].join('|')";
        Check.equal("true|true|true|true|true|true|true|true|TypeError|2|1|0|value failure|0|true|7",
                Scripts.eval(script), "iteration and validation continue after a nonfinite value");
    }

    /**
     * Verifies that native iteration contributes to the realm's step limit.
     * <p>
     * 내장 이터레이션도 realm의 실행 단계 제한에 반영되는지 검증한다.
     */
    public void testSumPreciseExecutionLimit() {
        JsRealm realm = JsRealm.create();
        realm.evaluateScript("var items = new Float64Array(500);", "sum-setup.js");
        realm.getLimits().setMaxSteps(50L);
        Check.equal("3", Ops.toString(realm.evaluateScript("Math.sumPrecise([3]);", "sum-control.js")), "small sum fits the budget");
        try {
            realm.evaluateScript("Math.sumPrecise(items);", "sum-limits.js");
            Check.fail("large typed array should exceed the step limit");
        } catch (ScriptAbortedException e) {
            Check.equal(ScriptAbortedException.Reason.STEP_LIMIT, e.getReason(), "per-element native work is charged");
        }
    }
}
