package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Exercises the ECMA-262 locale fallbacks through the script entry point.
 * <p>
 * 스크립트 진입점으로 ECMA-262 기본 지역화 메서드를 검증한다.
 */
public final class LocaleFallbackTest {

    /** Creates the test suite. / 시험 묶음을 만든다. */
    public LocaleFallbackTest() {
    }

    /**
     * Tests Unicode 18 canonical equivalence and operand coercion before comparison.
     * <p>
     * Unicode 18의 정준 동치와 비교 전 피연산자 변환 순서를 검증한다.
     */
    public void testStringComparison() {
        Check.equal("undefined|left,right|true|true|true|true|TypeError|stop", Scripts.eval(
                "var log=[], out=[], compare=String.prototype.localeCompare;"
                + "var spare={toString(){throw 'reserved';}};"
                + "var a='\\u{11938}', b='\\u{11935}\\u{11930}';"
                + "var order=compare.call({toString(){log.push('left');return a;}},"
                + "{toString(){log.push('right');return b;}},spare,spare);"
                + "out.push(typeof Intl,log.join(),Object.is(order,0),Object.is(b.localeCompare(a),0),"
                + "Object.is(('\\ud800'+a).localeCompare('\\ud800'+b),0),'a'.localeCompare('b')<0);"
                + "try{compare.call(null,{toString(){throw 'right';}});}catch(e){out.push(e.name);}"
                + "try{compare.call({toString(){throw 'stop';}},{toString(){throw 'right';}});}catch(e){out.push(e);}"
                + "out.join('|');"), "canonical equivalents match after ordered coercion");
        Check.equal("I,\u0130,\uD801\uDC00", Scripts.eval("var x={toString(){throw 'reserved';}};"
                + "['i'.toLocaleUpperCase(x),'\\u0130'.toLocaleUpperCase(x),'\\u{10428}'.toLocaleUpperCase(x)].join();"),
                "case mapping uses the default Unicode data and ignores extra arguments");
    }

    /**
     * Checks numeric and date formatting, receiver branding and ignored reserved arguments.
     * <p>
     * 숫자·날짜 서식, 수신 객체의 내부 타입, 예약 인수 생략을 확인한다.
     */
    public void testNumberBigIntAndDate() {
        Check.equal("1,234.568|-0|NaN|∞|-∞|-12345678901234567890|Invalid Date|Invalid Date|Invalid Date|true|"
                + "TypeError|TypeError|TypeError", Scripts.eval(
                "var spare={toString(){throw 'reserved';},get year(){throw 'reserved';}};"
                + "var number=Number.prototype.toLocaleString, big=BigInt.prototype.toLocaleString;"
                + "var date=Date.prototype.toLocaleString, invalid=new Date(NaN), valid=new Date(0);"
                + "var out=[number.call(new Number(1234.5678),spare,spare),(-0).toLocaleString(spare),"
                + "NaN.toLocaleString(spare),Infinity.toLocaleString(spare),(-Infinity).toLocaleString(spare),"
                + "big.call(Object(-12345678901234567890n),spare,spare),invalid.toLocaleString(spare,spare),"
                + "invalid.toLocaleDateString(spare,spare),invalid.toLocaleTimeString(spare,spare),"
                + "valid.toLocaleString(spare,spare)===valid.toLocaleDateString()+', '+valid.toLocaleTimeString()];"
                + "[number,big,date].forEach(f=>{try{f.call({valueOf(){throw 'wrong';}},spare);}"
                + "catch(e){out.push(e.name);}});out.join('|');"),
                "fallbacks ignore options and check internal receiver slots");
    }

    /**
     * Checks generic calls, sparse elements, fixed lengths and abrupt completion.
     * <p>
     * 범용 호출, 희소 원소, 길이 확보와 갑작스러운 완료를 검증한다.
     */
    public void testObjectArrayAndTypedArray() {
        Check.equal("true|5,,|length,0,method,2|0|1,|0|TypeError|stop|length,0", Scripts.eval(
                "var a=Array.prototype.toLocaleString, obj=Object.prototype.toLocaleString;"
                + "var reserved={toString(){throw 'reserved';}}, marker={},log=[], counts=[];"
                + "var o={toString(){return this===o&&arguments.length===0?marker:undefined;}};"
                + "var entry={toLocaleString(){counts.push(arguments.length);return this===entry?5:0;}};"
                + "var view={get length(){log.push('length');return 3;},get 0(){log.push('0');return entry;},"
                + "get 2(){log.push('2');return null;}};"
                + "var fun=entry.toLocaleString;"
                + "Object.defineProperty(entry,'toLocaleString',{get(){log.push('method');return fun;}});"
                + "var out=[obj.call(o,reserved,reserved)===marker,a.call(view,reserved,reserved),log.join(),counts.join()];"
                + "var buffer=new ArrayBuffer(2,{maxByteLength:4}), typed=new Uint8Array(buffer);typed[0]=1;"
                + "Number.prototype.toLocaleString=function(){'use strict';counts=[arguments.length];buffer.resize(1);return this;};"
                + "out.push(typed.toLocaleString(reserved,reserved),counts.join());"
                + "buffer.transfer();try{typed.toLocaleString(reserved);}catch(e){out.push(e.name);}"
                + "log=[];try{a.call({get length(){log.push('length');return 2;},"
                + "get 0(){log.push('0');return {toLocaleString(){throw 'stop';}};},get 1(){log.push('late');}});}"
                + "catch(e){out.push(e,log.join());}out.join('|');"),
                "element calls have zero arguments, retain receivers and stop at the first exception");
    }
}
