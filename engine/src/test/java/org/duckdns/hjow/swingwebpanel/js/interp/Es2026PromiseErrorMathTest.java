package org.duckdns.hjow.swingwebpanel.js.interp;

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
}
