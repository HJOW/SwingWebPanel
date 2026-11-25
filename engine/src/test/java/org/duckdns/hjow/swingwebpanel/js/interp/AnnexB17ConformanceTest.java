package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Verifies Annex B behavior in the seventeenth edition of ECMA-262.
 * <p>
 * ECMA-262 제17판 부속서 B의 동작을 검증한다.
 */
public class AnnexB17ConformanceTest {

    /**
     * Checks one-time initialization, ordering, and binding name inference in sloppy for-in.
     * <p>
     * 비엄격 for-in의 한 번만 하는 초기화, 평가 순서 및 바인딩 이름 추론을 확인한다.
     */
    public void testForInVarInitializerEvaluation() {
        String script = "const log=[];"
                + "for(var key=(log.push(typeof key),log.push('init'),42) in (log.push('rhs:'+key),{a:1,b:1})){"
                + "log.push(key);}"
                + "for(var named=function(){} in null)throw 'unreachable';"
                + "for(var number=(log.push('empty'),5) in undefined)throw 'unreachable';"
                + "try{for(var abrupt=(()=>{throw 'init error';})() in (log.push('unexpected'),{})){} }"
                + "catch(e){log.push(e);}"
                + "[log.join(),key,named.name,number].join('|')";
        Check.equal("undefined,init,rhs:42,a,b,empty,init error|b|named|5", Scripts.eval(script),
                "initializer runs once before enumeration even when the object is nullish");
    }

    /**
     * Checks that the rule is restricted to non-strict for-in with a single var binding identifier.
     * <p>
     * 비엄격 for-in의 단일 var 식별자에만 규칙이 적용되는지 확인한다.
     */
    public void testForInVarInitializerEarlyErrors() {
        String script = "function invalid(s){try{Function(s);return false;}catch(e){return e.name==='SyntaxError';}}"
                + "[\"'use strict';for(var x=0 in {}){}\",\"for(let x=0 in {}){}\",\"for(const x=0 in {}){}\","
                + "\"for(var x=0 of []){}\",\"for(var {x}={} in {}){}\",\"for(var [x]=[] in {}){}\","
                + "\"for(var x=0,y in {}){}\"].every(invalid)+':'"
                + "+Function('for(var x=0 in {}){}return x;')()";
        Check.equal("true:0", Scripts.eval(script), "initializer allowance applies only to the specified grammar");
    }

    /**
     * Verifies that suspension preserves the resolved binding and the initialized state.
     * <p>
     * 실행을 중단했다 재개해도 결정한 바인딩과 초기화 상태가 유지되는지 확인한다.
     */
    public void testForInVarInitializerSuspension() {
        String script = "const base={key:'base'},events=[];"
                + "function* run(){var key='outer';with(base){"
                + "for(var key=(events.push('init'),yield 'first') in (events.push('rhs:'+key),yield 'second')){events.push(key);}"
                + "}return key;}"
                + "const flow=run(),a=flow.next();delete base.key;const b=flow.next('assigned'),binding=base.key;"
                + "const c=flow.next({one:1});"
                + "[a.value,b.value,binding,c.value,c.done,base.key,events.join()].join('|')";
        Check.equal("first|second|assigned|outer|true|one|init,rhs:assigned,one", Scripts.eval(script),
                "binding resolution and initialization precede a resumable source expression");
    }
}
