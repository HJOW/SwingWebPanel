package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests the JSON raw token and source-aware parsing operations.
 * <p>
 * JSON 원문 토큰과 원문 정보를 사용하는 파싱 연산을 검증한다.
 */
public class Es2026RawJsonTest {

    /**
     * Checks primitive token validation, preserved text, and coercion errors.
     * <p>
     * 원시 토큰의 검증과 원문 보존, 강제 변환 오류를 확인한다.
     */
    public void testRawTokenSyntax() {
        String script = "const raw=JSON.rawJSON, show=JSON.stringify, bs=String.fromCharCode(92);"
                + "const esc='\"'+bs+'uD800\"', lone=String.fromCharCode(34,0xD800,34);"
                + "const bad=['',' ',' 1','1 ','\\nnull','true\\t','+1','012','01.2','1.','1e','NaN','Infinity',"
                + "'[]','{}','[1]','{\"a\":1}','true false','\"a\"\"b\"','\"'+bs+'uD80Z\"','\"'+bs+'uD80\"'];"
                + "let conversions=0;"
                + "function error(f){try{f();return '-';}catch(e){return e.name;}}"
                + "[bad.every(x=>error(()=>raw(x))==='SyntaxError'),raw(null).rawJSON, raw(true).rawJSON,"
                + "raw(12n).rawJSON,raw(-0).rawJSON,show(raw('-0')),show(raw('1E+0400')),show(raw(esc))===esc,"
                + "show(raw(lone))===lone,raw({toString(){conversions++;return '2e1';}}).rawJSON,conversions,"
                + "error(()=>raw(Symbol())),error(()=>raw())].join('|')";
        Check.equal("true|null|true|12|0|-0|1E+0400|true|true|2e1|1|TypeError|SyntaxError",
                Scripts.eval(script), "raw tokens follow JSON grammar and retain exact source code units");
    }

    /**
     * Checks the null prototype, frozen rawJSON property, and internal brand.
     * <p>
     * null 프로토타입, 동결한 rawJSON 프로퍼티와 내부 브랜드를 확인한다.
     */
    public void testRawTokenBrandAndShape() {
        JsRealm from = JsRealm.create();
        JsRealm to = JsRealm.create();
        to.getGlobalObject().defineBuiltin("foreign", from.evaluateScript("JSON.rawJSON('9007199254740993')", "raw-source.js"));
        String script = "let gets=0;const r=foreign,d=Object.getOwnPropertyDescriptor(r,'rawJSON');"
                + "const p=Proxy.revocable(r,{get(){gets++;throw 'get';}});"
                + "const before=JSON.isRawJSON(p.proxy);p.revoke();"
                + "[JSON.isRawJSON(r),before,JSON.isRawJSON(p.proxy),gets,Object.isFrozen(r),Object.getPrototypeOf(r)===null,"
                + "d.writable,d.enumerable,d.configurable,Object.keys(r).join(),JSON.isRawJSON(Object.create(r)),"
                + "JSON.isRawJSON({rawJSON:d.value}),JSON.isRawJSON(d.value),JSON.stringify(r)].join('|')";
        Check.equal("true|false|false|0|true|true|false|true|false|rawJSON|false|false|false|9007199254740993",
                Ops.toString(to.evaluateScript(script, "raw-target.js")), "raw JSON brand is independent of prototypes and realms");
    }

    /**
     * Checks raw insertion after toJSON and replacer and preservation through nested indentation.
     * <p>
     * toJSON과 replacer 이후 단일 토큰으로 원문을 삽입하고 들여쓰기 안에서도 유지하는지 검사한다.
     */
    public void testRawTokenStringifyHooks() {
        String script = "'use strict';const calls=[],raw=JSON.rawJSON;"
                + "const payload={a:{toJSON(k){calls.push('to:'+k);return raw('1.2300e+5');}},b:0,c:[9007199254740993n]};"
                + "BigInt.prototype.toJSON=function(k){calls.push('big:'+k+':'+typeof this);return raw(String(this));};"
                + "const output=JSON.stringify(payload,function(k,v){"
                + "if(k==='a'){calls.push('raw:'+JSON.isRawJSON(v));return v;}"
                + "if(k==='b')return raw('-0');return v;},2);"
                + "const overridden=JSON.stringify({x:raw('true')},(k,v)=>k==='x'?'normal':v);"
                + "[output,calls.join(),overridden,JSON.stringify([raw('false'),raw('null'),raw('\"quoted\"')])].join('|')";
        Check.equal("{\n  \"a\": 1.2300e+5,\n  \"b\": -0,\n  \"c\": [\n    9007199254740993\n  ]\n}"
                + "|to:a,raw:true,big:0:bigint|{\"x\":\"normal\"}|[false,null,\"quoted\"]",
                Scripts.eval(script), "raw serialization uses the value after the ordinary JSON hooks");
    }
}
