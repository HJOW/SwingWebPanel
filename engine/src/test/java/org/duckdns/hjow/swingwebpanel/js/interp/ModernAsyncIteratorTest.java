package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.js.AsyncIteratorHelper;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Execution tests for async generators and for-await-of (plan item P28-03-d).
 * <p>
 * async generator 와 for-await-of 실행 시험(계획 P28-03-d).
 */
public class ModernAsyncIteratorTest {

    /**
     * An async-from-sync iterator captures next, reads done before value and closes a rejected yield.
     * <p>
     * 동기 이터레이터를 감싸면 next를 저장하고 done 뒤 value를 읽으며 거부된 yield에서 닫는다.
     */
    public void testSyncAdapterObservationAndClosing() {
        Scripts.checkInline("const log=[];"
                + "const src={[Symbol.iterator](){return {"
                + " get next(){log.push('next');let n=0;return ()=>{n++;return {"
                + " get done(){log.push('done'+n);return false;},"
                + " get value(){log.push('value'+n);return n===1?Promise.resolve(3):Promise.reject('reason');}};};},"
                + " return(){log.push('close');throw 'ignored';}};}};"
                + "(async()=>{try{for await (const x of src) log.push(x);}catch(e){log.push(e);} "
                + "t('sync iterator',log.join(','),'next,done1,value1,3,done2,value2,close,reason');})();", 1);
    }

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public ModernAsyncIteratorTest() {
    }

    /**
     * Checks dynamic async function construction and the own prototypes of async generator functions.
     * <p>
     * 동적 비동기 함수 생성과 비동기 제너레이터 함수의 자체 프로토타입을 확인한다.
     */
    public void testDynamicAsyncFunctionKinds() {
        Scripts.checkInline("var A=(async()=>{}).constructor,G=(async function*(){}).constructor;"
                + "var add=new A('x','return (await x)+1;');"
                + "t('async function prototype',Object.getPrototypeOf(add),A.prototype);"
                + "t('async owns no prototype',Object.hasOwn(add,'prototype'),false);"
                + "t('async tag',Object.prototype.toString.call(add),'[object AsyncFunction]');"
                + "terr('await parameter',()=>new A('x=await 0','return x;'),'SyntaxError');"
                + "add(Promise.resolve(4)).then(v=>t('await result',v,5));"
                + "var make=new G('x','yield await x;return 9;'),iter=make(Promise.resolve(7));"
                + "t('own result prototype',Object.getPrototypeOf(iter),make.prototype);"
                + "t('shared result prototype',Object.getPrototypeOf(make.prototype),G.prototype.prototype);"
                + "t('only async iteration',typeof iter[Symbol.iterator],'undefined');"
                + "t('async iterator identity',iter[Symbol.asyncIterator](),iter);"
                + "var receiver={};t('generic asyncIterator',iter[Symbol.asyncIterator].call(receiver),receiver);"
                + "iter.next().then(r=>t('yielded',r.value+':'+r.done,'7:false'))"
                + ".then(()=>iter.next()).then(r=>t('completed',r.value+':'+r.done,'9:true'));",
                12);
    }

    /**
     * Checks explicit new targets and the default prototypes of foreign bound or proxied constructors.
     * <p>
     * 명시적 newTarget과 다른 렐름에 있는 바인딩·프록시 생성자의 기본 프로토타입을 확인한다.
     */
    public void testDynamicFunctionNewTarget() {
        Scripts.checkInline("var A=(async function(){}).constructor,G=(function*(){}).constructor;"
                + "function Target(){};Target.prototype={};"
                + "t('Function target',Object.getPrototypeOf(Reflect.construct(Function,['return 1'],Target)),Target.prototype);"
                + "t('GeneratorFunction target',Object.getPrototypeOf(Reflect.construct(G,['yield 1'],Target)),Target.prototype);"
                + "class Sub extends A{};"
                + "var af=new Sub('return 2;');t('derived constructor target',Object.getPrototypeOf(af),Sub.prototype);"
                + "af().then(v=>t('derived async body',v,2));",
                4);
        JsRealm local = JsRealm.create();
        JsRealm foreign = JsRealm.create();
        local.getGlobalObject().defineBuiltin("foreignTarget", foreign.evaluateScript("new Proxy((function(){}).bind(null),{});", "other"));
        JsObject generated = (JsObject) local.evaluateScript(
                "Reflect.construct((async function*(){}).constructor,['yield 1'],foreignTarget);", "local");
        Check.same(foreign.getIntrinsic("%AsyncGeneratorFunction.prototype%"), generated.getPrototypeOf(),
                "fallback prototype comes from the actual target function's realm");
    }

