package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests ES2017-ES2022 built-ins that are added by P28-05.
 * <p>
 * P28-05 가 추가하는 ES2017-ES2022 내장 객체를 검사한다.
 */
public final class Es2022BuiltinsTest {

    /**
     * Creates the suite.
     * <p>
     * 시험 묶음을 만든다.
     */
    public Es2022BuiltinsTest() {
    }

    /**
     * Checks the order of length, presence, read, compare, write and delete operations on a sparse proxy.
     * <p>
     * 희소 프록시에서 길이·존재 검사·읽기·비교·쓰기·삭제 순서를 확인한다.
     */
    public void testSortIndexedPropertyOrder() {
        Check.equal("length,h0,g0,h1,h2,g2,h3,g3,h4,compare,s0,s1,s2,d3,d4|1,3,undefined,false",
                Scripts.eval("var log=[], o=Object.create({2:undefined}); o.length=5; o[0]=3; o[3]=1;"
                + " var p=new Proxy(o,{get(t,k){log.push(k==='length'?k:'g'+k);return t[k];},"
                + " has(t,k){log.push('h'+k);return k in t;},set(t,k,v){log.push('s'+k);t[k]=v;return true;},"
                + " deleteProperty(t,k){log.push('d'+k);return delete t[k];}});"
                + " Array.prototype.sort.call(p,function(a,b){log.push('compare');return a-b;});"
                + " log.join()+'|'+o[0]+','+o[1]+','+o[2]+','+(3 in o)"),
                "gather inherited values first, then sort, write and delete every trailing index");
    }

    /**
     * Checks that abrupt completion stops at the current sort phase and keeps already visible side effects.
     * <p>
     * 예외가 발생한 단계에서 정렬이 멈추며 이미 발생한 부작용은 유지되는지 확인한다.
     */
    public void testSortAbruptCompletion() {
        Check.equal("stop|3,7,2", Scripts.eval("var a=[3,1,2];"
                + " try{a.sort(function(){a[1]=7;throw 'stop';});}catch(e){e+'|'+a.join();}"),
                "a throwing callback retains its mutation but prevents sort writeback");
        Check.equal("convert|0", Scripts.eval("var reads=0, writes=0, a=[];"
                + " function item(n){return {toString(){if(++reads===5)throw 'convert';return String(n);}};}"
                + " a.push(item(4),item(2),item(3),item(1));"
                + " var p=new Proxy(a,{set(t,k,v){writes++;t[k]=v;return true;}});"
                + " try{Array.prototype.sort.call(p);'completed';}catch(e){e+'|'+writes;}"),
                "default string conversion runs in each comparison and stops on error");
        Check.equal("TypeError|s0,d1|7", Scripts.eval("var log=[], o={length:4,3:7};"
                + " var p=new Proxy(o,{set(t,k,v){log.push('s'+k);t[k]=v;return true;},"
                + " deleteProperty(t,k){log.push('d'+k);return false;}});"
                + " try{Array.prototype.sort.call(p);}catch(e){e.name+'|'+log.join()+'|'+o[0];}"),
                "a rejected deletion of an absent key aborts after prior writes");
    }

    /**
     * Checks Object entries, values, descriptors, fromEntries and hasOwn.
     * <p>
     * Object 의 entries, values, descriptors, fromEntries, hasOwn 을 검사한다.
     */
    public void testModernObjectMethods() {
        Check.equal("a:1,b:2", Scripts.eval("var s=Symbol('x'),o={a:1};Object.defineProperty(o,'b',{value:2,enumerable:true});o[s]=3;Object.entries(o).map(function(x){return x.join(':')}).join(',')"), "entries");
        Check.equal("1,2", Scripts.eval("Object.values({a:1,b:2}).join(',')"), "values");
        Check.equal("1,true,true,true", Scripts.eval("var d=Object.getOwnPropertyDescriptors({x:1}); d.x.value+','+d.x.writable+','+d.x.enumerable+','+d.x.configurable"), "descriptors");
        Check.equal("1,2", Scripts.eval("var o=Object.fromEntries([['a',1],['b',2]]);o.a+','+o.b"), "fromEntries");
        Check.equal("true,false", Scripts.eval("Object.hasOwn({x:1},'x')+','+Object.hasOwn({x:1},'toString')"), "hasOwn");
    }

