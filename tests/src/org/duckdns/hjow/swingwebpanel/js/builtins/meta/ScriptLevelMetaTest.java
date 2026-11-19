package org.duckdns.hjow.swingwebpanel.js.builtins.meta;

import java.util.ArrayList;
import java.util.List;

import org.duckdns.hjow.swingwebpanel.js.ConsoleHook;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Script level tests of the meta library: JavaScript source texts are run by the interpreter of a standard realm
 * ({@link JsRealm#create()}) and the lines printed with <code>print</code> (including the ones printed by promise jobs that run after
 * the script) are compared with the expected output. They cover the way a page would use Proxy, Reflect, Promise and the Annex B
 * functions <code>escape</code> and <code>unescape</code>, and complement the Java level tests of the same classes.
 * <p>
 * 메타 라이브러리의 스크립트 수준 시험: 표준 realm({@link JsRealm#create()})의 인터프리터로 JavaScript 소스를 실행하고, <code>print</code>
 * 로 출력한 줄(스크립트 뒤에 실행되는 promise 작업이 출력한 것 포함)을 기대한 출력과 비교한다. 페이지가 Proxy, Reflect, Promise 와 부속서 B 의
 * <code>escape</code>, <code>unescape</code> 를 쓰는 방식을 다루며 같은 클래스의 Java 수준 시험을 보완한다.
 * <p>
 * A case reports all of its mismatches at once, so that one run shows every failing script.
 * <p>
 * 한 사례는 어긋난 것을 한꺼번에 보고하므로, 한 번 실행으로 실패한 스크립트를 모두 볼 수 있다.
 */
public class ScriptLevelMetaTest {

    /**
     * The mismatches of the running case.
     * <p>
     * 실행 중인 사례에서 어긋난 것.
     */
    private final List<String> failures = new ArrayList<String>();

    /**
     * The number of scripts that were run by the running case.
     * <p>
     * 실행 중인 사례가 실행한 스크립트 수.
     */
    private int scripts;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ScriptLevelMetaTest() {
    }

    /**
     * Resets the counters for each case.
     * <p>
     * 사례마다 집계를 초기화한다.
     */
    public void setUp() {
        failures.clear();
        scripts = 0;
    }

    // ------------------------------------------------------------------ helpers

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
        realm.evaluateScript(source, "meta-script.js");
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
     * Runs one script and records a mismatch (or an exception) instead of failing at once.
     * <p>
     * 스크립트 하나를 실행하고 바로 실패하는 대신 어긋남(또는 예외)을 기록한다.
     *
     * @param id       the case id / 사례 id
     * @param source   the script / 스크립트
     * @param expected the expected output / 기대한 출력
     */
    private void expect(String id, String source, String expected) {
        scripts++;
        String actual;
        try {
            actual = run(source);
        } catch (JsException e) {
            actual = "uncaught: " + e.getMessage();
        } catch (RuntimeException e) {
            actual = "java: " + e;
        }
        if (!expected.equals(actual)) {
            failures.add(id + ": expected [" + expected + "] but got [" + actual + "]");
        }
    }

    /**
     * Fails the case if any script mismatched.
     * <p>
     * 어긋난 스크립트가 하나라도 있으면 사례를 실패시킨다.
     */
    private void finish() {
        if (!failures.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append(failures.size()).append(" of ").append(scripts).append(" scripts mismatched:");
            // 어긋난 것을 한 줄씩 메시지에 싣는다.
            for (int i = 0; i < failures.size(); i++) {
                sb.append("\n  ").append(failures.get(i));
            }
            Check.fail(sb.toString());
        }
    }

    // ------------------------------------------------------------------ Proxy

    /**
     * Proxy: forwarding, the get and set traps, receivers, traps of every object operation and function proxies.
     * <p>
     * Proxy: 전달, get/set trap, receiver, 모든 객체 연산의 trap, 함수 프록시.
     */
    public void testProxyScripts() {
        expect("proxy.get", "var log=[]; var t={a:1}; var p=new Proxy(t,{get:function(t,k,r){log.push(String(k)+':'+(r===p)); return t[k];}});"
                + " print(p.a); print(log.join(','));", "1|a:true");
        expect("proxy.set", "var t={}; var p=new Proxy(t,{set:function(t,k,v,r){t[k]=v*2; return true;}}); p.x=5; print(t.x);", "10");
        expect("proxy.set.strict-false", "var p=new Proxy({},{set:function(){return false;}});"
                + " (function(){'use strict'; try{p.x=1; print('no');}catch(e){print(e.name);} })();"
                + " (function(){ p.y=1; print('sloppy ok'); })();", "TypeError|sloppy ok");
        expect("proxy.all-traps", "var log=[]; var h={};"
                + " ['has','deleteProperty','ownKeys','getOwnPropertyDescriptor','defineProperty','getPrototypeOf','setPrototypeOf',"
                + "'isExtensible','preventExtensions'].forEach(function(n){ h[n]=function(){ log.push(n); return Reflect[n].apply(null,arguments);}; });"
                + " var p=new Proxy({a:1},h); ('a' in p); delete p.a; Object.keys(p); Object.getOwnPropertyDescriptor(p,'z');"
                + " Object.defineProperty(p,'q',{value:1,configurable:true}); Object.getPrototypeOf(p); Object.setPrototypeOf(p,null);"
                + " Object.isExtensible(p); Object.preventExtensions(p); print(log.join(','));",
                "has,deleteProperty,ownKeys,getOwnPropertyDescriptor,defineProperty,getPrototypeOf,setPrototypeOf,isExtensible,preventExtensions");
        expect("proxy.function", "var f=new Proxy(function(a,b){return a+b;},{apply:function(t,thisArg,args){return t.apply(thisArg,args)*10;},"
                + "construct:function(t,args,nt){return {made:args.length};}}); print(f(1,2)); print((new f(1,2,3)).made); print(typeof f);",
                "30|3|function");
        expect("proxy.revocable", "var r=Proxy.revocable({},{}); r.revoke(); r.revoke(); try{ r.proxy.x; print('no'); }catch(e){ print(e instanceof TypeError); }"
                + " print(typeof r.proxy);", "true|object");
        expect("proxy.invariant", "var t={}; Object.defineProperty(t,'k',{value:1}); var p=new Proxy(t,{get:function(){return 2;}});"
                + " try{ p.k; print('no'); }catch(e){ print(e.name); }", "TypeError");
        expect("proxy.as-prototype", "var o=Object.create(new Proxy({},{get:function(t,k,r){return 'P:'+String(k);}})); print(o.foo);", "P:foo");
        expect("proxy.array", "var a=new Proxy([1,2,3],{}); print(Array.isArray(a)); print(JSON.stringify(a)); print(a.length);"
                + " print(Array.prototype.map.call(a,function(x){return x*2;}).join(','));", "true|[1,2,3]|3|2,4,6");
        expect("proxy.class-tags", "print(Object.prototype.toString.call(new Proxy([],{}))); print(Object.prototype.toString.call(new Proxy(function(){},{})));"
                + " print(typeof new Proxy({},{})); print(typeof new Proxy(function(){},{}));", "[object Array]|[object Function]|object|function");
        expect("proxy.for-in", "var p=new Proxy({a:1,b:2},{}); var ks=[]; for (var k in p) { ks.push(k); } print(ks.join(','));", "a,b");
        expect("proxy.shape", "print(Proxy.length); print('prototype' in Proxy); try{ Proxy({},{}); }catch(e){ print(e.name); }"
                + " try{ new Proxy(1,{}); }catch(e){ print(e.name); } try{ new Proxy({},1); }catch(e){ print(e.name); }", "2|false|TypeError|TypeError|TypeError");
        expect("proxy.new-target", "class A{ constructor(){ this.nt=new.target; } } var P=new Proxy(A,{}); var o=new P(); print(o.nt===P);", "true");
        finish();
    }

    // ------------------------------------------------------------------ Reflect

    /**
     * Reflect: every function with its receivers, new target, return values and errors for non-object targets.
     * <p>
     * Reflect: 모든 함수와 receiver, new target, 반환값, 객체가 아닌 대상에 대한 오류.
     */
    public void testReflectScripts() {
        expect("reflect.has-get-set", "print(Reflect.has({a:1},'a')); print(Reflect.get({get x(){return this.y;}},'x',{y:7}));"
                + " var o={}; var recv={}; print(Reflect.set(o,'k',1,recv)); print(recv.k+','+o.k);", "true|7|true|1,undefined");
        expect("reflect.ownKeys-order", "print(Reflect.ownKeys({b:1,a:2,1:0,[Symbol.iterator]:0}).map(function(k){return typeof k==='symbol'?'S':k;}).join(','));",
                "1,b,a,S");
        expect("reflect.construct", "function F(){ this.nt=new.target; } function G(){} var o=Reflect.construct(F,[],G);"
                + " print((o.nt===G)+','+(Object.getPrototypeOf(o)===G.prototype));", "true,true");
        expect("reflect.apply", "print(Reflect.apply(Math.max,null,[1,3,2])); try{ Reflect.apply(1); }catch(e){ print(e.name); }", "3|TypeError");
        expect("reflect.boolean-results", "print(Reflect.defineProperty(Object.freeze({}),'x',{value:1})); print(Reflect.deleteProperty(Object.freeze({a:1}),'a'));"
                + " print(Reflect.isExtensible(Object.freeze({}))); print(Reflect.preventExtensions({})); print(Reflect.setPrototypeOf({},null));"
                + " print(Reflect.setPrototypeOf(Object.preventExtensions({}),{})); print(Reflect.getPrototypeOf([])===Array.prototype);",
                "false|false|false|true|true|false|true");
        expect("reflect.descriptor", "print(JSON.stringify(Reflect.getOwnPropertyDescriptor({a:1},'a'))); print(Reflect.getOwnPropertyDescriptor({},'a'));",
                "{\"value\":1,\"writable\":true,\"enumerable\":true,\"configurable\":true}|undefined");
        expect("reflect.non-object-target", "var names=['get','ownKeys','getPrototypeOf','has','isExtensible']; names.forEach(function(n){ try{ Reflect[n](1,'a'); print(n+' no'); }"
                + "catch(e){ print(n+':'+e.name); } });", "get:TypeError|ownKeys:TypeError|getPrototypeOf:TypeError|has:TypeError|isExtensible:TypeError");
        expect("reflect.enumerate", "var o=Object.create({p:1}); o.q=2; print(Array.from(Reflect.enumerate(o)).join(','));", "q,p");
        expect("reflect.with-proxy", "var log=[]; var p=new Proxy({},{set:function(t,k,v,r){ log.push(k); return Reflect.set(t,k,v,r); },"
                + "defineProperty:function(t,k,d){ log.push('def:'+k); return Reflect.defineProperty(t,k,d); }}); p.x=1; print(log.join(','));", "x,def:x");
        finish();
    }

    // ------------------------------------------------------------------ Promise

    /**
     * Promise: job ordering, thenable assimilation ticks, combinators, error propagation and the host rejection tracker.
     * <p>
     * Promise: 작업 순서, thenable 흡수에 걸리는 tick, 결합 함수, 오류 전파, 호스트의 거부 추적.
     */
    public void testPromiseScripts() {
        expect("promise.ordering", "Promise.resolve().then(function(){print('p1');}); print('s1'); Promise.resolve().then(function(){print('p2');}); print('s2');",
                "s1|s2|p1|p2");
        expect("promise.thenable-ticks", "var out=[]; var p=Promise.resolve(); new Promise(function(res){ res(Promise.resolve()); }).then(function(){ out.push('A'); });"
                + " p.then(function(){out.push('1');}).then(function(){out.push('2');}).then(function(){out.push('3');}).then(function(){out.push('4');})"
                + ".then(function(){ print(out.join(',')); });", "1,2,A,3,4");
        expect("promise.all", "Promise.all([1,Promise.resolve(2),{then:function(r){ r(3); }}]).then(function(v){ print(v.join('+')); });", "1+2+3");
        expect("promise.all-empty-and-reject", "Promise.all([]).then(function(v){ print(v.length); }); Promise.all([Promise.reject(1),Promise.resolve(2)]).catch(print);",
                "0|1");
        expect("promise.race", "Promise.race([Promise.reject('x'),Promise.resolve(1)]).catch(function(e){ print('r:'+e); }); Promise.race([]).then(function(){ print('never'); });",
                "r:x");
        expect("promise.callback-throws", "Promise.resolve().then(function(){ throw new RangeError('boom'); }).catch(function(e){ print(e.name+':'+e.message); });",
                "RangeError:boom");
        expect("promise.chain-flatten", "Promise.resolve(1).then(function(v){ return v+1; }).then(function(v){ return new Promise(function(res){ res(v*10); }); }).then(print);",
                "20");
        expect("promise.identity-and-errors", "var p=Promise.resolve(1); print(Promise.resolve(p)===p); print(p.constructor===Promise);"
                + " print(Object.prototype.toString.call(p)); try{ Promise(function(){}); }catch(e){ print(e.name); } try{ new Promise(1); }catch(e){ print(e.name); }",
                "true|true|[object Promise]|TypeError|TypeError");
        expect("promise.subclass", "class MyP extends Promise {} var q=MyP.resolve(1); print(q instanceof MyP); print(q.then(function(){}) instanceof MyP);"
                + " print(MyP.all([]) instanceof MyP);", "true|true|true");
        expect("promise.executor-throws-and-double-resolve", "new Promise(function(){ throw new Error('x'); }).catch(function(e){ print(e.message); });"
                + " new Promise(function(res,rej){ res(1); rej(2); res(3); }).then(print,function(){ print('bad'); });", "x|1");
        expect("promise.self-resolution", "var res; var p=new Promise(function(r){ res=r; }); res(p); p.catch(function(e){ print(e.name); });", "TypeError");
        expect("promise.generator-runner", "function run(g){ var it=g(); function step(v){ var r=it.next(v); if(r.done){ return Promise.resolve(r.value); }"
                + " return Promise.resolve(r.value).then(step); } return step(); }"
                + " run(function*(){ var a=yield Promise.resolve(1); var b=yield 2; return a+b; }).then(print);", "3");
        expect("promise.shape", "print(Promise.length); print(Promise.prototype.then.length); print(Promise.prototype.catch.length);"
                + " print(typeof Promise[Symbol.species]); print(Promise[Symbol.species]===Promise);", "1|2|1|function|true");
        finish();
    }

    /**
     * The host tracker is told about a rejection without a handler and about a handler that is added later.
     * <p>
     * 호스트 추적기가 처리기 없는 거부와 나중에 추가된 처리기를 통지받는다.
     */
    public void testRejectionTrackerFromScript() {
        final List<String> log = new ArrayList<String>();
        JsRealm realm = JsRealm.create();
        PromiseJobs.setRejectionTracker(realm, new PromiseRejectionTracker() {
            public void rejectedWithoutHandler(JsRealm r, JsObject promise, JsValue reason) {
                log.add("reject:" + Ops.toString(reason));
            }

            public void handlerAddedToRejectedPromise(JsRealm r, JsObject promise) {
                log.add("handled");
            }
        });
        realm.evaluateScript("Promise.reject(1); var p=Promise.reject(2); p.catch(function(){}); Promise.resolve(3);", "tracker.js");
        realm.runJobs();
        Check.equal("reject:1,reject:2,handled", join(log), "tracker notifications");
    }

    /**
     * Joins a list with commas.
     * <p>
     * 목록을 쉼표로 잇는다.
     *
     * @param items the items / 항목들
     * @return the joined text / 이은 문자열
     */
    private static String join(List<String> items) {
        StringBuilder sb = new StringBuilder();
        // 항목을 순서대로 쉼표로 잇는다.
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(items.get(i));
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ Annex B

    /**
     * Annex B.2.1: escape and unescape from a script.
     * <p>
     * 부속서 B.2.1: 스크립트에서 쓰는 escape 와 unescape.
     */
    public void testLegacyScripts() {
        expect("legacy.escape", "print(escape('a b/'+String.fromCharCode(0xD55C,0xE9)));", "a%20b/%uD55C%E9");
        expect("legacy.unescape", "print(unescape('%u0041%41%zz%u00')); print(unescape(escape('x y')+'%u0020'));", "AA%zz%u00|x y ");
        expect("legacy.shape", "print(escape.length+','+unescape.length+','+typeof escape+','+escape.name);", "1,1,function,escape");
        finish();
    }
}