    /**
     * Checks the internal adapter has the realm's shared async-from-sync prototype.
     * <p>
     * 내부 어댑터가 렐름의 공용 Async-from-Sync 프로토타입을 쓰는지 확인한다.
     */
    public void testAsyncFromSyncPrototype() {
        JsRealm realm = JsRealm.create();
        JsObject adapter = AsyncIteratorHelper.getAsyncIterator(realm, realm.newArray());
        Check.same(realm.getIntrinsic("%AsyncFromSyncIteratorPrototype%"), adapter.getPrototypeOf(), "shared adapter prototype");
        Check.equal(0, adapter.ownKeys().size(), "adapter state is internal");
    }

    /**
     * Checks for-await and async yield* capture next exactly once while retaining live return lookups.
     * <p>
     * for-await와 async yield*는 next를 한 번만 저장하고 return은 호출할 때 조회하는지 확인한다.
     */
    public void testAsyncIterationCapturesNext() {
        Scripts.checkInline("function iterable(log){return {[Symbol.asyncIterator](){var index=0;return {"
                + "get next(){log.push('next');return function(){"
                + "Object.defineProperty(this,'next',{value(){throw 'unwanted reread';}});"
                + "return Promise.resolve({value:++index,done:index>2});};},"
                + "get return(){log.push('close');return function(){return Promise.resolve({done:true});};}"
                + "};}};} (async()=>{var a=[],b=[];"
                + "for await(var x of iterable(a)){a.push(x);if(x===2)break;}"
                + "async function* delegate(){yield* iterable(b);}"
                + "for await(var y of delegate())b.push(y);"
                + "t('for await',a.join(),'next,1,2,close');"
                + "t('async yield star',b.join(),'next,1,2');})();",
                2);
    }

    /**
     * Checks synchronous yield* keeps its next method and propagates a failing close when throw is unavailable.
     * <p>
     * 동기 yield*가 next를 보관하고 throw 메서드 없이 close가 실패했을 때 오류를 전파하는지 확인한다.
     */
    public void testSynchronousDelegationCapturesNext() {
        Scripts.checkInline("var gets=0;var source={[Symbol.iterator](){var count=0;return {"
                + "get next(){++gets;return ()=>({value:++count,done:false});},"
                + "get return(){return ()=>{throw 'close error';};}};}};"
                + "function* delegate(){yield* source;}"
                + "var it=delegate();t('first',it.next().value,1);t('second',it.next().value,2);"
                + "t('captured getter',gets,1);"
                + "try{it.throw('original');}catch(e){t('close error wins',e,'close error');}",
                4);
    }

    /**
     * Checks sequential next requests, Promise yield resolution and sent values.
     * <p>
     * next 요청 순서, Promise yield 해결, 전달 값을 검사한다.
     */
    public void testAsyncGenerator() {
        Scripts.checkInline(
                "async function* g(){ yield Promise.resolve(1); var x = yield 2; yield x; }"
                + "var it = g(), a = it.next(), b = it.next(7), c = it.next(9), d = it.next();"
                + "t('async iterator', it[Symbol.asyncIterator]() === it, true);"
                + "Promise.all([a,b,c,d]).then(function(r){"
                + " t('a', r[0].value + ':' + r[0].done, '1:false');"
                + " t('b', r[1].value + ':' + r[1].done, '2:false');"
                + " t('c', r[2].value + ':' + r[2].done, '9:false');"
                + " t('d', r[3].done, true); });",
                5);
    }

