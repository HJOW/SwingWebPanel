package org.duckdns.hjow.swingwebpanel.js.builtins.stringarray;

import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Verifies the UTF-16 well-formedness methods exposed on String.prototype.
 * <p>
 * String.prototype에 노출되는 UTF-16 정합성 메서드를 검증한다.
 */
public class Es2026WellFormedStringTest {

    /**
     * Checks pairs, lone high and low surrogates, and the effect of adjacent invalid units.
     * <p>
     * 쌍, 짝 없는 상위·하위 서로게이트와 인접한 잘못된 코드 단위를 확인한다.
     */
    public void testPairedAndUnpairedCodeUnits() {
        String script = "const from=String.fromCharCode, good=from(0xD834,0xDD1E);"
                + "const broken=from(0xDC00,0xD834,0xDD1E,0xD800,65,0xDC00,0xD800,0xD800,0xDC00);"
                + "const repaired=broken.toWellFormed();"
                + "const units=Array.from({length:repaired.length},(_,i)=>repaired.charCodeAt(i).toString(16)).join();"
                + "[''.isWellFormed(),'text'.toWellFormed(),good.isWellFormed(),good.toWellFormed()===good,"
                + "from(0xD800).isWellFormed(),from(0xDC00).isWellFormed(),broken.isWellFormed(),"
                + "repaired.isWellFormed(),repaired.length===broken.length,units].join('|')";
        Check.equal("true|text|true|true|false|false|false|true|true|fffd,d834,dd1e,fffd,41,fffd,fffd,d800,dc00",
                evaluate(script), "unpaired UTF-16 units are replaced without affecting valid pairs");
    }

    /**
     * Both methods perform RequireObjectCoercible followed by one string conversion.
     * <p>
     * 두 메서드는 RequireObjectCoercible 뒤에 문자열 변환을 한 번 실행한다.
     */
    public void testReceiverCoercionAndPropertyAttributes() {
        String script = "function fail(f){try{f();return 'missing';}catch(e){return e.name||e;}}"
                + "let log=[];const p=String.prototype;const obj={ [Symbol.toPrimitive](h){log.push(h);return String.fromCharCode(0xD800);}};"
                + "const desc=Object.getOwnPropertyDescriptor(p,'toWellFormed');"
                + "[p.isWellFormed.call(obj),p.toWellFormed.call(obj).charCodeAt(0).toString(16),log.join(),"
                + "p.isWellFormed.call(42n),p.toWellFormed.call(new String('abc')),"
                + "fail(()=>p.toWellFormed.call(null)),fail(()=>p.isWellFormed.call(undefined)),"
                + "fail(()=>p.toWellFormed.call(Symbol())),fail(()=>p.isWellFormed.call({toString(){throw 'coercion';}})),"
                + "desc.value.name,desc.value.length,desc.writable,desc.enumerable,desc.configurable].join('|')";
        Check.equal("false|fffd|string,string|true|abc|TypeError|TypeError|TypeError|coercion|toWellFormed|0|true|false|true",
                evaluate(script), "prototype methods use generic ToString rather than the primitive-only String brand");
    }

    /**
     * Executes a script and converts its completion value to the expected text form.
     * <p>
     * 스크립트를 실행하고 완료 값을 기대하는 문자열 형태로 변환한다.
     *
     * @param script test source / 시험 소스
     * @return script result text / 스크립트 결과 문자열
     */
    private static String evaluate(String script) {
        return Ops.toString(JsRealm.create().evaluateScript(script, "well-formed-string.js"));
    }
}
