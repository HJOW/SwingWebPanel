package org.duckdns.hjow.swingwebpanel.js.builtins.datajson;

import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests the ECMAScript keyed collection methods added after ES2022.
 * <p>
 * ES2022 이후 추가된 ECMAScript 키 기반 컬렉션 메서드를 시험한다.
 */
public class Es2026CollectionMethodsTest {

    /**
     * Checks result allocation, canonical keys and the order of four Set-producing operations.
     * <p>
     * 집합을 반환하는 네 연산의 결과 할당, 키 정규화와 순서를 확인한다.
     */
    public void testSetAlgebraWithMap() {
        String script = "class Selected extends Set {}"
                + "const a=new Selected([3,1,NaN,-0,2]);const b=new Map([[2,7],[3,8],[5,9],[NaN,10]]);"
                + "const i=a.intersection(b), u=a.union(b), d=a.difference(b), x=a.symmetricDifference(b);"
                + "[... [i,u,d,x].map(s=>Array.from(s).join(',')),"
                + "[i,u,d,x].every(s=>Object.getPrototypeOf(s)===Set.prototype),"
                + "Object.is(Array.from(d)[1],0),Array.from(a).join()].join('|')";
        Check.equal("2,3,NaN|3,1,NaN,0,2,5|1,0|1,0,5|true|true|3,1,NaN,0,2", evaluate(script),
                "each operation preserves the required order and leaves its inputs intact");
    }

    /**
     * Checks that keys and next are obtained before the snapshot in symmetric difference and that duplicate keys do not toggle membership.
     * <p>
     * 대칭차집합의 사본을 만들기 전에 keys와 next를 얻고 중복 키가 포함 여부를 반복해서 바꾸지 않는지 검증한다.
     */
    public void testSetLikeKeyAcquisitionOrder() {
        String script = "const a=new Set([1,2,0]);let log='';"
                + "const b={get size(){log+='s';return {valueOf(){log+='v';return -0.5;}};},"
                + "get has(){log+='h';return ()=>{throw 'has';};}, get keys(){log+='k';return function(){"
                + "log+='K';a.add(8);const values=[2,2,4,4,-0];let i=0;"
                + "return {get next(){log+='n';a.add(9);return ()=>i<values.length?{value:values[i++],done:false}:{done:true};}};};}};"
                + "[Array.from(a.symmetricDifference(b)).join(),Array.from(a).join(),log].join('|')";
        Check.equal("1,8,9,4|1,2,0,8,9|svhkKn", evaluate(script), "captures properties, then calls keys before taking the snapshot");
    }

    /**
     * Compares live iteration in intersection with the copied candidates in difference.
     * <p>
     * 교집합의 실시간 순회와 차집합에서 사본의 후보를 읽는 동작을 비교한다.
     */
    public void testSetMutationDuringMembershipCalls() {
        String script = "const a=new Set([1,2]);const trace=[];let changed=false;"
                + "const b={size:10,has(v){trace.push(v);if(!changed){changed=true;a.delete(2);a.add(3);a.delete(1);a.add(1);}"
                + "return this===b;},keys(){throw 'keys';}};"
                + "const i=a.intersection(b);"
                + "const c=new Set([1,2]), checked=[];"
                + "const d=c.difference({size:10,has(v){checked.push(v);c.clear();c.add(3);return v===1;},keys(){throw 'keys';}});"
                + "const e=new Set([5,6]);const f=e.difference({size:1,has(){throw 'has';},"
                + "keys(){e.clear();return [6].values();}});"
                + "[Array.from(i).join(),trace.join(),Array.from(a).join(),Array.from(d).join(),checked.join(),"
                + "Array.from(c).join(),Array.from(f).join(),e.size].join('|')";
        Check.equal("1,3|1,3,1|3,1|2|1,2|3|5|0", evaluate(script),
                "new entries are visible in live traversal and do not affect a difference snapshot");
    }