    /**
     * Checks return and throw requests for an async generator.
     * <p>
     * async generator의 return·throw 요청을 검사한다.
     */
    public void testAsyncGeneratorAbruptRequests() {
        Scripts.checkInline(
                "async function* g(){ try { yield 1; } finally { globalThis.closed = true; } }"
                + "var it = g(); it.next().then(function(){ it.return(5).then(function(r){"
                + " t('return', r.value + ':' + r.done, '5:true'); t('finally', globalThis.closed, true); }); });"
                + "async function* h(){ throw 'bad'; } var q = h();"
                + "q.next().then(function(){}, function(e){ t('reject', e, 'bad'); });"
                + "async function* k(){ try { yield 1; } catch(e) { yield e + '!'; } }"
                + "var u=k(); u.next().then(function(){ return u.throw('x'); }).then(function(r){"
                + " t('throw resume', r.value, 'x!'); });",
                4);
    }

    /**
     * Checks for-await over an async iterator and a synchronous iterable wrapper.
     * <p>
     * 비동기 이터레이터와 동기 반복 가능 객체 래퍼에 대한 for-await 를 검사한다.
     */
    public void testForAwait() {
        Scripts.checkInline(
                "var source = {}; source[Symbol.asyncIterator] = function(){ var i=0; return {"
                + " next:function(){ i++; return Promise.resolve({value:i,done:i>3}); } }; };"
                + "async function collect(){ var a=[]; for await (var x of source) a.push(x);"
                + " for await (var y of [4,5]) a.push(y); return a.join(','); }"
                + "collect().then(function(v){ t('values', v, '1,2,3,4,5'); });",
                1);
    }

    /**
     * Checks yield* delegation to asynchronous and synchronous iterables.
     * <p>
     * 비동기·동기 반복 가능 객체로의 yield* 위임을 검사한다.
     */
    public void testAsyncYieldStar() {
        Scripts.checkInline(
                "var source = {}; source[Symbol.asyncIterator] = function(){ var i=0; return {"
                + " next:function(v){ i++; return Promise.resolve({value:i,done:i>2}); } }; };"
                + "async function* g(){ yield* source; yield* [3,4]; }"
                + "async function run(){ var out=[]; for await (var v of g()) out.push(v); return out.join(','); }"
                + "run().then(function(v){ t('delegate values', v, '1,2,3,4'); });",
                1);
    }

    /**
     * Checks throw forwarding to sync iterators and cleanup when an async iterator has no throw method.
     * <p>
     * 동기 반복자의 throw 전달과 async 반복자에 throw 메서드가 없을 때의 정리를 검사한다.
     */
    public void testAsyncYieldStarThrow() {
        Scripts.checkInline(
                "var sync = {}; sync[Symbol.iterator] = function(){ return {"
                + " next:function(){ return {value:'open',done:false}; },"
                + " throw:function(e){ return {value:'caught:'+e,done:true}; } }; };"
                + "async function* sg(){ return yield* sync; } var si=sg();"
                + "si.next().then(function(){ return si.throw('x'); }).then(function(r){"
                + " t('sync throw', r.value + ':' + r.done, 'caught:x:true'); });"
                + "var closed=0, asyncSource={}; asyncSource[Symbol.asyncIterator]=function(){ var n=0; return {"
                + " next:function(){ n++; return Promise.resolve({value:n,done:false}); },"
                + " return:function(){ closed++; return Promise.resolve({done:true}); } }; };"
                + "async function* ag(){ yield* asyncSource; } var ai=ag();"
                + "ai.next().then(function(){ return ai.throw('bad'); }).then(function(){ t('missing throw rejected', false, true); },"
                + " function(e){ t('missing throw type', e instanceof TypeError, true); t('missing throw close', closed, 1); });",
                3);
    }

    /**
     * Checks async iterator close on break and body rejection.
     * <p>
     * break 와 본문 거부에서 비동기 이터레이터 close 를 검사한다.
     */
    public void testForAwaitClose() {
        Scripts.checkInline(
                "var closed = 0; var source = {}; source[Symbol.asyncIterator] = function(){ var i=0; return {"
                + " next:function(){ return Promise.resolve({value:++i,done:false}); },"
                + " return:function(){ closed++; return Promise.resolve({done:true}); } }; };"
                + "async function stop(){ for await (var x of source) { break; } }"
                + "async function fail(){ for await (var x of source) { throw 'bad'; } }"
                + "stop().then(function(){ t('break close', closed, 1); return fail(); })"
                + ".then(function(){}, function(e){ t('original throw', e, 'bad'); t('throw close', closed, 2); });",
                3);
    }
}