    /**
     * Checks relative indexing, flattening, mapping and holes.
     * <p>
     * 상대 인덱스, 평탄화, 매핑과 구멍을 검사한다.
     */
    public void testModernArrayMethods() {
        Check.equal("3,undefined,undefined", Scripts.eval("[1,2,3].at(-1)+','+[1].at(2)+','+[1].at(-2)"), "at");
        Check.equal("1,2,3,4", Scripts.eval("[1,[2,[3]],4].flat(Infinity).join()"), "flat");
        Check.equal("a0x,a0y,b1x,b1y", Scripts.eval("['a','b'].flatMap(function(v,i){return [v+i+'x',v+i+'y']}).join()"), "flatMap");
        Check.equal("1,1", Scripts.eval("var a=[,1];var n=0;a.flatMap(function(v,i){n++;return [v,i]});n+','+a.flatMap(function(v,i){return [i]}).length"), "holes");
        Check.equal("2,undefined", Scripts.eval("new Uint8Array([1,2]).at(-1)+','+new Uint8Array([1,2]).at(-3)"), "typed at");
    }

    /**
     * Checks String.at, replaceAll with strings and matchAll availability.
     * <p>
     * String.at, 문자열 replaceAll, matchAll 사용 가능성을 검사한다.
     */
    public void testModernStringMethods() {
        Check.equal("c,undefined", Scripts.eval("'abc'.at(-1)+','+'abc'.at(5)"), "String.at");
        Check.equal("x-b-x", Scripts.eval("'a-b-a'.replaceAll('a','x')"), "replaceAll string");
        Check.equal("-a-b-", Scripts.eval("'ab'.replaceAll('', '-')"), "replaceAll empty");
        Check.equal("TypeError", Scripts.eval("try{'a'.replaceAll(/a/,'b')}catch(e){e.name}"), "replaceAll non-global RegExp");
        Check.equal("function", Scripts.eval("typeof ''.matchAll"), "matchAll");
    }

    /**
     * Checks Symbol.description and the ES2022 matchAll well-known symbol.
     * <p>
     * Symbol.description 과 ES2022 Symbol.matchAll 잘 알려진 심볼을 검사한다.
     */
    public void testSymbolExtensions() {
        Check.equal("x,undefined,undefined,undefined,symbol", Scripts.eval("Symbol('x').description+','+Symbol().description+','+Symbol().description+','+String(Symbol().description)+','+typeof Symbol.matchAll"), "symbols");
    }

    /**
     * Checks ES2022 Error cause and AggregateError shape.
     * <p>
     * ES2022 Error cause 와 AggregateError 형태를 검사한다.
     */
    public void testModernErrors() {
        Check.equal("42,true", Scripts.eval("var e=new Error('x',{cause:42});e.cause+','+!Object.prototype.propertyIsEnumerable.call(e,'cause')"), "cause");
        Check.equal("AggregateError,2,1,2", Scripts.eval("var e=new AggregateError([1,2],'x');e.name+','+e.errors.length+','+e.errors.join()"), "aggregate");
    }

    /**
     * Checks Promise.prototype.finally and Promise.any/allSettled.
     * <p>
     * Promise.prototype.finally 와 Promise.any/allSettled 를 검사한다.
     */
    public void testModernPromises() {
        Check.equal("1,clean", Scripts.output("var s='';Promise.resolve(1).finally(function(){s='clean';}).then(function(v){print(v+','+s)})"), "finally");
        Check.equal("ok,ok\nfulfilled,1,rejected,e", Scripts.output("Promise.any([Promise.reject('e'),Promise.resolve('ok')]).then(function(v){print('ok,'+v)});"
                + "Promise.allSettled([Promise.resolve(1),Promise.reject('e')]).then(function(v){print(v[0].status+','+v[0].value+','+v[1].status+','+v[1].reason)})"), "promise results");
    }

