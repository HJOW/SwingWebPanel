package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of statements and completion records (plan item P13-04).
 * <p>
 * 문장과 완료 레코드 시험(계획 항목 P13-04).
 */
public class ControlFlowTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public ControlFlowTest() {
    }

    /**
     * Every statement kind and the completion value rules.
     * <p>
     * 모든 문장 종류와 완료 값 규칙.
     */
    public void testStatements() {
        Scripts.checkResource("control.js", 100);
    }

    /**
     * Checks integer key order, prototype order and shadowed or deleted properties.
     * <p>
     * 정수 키 순서, 프로토타입 순서, 가려지거나 삭제된 프로퍼티를 확인한다.
     */
    public void testForInOrder() {
        Check.equal("2,10,z,01,3,b", Scripts.eval(
                "var p={b:1, shadow:1, 3:1, z:1}, o=Object.create(p);"
                + " o.z=1; o[10]=1; o[2]=1; o['01']=1; o[Symbol('hidden')]=1;"
                + " Object.defineProperty(o,'shadow',{value:1});"
                + " var result=[]; for (var k in o) result.push(k); result.join()"), "own keys before prototype keys");
        Check.equal("a,c", Scripts.eval(
                "var o={a:1,b:2,c:3}; function* enumerate(){for(var k in o) yield k;}"
                + " var it=enumerate(), first=it.next().value; delete o.b;"
                + " first+','+it.next().value+(it.next().done ? '' : ',extra')"), "discard a deleted key after resuming");
    }

    /**
     * Checks the proxy operations used by for-in and suppression of symbol keys.
     * <p>
     * for-in이 호출하는 프록시 연산과 심볼 키 제외를 확인한다.
     */
    public void testForInProxy() {
        Check.equal("b,c|keys,b,c,missing,proto", Scripts.eval(
                "var log=[], seen=[], symbol=Symbol('hidden');"
                + " var proxy=new Proxy({b:1,c:1},{"
                + " ownKeys:function(){log.push('keys');return ['b',symbol,'c','missing'];},"
                + " getOwnPropertyDescriptor:function(t,k){log.push(String(k));return Reflect.getOwnPropertyDescriptor(t,k);},"
                + " getPrototypeOf:function(){log.push('proto');return null;},"
                + " has:function(){throw new Error('has trap');},"
                + " enumerate:function(){throw new Error('enumerate trap');}});"
                + " for (var k in proxy) seen.push(k); seen.join()+'|'+log.join()"),
                "own keys and descriptors determine enumeration");
    }

    /**
     * Uncaught throw of any value surfaces to the host as an exception that carries the thrown value.
     * <p>
     * 잡히지 않은 throw 는 던진 값을 담은 예외로 호스트에 전달된다.
     */
    public void testUncaughtThrowReachesHost() {
        Check.equal("Error: boom", Scripts.thrown("throw new Error('boom')"), "error object text");
        Check.contains(Scripts.thrown("throw 5"), "5", "primitive thrown value text");
        Check.contains(Scripts.thrown("throw 'text'"), "text", "string thrown value text");
        Check.contains(Scripts.thrown("function f() { f2(); } function f2() { throw new RangeError('r'); } f()"), "RangeError: r", "nested throw");
    }
}