    /**
     * Validates size, method descriptors and the receiver before inspecting the other operand.
     * <p>
     * 크기와 메서드 기술자를 검증하고 다른 피연산자보다 먼저 수신 객체를 검사한다.
     */
    public void testSetRecordValidation() {
        String script = "let log='';function fail(f){try{f();return 'missing';}catch(e){return e.name;}}"
                + "const a=new Set([1]);const getter={get size(){log+='bad';return 0;}};"
                + "const noHas={size:0,has:0,get keys(){log+='unexpected';return ()=>[1].values();}};"
                + "const badKeys={size:0,has(){},keys(){return 1;}};"
                + "const d=Object.getOwnPropertyDescriptor(Set.prototype,'union');"
                + "[fail(()=>Set.prototype.union.call({},getter)),fail(()=>a.union(2)),fail(()=>a.union({})),"
                + "fail(()=>a.union({size:-1})),fail(()=>a.union(noHas)),fail(()=>a.union(badKeys)),"
                + "fail(()=>a.intersection({size:Infinity,has(){return true;}})),log,"
                + "d.value.name,d.value.length,d.writable,d.enumerable,d.configurable].join('|')";
        Check.equal("TypeError|TypeError|TypeError|RangeError|TypeError|TypeError|TypeError||union|1|true|false|true",
                evaluate(script), "Set-like operand access order and built-in metadata");
    }

    /**
     * The set predicates close a partially consumed keys iterator and report failures from its return method.
     * <p>
     * 집합 조건 검사는 일부만 소비한 키 이터레이터를 닫고 return 메서드의 오류를 전파한다.
     */
    public void testSetPredicatesCloseEarly() {
        String script = "const a=new Set([1,2]);let log='';function fail(f){try{f();return 'missing';}catch(e){return e.name||e;}}"
                + "function other(v,name,mode){return {size:1,has(){throw 'has';},keys(){log+=name;"
                + "const it={next(){log+='n';return {value:v,done:false};}, return(){"
                + "log+=this===it && arguments.length===0?'r':'!';"
                + "if(mode===1)throw 'return';if(mode===2)return 42;return {done:true};}};return it;}};}"
                + "const s=a.isSupersetOf(other(3,'S',0));const d=a.isDisjointFrom(other(1,'D',0));"
                + "const thrown=fail(()=>a.isDisjointFrom(other(2,'T',1)));"
                + "const type=fail(()=>a.isSupersetOf(other(4,'P',2)));"
                + "const big={get size(){log+='s';return 3;}, get has(){log+='h';return ()=>true;},"
                + "get keys(){log+='k';return ()=>{throw 'called';};}};"
                + "[s,d,thrown,type,a.isSupersetOf(big),log].join('|')";
        Check.equal("false|false|return|TypeError|false|SnrDnrTnrPnrshk", evaluate(script),
                "early normal exits close the iterator and preserve any return error");
    }

    /**
     * The size checks select live receiver traversal or a keys iterator according to the operation.
     * <p>
     * 크기 검사 결과에 따라 연산별로 수신 집합의 현재 키나 상대의 키 이터레이터를 순회한다.
     */
    public void testSetPredicateSizesAndLiveMembership() {
        String script = "const a=new Set([1]);const seen=[];"
                + "const b={size:3,has(v){seen.push(v);a.add(2);return v===1;},keys(){throw 'keys';}};"
                + "const subset=a.isSubsetOf(b);"
                + "const small={size:0,has(){throw 'has';},keys(){throw 'keys';}};"
                + "const large={size:2,has(v){seen.push(-v);return v===1;},keys(){throw 'keys';}};"
                + "const c=new Set([1,2,3]);"
                + "const dynamic={size:1,has(){throw 'has';},keys(){return {next(){c.add(4);return {done:false,value:4};},return(){return {};}};}};"
                + "[subset,a.isSubsetOf(small),a.isDisjointFrom(large),c.isDisjointFrom(dynamic),seen.join(),"
                + "a.isSubsetOf(new Set([1,2,3])),a.isSupersetOf(new Map([[2,7]])),a.isDisjointFrom(new Set([4]))].join('|')";
        Check.equal("false|false|false|false|1,2,-1|true|true|true", evaluate(script),
                "predicates update membership after observable calls and skip unnecessary methods");
    }

    /**
     * Evaluates a test script and converts the result to text.
     * <p>
     * 시험 스크립트를 평가한 뒤 결과를 문자열로 바꾼다.
     *
     * @param script script source / 스크립트 소스
     * @return result text / 결과 문자열
     */
    private static String evaluate(String script) {
        return Ops.toString(JsRealm.create().evaluateScript(script, "collection-methods.js"));
    }
}
