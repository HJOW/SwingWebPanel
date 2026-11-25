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
     * Checks nested ranges, intersection, subtraction, and empty sets over complete code points.
     * <p>
     * 전체 코드 포인트로 중첩 범위, 교집합, 차집합, 빈 집합을 검사한다.
     */
    public void testCodePointSetExpressions() {
        String script = "const letters=/[[a-z]--[aeiou]]/gv, wide=/[[\uD801\uDC00-\uD801\uDC27]&&\\p{Letter}]/gv;"
                + "const edge=String.fromCodePoint(0x10400), past=String.fromCodePoint(0x10428);"
                + "[ 'abcefg'.match(letters).join(''),"
                + "'ABCaz2'.match(/[\\p{ASCII}&&\\p{Letter}&&[^Bz]]/gv).join(''),"
                + "'abcdef'.match(/[[a-f]--[b-d]--f]/gv).join(''),"
                + "'abcXYZ5'.match(/[[\\p{Letter}--[a-z]]0-9]/gv).join(''),"
                + "(past+edge+past).match(wide)[0]===edge, /[]/v.test('x'),/[^]/v.test(edge),"
                + "/[[a-z]&&[]]/v.test('a'), /[[a-z]--[^]]/v.test('a'), /[[a-z]--[]]/v.test('a') ].join('|')";
        Check.equal("bcfg|ACa|ae|XYZ5|true|false|true|false|false|true", evaluate(script),
                "Unicode set union, intersection, subtraction, and complete code points");
    }

    /**
     * Checks which characters are reserved in v classes and rejects mixed operators and invalid ranges.
     * <p>
     * v 클래스의 예약 문자를 확인하고 혼합 연산자와 잘못된 범위를 거부한다.
     */
    public void testUnicodeSetsPunctuationAndEarlyErrors() {
        String script = "const bad=['[()]','[{}]','[a|b]','[a/b]','[-a]','[a-]','[a--]','[a&&]','[a&&&b]',"
                + "'[a&&b--c]','[ab&&c]','[a&&bc]','[a-z&&b]','[a--b-c]',"
                + "'[a-\\\\d]','[\\\\d-a]','[z-a]','[a\\\\01]','[a\\\\a]','[[a]','[a!!]',"
                + "'['+String.fromCharCode(96,96)+']','[^^^]','[==]','[..]'];"
                + "function invalid(p){try{new RegExp(p,'v');return false;}catch(e){return e.name==='SyntaxError';}}"
                + "const punctuation=/[\\(\\)\\{\\}\\/\\|\\-\\!\\&\\=\\~]/gv;"
                + "[bad.every(invalid), '(){}/|-!&=~'.match(punctuation).join(''),"
                + "/[&=.!~]/v.test('&'), /[\\b\\0]/v.test(String.fromCharCode(0)),"
                + "/[^]/v.test(String.fromCharCode(0xD800)), /[/]/u.test('/'),"
                + "(function(){try{Function('return /[a&&]/v');return false;}catch(e){return e.name==='SyntaxError';}})()].join('|')";
        Check.equal("true|(){}/|-!&=~|true|true|true|true|true", evaluate(script), "Unicode Sets class grammar");
    }

    /**
     * Checks the order of Unicode case folding and complement and the treatment of word classes.
     * <p>
     * 유니코드 대소문자 접기와 여집합의 순서 및 단어 클래스를 검사한다.
     */
    public void testFoldBeforeUnicodeSetOperation() {
        String script = "const kelvin=String.fromCodePoint(0x212A), capital=String.fromCodePoint(0x10400),"
                + " small=String.fromCodePoint(0x10428);"
                + "[/\\P{Lowercase_Letter}/iu.test('A'),/\\P{Lowercase_Letter}/iv.test('A'),"
                + "/\\P{Uppercase_Letter}/iv.test('a'),/\\P{gc=Uppercase_Letter}/iv.test('a'),"
                + "/\\P{Lowercase_Letter}/iv.test('@'),/[\\p{Ll}&&\\p{Lu}]/iv.test('a'),"
                + "/[a&&A]/iv.test('A'),/[a&&A]/v.test('A'),/[[A-Z]--[kK]]/iv.test(kelvin),"
                + "/[[A-Z]&&[kK]]/iv.test(kelvin),/[^[K]]/iv.test(kelvin), /[^\\p{Lu}]/iv.test('a'),"
                + "/[\\w&&[^a-z0-9_]]/iv.test(kelvin), /\\W/iv.test(kelvin), /\\W/iu.test(kelvin),"
                + "new RegExp('['+capital+'&&'+small+']','iv').test(capital)].join()";
        Check.equal("true,false,false,false,true,true,true,false,false,true,false,false,false,false,false,true",
                evaluate(script), "folding for ranges, properties, complements, word escapes, and supplementary code points");
    }

    /**
     * Checks that longer alternatives come first and shorter and empty ones remain available for backtracking.
     * <p>
     * 긴 대안을 먼저 선택하고 역추적 시 짧은 대안과 빈 대안도 시도하는지 확인한다.
     */
    public void testStringClassMatchOrder() {
        String script = "const longer=/[\\q{a|ab|abc}]/dv.exec('abc');"
                + "const short=/^([\\q{a|ab|abc}])bc$/v.exec('abc');"
                + "const empty=/^([\\q{|ab|a}])ab$/v.exec('ab');"
                + "const behind=/(?<=([\\q{x|ax}]))b/v.exec('axb');"
                + "const escaping=/[\\q{a\\|b|a\\}b|a\\[b}]/gv;"
                + "[longer[0],longer.indices[0].join(),short[1],empty[1]==='',behind[1],"
                + "'a|b a}b a[b'.match(escaping).join(),/^[\\q{}]$/v.test('')].join('!')";
        Check.equal("abc!0,3!a!true!ax!a|b,a}b,a[b!true", evaluate(script),
                "longest string match, backtracking, escapes, and reversed execution");
    }

    /**
     * Checks string and character membership in the same set and precise treatment of surrogate code points.
     * <p>
     * 같은 집합의 문자열·문자 원소와 서로게이트 코드 포인트의 정확한 처리를 확인한다.
     */
    public void testStringClassSetOperations() {
        String script = "const both=/[[\\q{ab|a|abc}]&&[\\q{AB|a|xx}]]/giv;"
                + "const difference=/[[\\q{ab|a|abc}]--[\\q{AB|a}]]/giv;"
                + "const c=String.fromCodePoint(0x10400);"
                + "['A Ab aBC'.match(both).join(), 'Ab aBC'.match(difference).join(),"
                + "/[\\q{a}&&a]/v.test('a'),/[[a-z]--\\q{a|ba}]/v.test('a'),"
                + "/[^\\q{A|b}]/iv.test('a'),/[^\\q{A|b}]/iv.test('c'),"
                + "/[^\\q{aa|a}&&[a]]/iv.test('A'),/[^\\q{aa|a}&&[a]]/iv.test('c'),"
                + "/^[\\q{\\uD801\\uDC00}]$/v.test(c),/^[\\q{\\u{D801}\\u{DC00}}]$/v.test(c),"
                + "/[\\q{\\u{10400}}&&\\u{10428}]/iv.test(c)].join('!')";
        Check.equal("A,Ab,aB!aBC!true!false!false!true!false!true!true!false!true", evaluate(script),
                "set operations compare character and string elements with Unicode folding");
    }

    /**
     * Checks early errors from q grammar, static complement restrictions, and excluded emoji properties.
     * <p>
     * q 문법, 여집합의 정적 제한과 제외한 이모지 속성의 조기 오류를 확인한다.
     */
    public void testStringClassEarlyErrorsAndEmojiPolicy() {
        String script = "function invalid(p,flags){try{new RegExp(p,flags);return false;}catch(e){return e.name==='SyntaxError';}}"
                + "const bad=['\\\\q{ab}','[\\\\q{ab]','[\\\\q{a-b}]','[\\\\q{\\\\w}]','[\\\\q{a!!}]',"
                + "'[\\\\q{a}-b]','[a-\\\\q{b}]','[^\\\\q{ab}]','[^\\\\q{}]',"
                + "'[^\\\\q{ab}--\\\\q{ab}]','[^\\\\q{ab}&&\\\\q{cd}]'];"
                + "const emoji=['Basic_Emoji','Emoji_Keycap_Sequence','RGI_Emoji_Modifier_Sequence',"
                + "'RGI_Emoji_Flag_Sequence','RGI_Emoji_Tag_Sequence','RGI_Emoji_ZWJ_Sequence','RGI_Emoji',"
                + "'Emoji','Emoji_Presentation','Emoji_Modifier','Emoji_Modifier_Base','Emoji_Component','Extended_Pictographic'];"
                + "[bad.every(p=>invalid(p,'v')),invalid('[\\\\q{a}]','u'),"
                + "emoji.every(n=>invalid('\\\\p{'+n+'}','v')&&invalid('[\\\\p{'+n+'}]','v')&&invalid('\\\\P{'+n+'}','v')),"
                + "(function(){try{Function('return /\\\\p{Basic_Emoji}/v');return false;}"
                + "catch(e){return e.name==='SyntaxError';}})()].join('!')";
        Check.equal("true!true!true!true", evaluate(script), "q syntax and EXC-15 are enforced before matching");
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
