package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Checks choices made by the latest edition where older ECMAScript versions differ.
 * <p>
 * 이전 ECMAScript 판본과 동작이 다른 부분에서 최신 판본의 의미를 검증한다.
 */
public class EditionTransitionTest {

    /**
     * Checks numeric strings, current whitespace classification, and Reflect's current property set.
     * <p>
     * 숫자 문자열, 현재 공백 문자 분류와 최신 Reflect 프로퍼티를 확인한다.
     */
    public void testNumericAndReflectChanges() {
        String script = "const oldSpace=String.fromCharCode(0x180E);"
                + "[Number('0b10'),Number('0O10'),Number.isNaN(Number('+0b1')),Number.isNaN(Number('-0o1')),"
                + "oldSpace.trim().length,/\\s/u.test(oldSpace),Number.isNaN(+oldSpace),"
                + "Object.is(Number(' -0 '),-0),typeof Reflect.enumerate,Object.getOwnPropertyNames(Reflect).length,"
                + "Object.prototype.toString.call(Reflect)].join('|')";
        Check.equal("2|8|true|true|1|false|true|true|undefined|13|[object Reflect]", Scripts.eval(script),
                "latest string conversions and the current Reflect surface");
    }

    /**
     * Verifies identifier and statement parsing changes and an immutable function name's runtime error.
     * <p>
     * 식별자와 문장 파싱 변경 및 변경 불가 함수 이름의 런타임 오류를 검증한다.
     */
    public void testGrammarAndFunctionShape() {
        String script = "function error(work){try{work();return 'none';}catch(e){return e.name;}}"
                + "const check=Function(\"'use strict';return function self(){self=1;}\")();"
                + "const d=Object.getOwnPropertyDescriptor(function f(a,b){},'length');"
                + "[d.value,d.configurable,d.writable,d.enumerable,Object.keys('a').join(),Object.keys(12).length,"
                + "Function(\"'use strict';return ({p:1,p:2}).p;\")(),"
                + "Function('let\\nname=7;return name;')(),Function('var i=0;do{i++;}while(false) i++;return i;')(),"
                + "error(check),error(()=>Object.keys(null))].join('|')";
        Check.equal("2|true|false|false|0|0|2|7|2|TypeError|TypeError", Scripts.eval(script),
                "current grammar and descriptors replace the older edition rules");
    }

    /**
     * Checks Date and RegExp prototype brands, line-terminator escaping, and current sort conversion.
     * <p>
     * Date와 RegExp 프로토타입 브랜드, 줄 끝 이스케이프 및 최신 정렬 변환을 확인한다.
     */
    public void testDateRegExpAndComparatorChanges() {
        String script = "function err(work){try{work();return 'none';}catch(e){return e.name;}}"
                + "const bs=String.fromCharCode(92), r=new RegExp('\\n\\r'+String.fromCharCode(0x2028,0x2029));"
                + "const sorted=[3,1,2].sort((a,b)=>({valueOf(){return a-b;}}));"
                + "[Object.is(new Date(-0).getTime(),0),String(new Date(NaN)),err(()=>new Date(NaN).toISOString()),"
                + "err(()=>Date.prototype.getTime()),err(()=>RegExp.prototype.exec('')),"
                + "RegExp.prototype.source,r.source===bs+'n'+bs+'r'+bs+'u2028'+bs+'u2029',"
                + "'ab'.match(/a|b/g).join(), 'ab'.replace(/(?:)/g,'.'),"
                + "sorted.join(),[2,1,3].sort(()=>NaN).join()].join('|')";
        Check.equal("true|Invalid Date|RangeError|TypeError|TypeError|(?:)|true|a,b|.a.b.|1,2,3|2,1,3",
                Scripts.eval(script), "Date, RegExp and sort behavior under the latest edition");
    }

    /**
     * Checks that an array length update observes length changes caused by argument conversion.
     * <p>
     * 배열 길이 갱신이 인수 변환 중 일어난 길이 변경을 반영하는지 검사한다.
     */
    public void testArrayLengthAfterConversion() {
        String script = "const a=[1,2,3];let calls=0;"
                + "const length={valueOf(){if(++calls===1)a[4]=5;return 2;}};"
                + "Object.defineProperty(a,'length',{value:length});"
                + "[calls,a.length,4 in a,a.join()].join('|')";
        Check.equal("2|2|false|1,2", Scripts.eval(script), "array shrinking uses the length after both conversions");
    }
}
