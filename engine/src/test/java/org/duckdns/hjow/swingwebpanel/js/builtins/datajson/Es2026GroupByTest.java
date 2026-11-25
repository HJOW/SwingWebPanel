package org.duckdns.hjow.swingwebpanel.js.builtins.datajson;

import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Exercises Object.groupBy and Map.groupBy through the iterator and key conversion protocols.
 * <p>
 * 이터레이터와 키 변환 규칙을 통해 Object.groupBy와 Map.groupBy를 검증한다.
 */
public class Es2026GroupByTest {

    /**
     * Object groups use property keys, ordinary writable properties, and a null prototype.
     * <p>
     * 객체 그룹은 프로퍼티 키, 쓸 수 있는 일반 프로퍼티와 null 프로토타입을 사용한다.
     */
    public void testObjectGroupByPropertyKeys() {
        String script = "const symbol=Symbol('group');const log=[];"
                + "const coercible={ [Symbol.toPrimitive](hint){log.push(hint);return '__proto__';} };"
                + "const keys=[symbol,coercible,7,symbol,7n];"
                + "const g=Object.groupBy.call(null,'abcde',function(v,i){'use strict';log.push(this===undefined && v+i);return keys[i];});"
                + "const d=Object.getOwnPropertyDescriptor(g,'__proto__');"
                + "[Object.getPrototypeOf(g)===null,Object.keys(g).join(),Object.getOwnPropertySymbols(g)[0]===symbol,"
                + "g[symbol].join(),g[7].join(),g.__proto__.join(),d.enumerable,d.configurable,d.writable,log.join(),"
                + "Object.groupBy.name,Object.groupBy.length].join('|')";
        Check.equal("true|7,__proto__|true|a,d|c,e|b|true|true|true|a0,b1,string,c2,d3,e4|groupBy|2", evaluate(script),
                "groups use ToPropertyKey in source order");
    }

    /**
     * Map groups retain object identity and canonicalize zero and NaN keys.
     * <p>
     * Map 그룹은 객체 동일성을 유지하고 0과 NaN 키를 정규화한다.
     */
    public void testMapGroupByCollectionKeys() {
        String script = "const identity={toString(){throw 'unexpected';}}, different={};"
                + "const keys=[NaN,-0,identity,0,NaN,different]; class S extends Map {}"
                + "const m=S.groupBy(['a','b','c','d','e','f'],function(v,i){return keys[i];});"
                + "const ordered=Array.from(m.keys());"
                + "[Object.getPrototypeOf(m)===Map.prototype,m.size,Number.isNaN(ordered[0]),Object.is(ordered[1],0),"
                + "ordered[2]===identity,ordered[3]===different,m.get(NaN).join(),m.get(-0).join(),m.get(identity)[0],"
                + "Map.groupBy.name,Map.groupBy.length].join('|')";
        Check.equal("true|4|true|true|true|true|a,e|b,d|c|groupBy|2", evaluate(script),
                "Map allocation and keys are independent from the this value");
    }

    /**
     * Iteration captures next once and preserves the original failure while invoking return on callback and key errors.
     * <p>
     * 순회는 next를 한 번만 읽고 콜백이나 키 오류에서 return을 호출하더라도 원래 오류를 유지한다.
     */
    public void testIteratorAcquisitionAndAbruptClose() {
        String script = "let log='';function fail(f){try{f();return 'missing';}catch(e){return e.name||e;}}"
                + "function make(){let i=0;return {[Symbol.iterator](){log+='i';return {"
                + "get next(){log+='n';return ()=>({value:++i,done:i>2});}, return(){log+='r';throw 'return';}};}};}"
                + "const a=Map.groupBy(make(),()=>true).get(true).join();"
                + "const b=fail(()=>Map.groupBy(make(),()=>{throw 'callback';}));"
                + "const c=fail(()=>Object.groupBy(make(),()=>({toString(){throw 'key';}})));"
                + "const bad={[Symbol.iterator](){return {next(){return {get value(){throw 'step';},done:false};},"
                + "return(){log+='!';return {};}};}};"
                + "const d=fail(()=>Object.groupBy(bad,()=>1));"
                + "const invalid=fail(()=>Map.groupBy(make(),0));"
                + "[a,b,c,d,invalid,log,fail(()=>Object.groupBy(null,()=>1))].join('|')";
        Check.equal("1,2|callback|key|step|TypeError|ininrinr|TypeError", evaluate(script),
                "caller failures close the iterator but an iterator step failure ends it directly");
    }

    /**
     * Runs a script in an independent realm and converts its result for comparison.
     * <p>
     * 독립 realm에서 스크립트를 실행한 뒤 결과를 비교용으로 변환한다.
     *
     * @param script the test source / 시험 소스
     * @return result as text / 문자열 결과
     */
    private static String evaluate(String script) {
        return Ops.toString(JsRealm.create().evaluateScript(script, "group-by.js"));
    }
}
