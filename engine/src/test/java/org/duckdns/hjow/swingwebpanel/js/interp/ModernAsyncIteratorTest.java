package org.duckdns.hjow.swingwebpanel.js.interp;

/**
 * Execution tests for async generators and for-await-of (plan item P28-03-d).
 * <p>
 * async generator 와 for-await-of 실행 시험(계획 P28-03-d).
 */
public class ModernAsyncIteratorTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public ModernAsyncIteratorTest() {
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