    /**
     * Checks WeakRef, FinalizationRegistry, unregister and target validation.
     * <p>
     * WeakRef, FinalizationRegistry, 등록 해제와 대상 검증을 확인한다.
     */
    public void testWeakReferences() {
        Check.equal("true", Scripts.eval("var o={},r=new WeakRef(o);r.deref()===o"), "WeakRef.deref");
        Check.equal("TypeError", Scripts.eval("try{new WeakRef(1)}catch(e){e.name}"), "invalid target");
        Check.equal("true,false", Scripts.eval("var t={},u={},f=new FinalizationRegistry(function(){});f.register(t,'held',u);f.unregister(u)+','+f.unregister(u)"), "unregister");
        Check.equal("TypeError", Scripts.eval("var t={},f=new FinalizationRegistry(function(){});try{f.register(t,t)}catch(e){e.name}"), "target equals holdings");
    }

    /**
     * Checks that garbage collection clears WeakRef targets and schedules FinalizationRegistry cleanup on the realm queue.
     * <p>
     * GC 가 WeakRef 대상을 지우고 FinalizationRegistry 정리를 realm 큐에 넣는지 확인한다.
     */
    public void testWeakReferenceCollection() {
        Scripts.Capture capture = new Scripts.Capture();
        JsRealm realm = Scripts.realm(capture);
        realm.evaluateScript("var cleaned=0, lost=false; var weak=(function(){var t={x:1};return new WeakRef(t)})();"
                + "var registry=new FinalizationRegistry(function(value){if(value==='held') cleaned++;});"
                + "(function(){var target={value:1};registry.register(target,'held');})();", "weak.js");
        boolean collected = false;
        // GC 시점을 보장하지 않으므로 짧은 간격으로 여러 번 확인한다.
        for (int i = 0; i < 24; i++) {
            System.gc();
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            realm.runJobs();
            collected = "undefined,true".equals(Ops.toString(realm.evaluateScript(
                    "typeof weak.deref()+','+(cleaned === 1)", "poll.js")));
            if (collected) {
                break;
            }
        }
        Check.isTrue(collected, "WeakRef target and FinalizationRegistry cell are collected and cleaned");
    }

    /**
     * Confirms ES2019 stable Array.sort with equal comparison values.
     * <p>
     * ES2019 의 비교 값이 같은 Array.sort 안정 정렬을 확인한다.
     */
    public void testStableSort() {
        Check.equal("a1,b1,a2", Scripts.eval("[{v:1,n:'a1'},{v:1,n:'b1'},{v:1,n:'a2'}].sort(function(a,b){return 0}).map(function(x){return x.n}).join()"), "stable sort");
    }

    /**
     * Checks constructor and method names, lengths and standard attributes.
     * <p>
     * 생성자·메서드 이름, length 와 표준 속성을 검사한다.
     */
    public void testModernPropertyTables() {
        Check.equal("true", Scripts.eval("Object.entries.length===1&&Object.values.length===1&&Object.fromEntries.length===1"
                + "&&Object.getOwnPropertyDescriptors.length===1&&Object.hasOwn.length===2"
                + "&&Array.prototype.flat.length===0&&Array.prototype.flatMap.length===1&&Array.prototype.at.length===1"
                + "&&String.prototype.at.length===1&&String.prototype.replaceAll.length===2&&String.prototype.matchAll.length===1"
                + "&&Promise.allSettled.length===1&&Promise.any.length===1&&Promise.prototype.finally.length===1"), "lengths");
        Check.equal("true", Scripts.eval("WeakRef.length===1&&WeakRef.prototype.deref.length===0"
                + "&&FinalizationRegistry.length===1&&FinalizationRegistry.prototype.register.length===2"
                + "&&FinalizationRegistry.prototype.unregister.length===1&&AggregateError.length===2"), "weak and aggregate lengths");
        Check.equal("true", Scripts.eval("var d=Object.getOwnPropertyDescriptor(String.prototype,'replaceAll');"
                + "d.name===undefined&&d.writable&&d.configurable&&!d.enumerable"
                + "&&Object.getOwnPropertyDescriptor(Symbol.prototype,'description').set===undefined"), "attributes");
    }
}
