package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests the Unicode Sets regular expression mode and its string protocols.
 * <p>
 * Unicode Sets 정규식 모드와 문자열 프로토콜을 검증한다.
 */
public class Es2026RegExpUnicodeSetsTest {

    /**
     * Checks v in constructors and literals, including both Unicode modes' mutual exclusion.
     * <p>
     * 생성자와 리터럴의 v 및 두 Unicode 모드의 동시 지정 거부를 확인한다.
     */
    public void testUnicodeSetsFlagAndWholeCodePoints() {
        String script = "function fail(f){try{f();return 'missing';}catch(e){return e.name;}}"
                + "const s=String.fromCodePoint(0x10400), r=/^.$/v, sticky=new RegExp('.','yvgd');"
                + "const m=sticky.exec(s); const letter=new RegExp('\\\\p{Letter}','v');"
                + "[r.unicodeSets,r.unicode,/a/u.unicodeSets,r.flags,sticky.flags,sticky.lastIndex,m.indices[0].join(),"
                + "r.test(s),letter.test(s),/^.$/.test(s),fail(()=>new RegExp('a','uv')),"
                + "fail(()=>new RegExp('a','vu')),fail(()=>new RegExp('a','vv')),fail(()=>Function('return /a/uv')), "
                + "fail(()=>Object.getOwnPropertyDescriptor(RegExp.prototype,'unicodeSets').get.call({}))].join('|')";
        Check.equal("true|false|false|v|dgvy|2|0,2|true|true|false|SyntaxError|SyntaxError|SyntaxError|SyntaxError|TypeError",
                evaluate(script), "Unicode mode flag values and code-point matching");
    }

    /**
     * Checks the generic flags getter's property access order.
     * <p>
     * 일반 flags getter의 프로퍼티 접근 순서를 확인한다.
     */
    public void testFlagsAccessorOrder() {
        String script = "let seen='';const fake={get hasIndices(){seen+='d';return true;},get global(){seen+='g';return true;},"
                + "get ignoreCase(){seen+='i';return false;},get multiline(){seen+='m';return true;},"
                + "get dotAll(){seen+='s';return false;},get unicode(){seen+='u';return true;},"
                + "get unicodeSets(){seen+='v';return true;},get sticky(){seen+='y';return true;}};"
                + "const p=RegExp.prototype;const d=Object.getOwnPropertyDescriptor(p,'unicodeSets');"
                + "[Object.getOwnPropertyDescriptor(p,'flags').get.call(fake),seen,d.get.name,d.get.length,"
                + "d.enumerable,d.configurable,d.get.call(p)===undefined].join('|')";
        Check.equal("dgmuvy|dgimsuvy|get unicodeSets|0|false|true|true", evaluate(script), "computed flags follow the specified order");
    }

    /**
     * Global string operations advance empty matches over complete code points in v mode.
     * <p>
     * 전역 문자열 연산은 v 모드에서 빈 일치 다음으로 갈 때 전체 코드 포인트를 넘는다.
     */
    public void testUnicodeSetsAdvanceForStringProtocols() {
        String script = "const c=String.fromCodePoint(0x1D11E), s=c+'x';"
                + "const it=s.matchAll(/(?:)/gv), indices=it.map(m=>m.index).toArray().join();"
                + "const split=s.split(/(?:)/v), replaced=s.replace(/(?:)/gv,'|');"
                + "[it instanceof Iterator,Iterator.from(it)===it,it.next().done,indices,s.match(/(?:)/gv).length,"
                + "split.length,split[0]===c,split[1],replaced===('|'+c+'|x|')].join('!')";
        Check.equal("true!true!true!0,2,3!3!2!true!x!true", evaluate(script), "match, matchAll, split and replace advance consistently");
    }

    /**
     * matchAll reads the original flags and returns a branded iterator that stops calling exec after completion.
     * <p>
     * matchAll은 원본 플래그를 읽고 완료 뒤 exec 호출을 멈추는 전용 이터레이터를 반환한다.
     */
    public void testMatchAllUsesSpeciesAndStopsAfterCompletion() {
        String script = "let seen='', calls=0;function fail(f){try{f();return 'missing';}catch(e){return e.name;}}"
                + "const fake={flags:'v',lastIndex:2,constructor:{[Symbol.species]:function(rx,flags){"
                + "seen=flags+String(rx===fake);"
                + "return {get unicode(){throw 'do not read';},get unicodeSets(){throw 'do not read';},"
                + "exec(s){calls++;return {0:s[this.lastIndex]};}};}}};"
                + "const it=RegExp.prototype[Symbol.matchAll].call(fake,'abc');"
                + "const match=it.next(), last=it.next(), again=it.next();"
                + "[seen,calls,match.value[0],match.done,last.done,again.done,it[Symbol.iterator]()===it,"
                + "Object.prototype.toString.call(it),fail(()=>it.next.call({})),"
                + "fail(()=>'a'.matchAll(/a/v))].join('|')";
        Check.equal("vtrue|1|c|false|true|true|true|[object RegExp String Iterator]|TypeError|TypeError",
                evaluate(script), "direct Symbol.matchAll can iterate a non-global regexp once");
    }

    /**
     * Evaluates a regular expression test script in an independent realm.
     * <p>
     * 독립 realm에서 정규식 시험 스크립트를 실행한다.
     *
     * @param script the source / 스크립트 소스
     * @return the completion value as text / 문자열 완료 값
     */
    private static String evaluate(String script) {
        return Ops.toString(JsRealm.create().evaluateScript(script, "regexp-unicode-sets.js"));
    }
}
