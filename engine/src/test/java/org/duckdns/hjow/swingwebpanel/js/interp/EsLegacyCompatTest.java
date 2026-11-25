package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Runs ES1 through ES5.1 coding idioms alongside current ECMAScript syntax and built-ins.
 * <p>
 * ES1부터 ES5.1까지 사용한 코드 관용구를 현재 ECMAScript 문법과 내장 객체와 함께 실행한다.
 */
public final class EsLegacyCompatTest {

    /** Creates the suite. / 시험 묶음을 만든다. */
    public EsLegacyCompatTest() {
    }

    /**
     * Verifies declaration instantiation and the environments used by eval and Function.
     * <p>
     * 선언 인스턴스화와 eval 및 Function이 쓰는 환경을 검증한다.
     */
    public void testHoistingAndEvalEnvironments() {
        Check.equal("undefined,4,5,5,undefined,undefined,19,outer", Scripts.eval(
                "var word='outer', log=[];"
                + "function legacy(arg){"
                + "log.push(typeof later,hoisted());if(false)var later=1;function hoisted(){return arg+1;}"
                + "var first=eval('var introduced=arg+2;introduced');"
                + "var absent=(0,eval)('typeof introduced');"
                + "eval(\"'use strict';var hidden=99;\");log.push(first,introduced,absent,typeof hidden);"
                + "(0,eval)('var installed=19');var word='inner';log.push(installed,Function('return word')());"
                + "}legacy(3);log.join()"),
                "sloppy direct eval uses the caller, strict eval owns its declarations, and indirect code sees globals");
    }

    /**
     * Verifies mapping of duplicate simple parameters and the ordinary callee property of the legacy arguments object.
     * <p>
     * 중복 단순 매개변수의 매핑과 레거시 arguments의 callee 프로퍼티를 확인한다.
     */
    public void testMappedArgumentsAndCallee() {
        Check.equal("1,2,2,true,1,7,7,11,13,true,17|true|3,4,TypeError", Scripts.eval(
                "function old(a,a){var log=[arguments[0],arguments[1],a,arguments.callee===old];"
                + "a=7;log.push(arguments[0],arguments[1]);arguments[0]=9;log.push(a);"
                + "Object.defineProperty(arguments,'1',{writable:false,value:11});"
                + "a=13;log.push(arguments[1],a);delete arguments[1];a=17;log.push(arguments[1]===undefined,a);"
                + "return log.join();}"
                + "function descriptor(){var d=Object.getOwnPropertyDescriptor(arguments,'callee');"
                + "return d.value===descriptor&&d.writable&&!d.enumerable&&d.configurable;}"
                + "function newer(a=1){a=4;try{arguments.callee;}catch(e){return [arguments[0],a,e.name].join();}}"
                + "[old(1,2),descriptor(),newer(3)].join('|')"),
                "mapping stops after a descriptor update and only applies to sloppy simple parameters");
    }

    /**
     * Verifies legacy with lookup, including prototype traversal and current Symbol.unscopables.
     * <p>
     * 프로토타입 조회와 현재 Symbol.unscopables를 포함한 레거시 with 탐색을 검증한다.
     */
    public void testWithUnscopablesAndPrototypeLookup() {
        Check.equal("inherited:lexical:41|after:true|object|SyntaxError", Scripts.eval(
                "var value='lexical', named='outer', total=40, log=[];"
                + "var scope=Object.create({value:'object',named:'inherited',set mark(v){this.result=v}});"
                + "scope[Symbol.unscopables]=Object.create({value:true});"
                + "with(scope){mark='after';++total;log.push(named+':'+value+':'+total);}"
                + "log.push(scope.result+':'+!Object.prototype.hasOwnProperty.call(scope,'mark'));"
                + "scope[Symbol.unscopables]=true;with(scope){log.push(value);}"
                + "try{Function(\"'use strict';with({}){}\");}catch(e){log.push(e.name);} log.join('|')"),
                "with reads inherited properties, keeps the object receiver, and skips unscopable names");
    }

    /**
     * Mixes old constructor prototypes, sparse Array methods and legacy built-ins with modern data APIs.
     * <p>
     * 구판 생성자 프로토타입, 희소 배열 메서드, 레거시 내장 객체를 현대 데이터 API와 함께 확인한다.
     */
    public void testLegacyBuiltinsMixedWithCurrentCode() {
        Check.equal("true,true,15,12,9,bc,%uAC00,가,2026", Scripts.eval(
                "function Base(n){this.seed=n;}"
                + "Base.prototype.step=function(x){return this.seed+x;};"
                + "function Old(n){Base.call(this,n);}Old.prototype=Object.create(Base.prototype);"
                + "Object.defineProperty(Old.prototype,'constructor',{value:Old});"
                + "var old=new Old(4), sparse=[1,,3];"
                + "const scores=sparse.map(value=>old.step(value)), named=new Map([['total',scores.reduce((a,b)=>a+b,0)]]);"
                + "const data=JSON.parse(JSON.stringify(Object.fromEntries(named))), [first, , last]=scores;"
                + "[old instanceof Base,Object.getPrototypeOf(old)===Old.prototype,first+last+scores.length,"
                + "data.total,Math.max(...scores.filter(Boolean))+2,'abcde'.substr(-4,2),escape('가'),"
                + "unescape('%uAC00'),new Date(Date.UTC(2026,0,1)).getUTCFullYear()].join()"),
                "the old object model and built-ins work alongside destructuring, Map, and Object.fromEntries");
    }
}
