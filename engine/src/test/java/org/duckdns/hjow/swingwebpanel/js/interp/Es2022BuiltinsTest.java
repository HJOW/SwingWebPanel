package org.duckdns.hjow.swingwebpanel.js.interp;

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
}
