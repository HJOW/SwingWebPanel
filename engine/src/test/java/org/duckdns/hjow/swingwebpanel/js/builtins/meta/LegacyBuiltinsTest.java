package org.duckdns.hjow.swingwebpanel.js.builtins.meta;

import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsNumber;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the global functions <code>escape</code> and <code>unescape</code> (ECMA-262 6th edition Annex B.2.1): the set of characters
 * that stay unescaped, the <code>%XX</code> and <code>%uXXXX</code> forms, surrogate pairs, malformed escapes, argument conversion and
 * the attributes of the global properties.
 * <p>
 * 전역 함수 <code>escape</code>, <code>unescape</code> 시험(ECMA-262 6판 부속서 B.2.1): 그대로 두는 문자 집합, <code>%XX</code> 와
 * <code>%uXXXX</code> 형식, 서로게이트 쌍, 잘못된 이스케이프, 인수 변환, 전역 프로퍼티의 속성.
 */
public class LegacyBuiltinsTest {

    /**
     * The environment of the running case.
     * <p>
     * 실행 중인 사례의 환경.
     */
    private MetaTestSupport.Env e;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public LegacyBuiltinsTest() {
    }

    /**
     * Creates a fresh realm for each case.
     * <p>
     * 사례마다 새 realm 을 만든다.
     */
    public void setUp() {
        e = new MetaTestSupport.Env();
    }

    /**
     * Calls a global function with one argument and returns its string result.
     * <p>
     * 전역 함수를 인수 하나로 호출하고 문자열 결과를 돌려준다.
     *
     * @param name the function name / 함수 이름
     * @param arg  the argument / 인수
     * @return the result text / 결과 문자열
     */
    private String run(String name, JsValue arg) {
        return ((JsString) org.duckdns.hjow.swingwebpanel.js.Ops.call(e.global(name), Js.UNDEFINED, MetaTestSupport.args(arg))).getValue();
    }

    /**
     * B.2.1: names, lengths and attributes of the two functions.
     * <p>
     * B.2.1: 두 함수의 이름, length, 속성.
     */
    public void testShape() {
        String[] names = { "escape", "unescape" };
        // 두 함수 모두 length 1, writable/configurable, 열거 불가, 생성자 아님이다.
        for (int i = 0; i < names.length; i++) {
            JsObject f = (JsObject) e.global(names[i]);
            Check.equal(names[i], f.getString("name"), "name");
            Check.equal(1.0, ((JsNumber) f.get("length")).getValue(), names[i] + ".length");
            Check.isFalse(f.isConstructor(), names[i] + " is not a constructor");
            PropertyDescriptor d = e.realm.getGlobalObject().getOwnProperty(names[i]);
            Check.isTrue(d.isWritableTrue() && d.isConfigurableTrue() && !d.isEnumerableTrue(), names[i] + " global attributes");
        }
    }

    /**
     * B.2.1.1 escape: letters, digits and <code>@*_+-./</code> stay; other Latin-1 characters become %XX; others %uXXXX; every UTF-16
     * code unit is escaped separately (a surrogate pair gives two %u escapes).
     * <p>
     * B.2.1.1 escape: 영문자, 숫자, <code>@*_+-./</code> 는 그대로이고 그 밖의 Latin-1 문자는 %XX, 나머지는 %uXXXX 가 된다. 모든 UTF-16 코드
     * 단위를 따로 이스케이프한다(서로게이트 쌍은 %u 두 개).
     */
    public void testEscape() {
        Check.equal("abcXYZ019@*_+-./", run("escape", MetaTestSupport.s("abcXYZ019@*_+-./")), "characters that stay");
        Check.equal("%20%21%22%23%24%25%26%27%28%29%2C%3A%3B%3C%3D%3E%3F%5B%5C%5D%5E%60%7B%7C%7D%7E", run("escape", MetaTestSupport.s(" !\"#$%&'(),:;<=>?[\\]^`{|}~")), "ASCII punctuation");
        Check.equal("%E9%FF%00%0A", run("escape", MetaTestSupport.s("éÿ" + (char) 0 + "\n")), "Latin-1 and controls use %XX with upper-case hex");
        Check.equal("%u0100%uD55C%uFFFF", run("escape", MetaTestSupport.s("Ā한" + (char) 0xFFFF)), "code units above 255 use %uXXXX");
        Check.equal("%uD83D%uDE00", run("escape", MetaTestSupport.s("😀")), "a surrogate pair is two escapes");
        Check.equal("%uD83D", run("escape", MetaTestSupport.s("\ud83d")), "a lone surrogate");
        Check.equal("", run("escape", MetaTestSupport.s("")), "empty string");
    }

    /**
     * B.2.1.2 unescape: %XX and %uXXXX are decoded (either hex case); malformed sequences stay as they are.
     * <p>
     * B.2.1.2 unescape: %XX 와 %uXXXX 를 해독하며(16진수 대소문자 모두) 잘못된 시퀀스는 그대로 둔다.
     */
    public void testUnescape() {
        Check.equal("éÿ", run("unescape", MetaTestSupport.s("%E9%ff")), "%XX in both cases");
        Check.equal("한한", run("unescape", MetaTestSupport.s("%uD55C%ud55c")), "%uXXXX in both cases");
        Check.equal("😀", run("unescape", MetaTestSupport.s("%uD83D%uDE00")), "surrogates are joined by the string itself");
        Check.equal("%", run("unescape", MetaTestSupport.s("%")), "lone percent");
        Check.equal("%z", run("unescape", MetaTestSupport.s("%z")), "percent and a non-hex character");
        Check.equal("%1", run("unescape", MetaTestSupport.s("%1")), "percent and one digit");
        Check.equal("%1z", run("unescape", MetaTestSupport.s("%1z")), "percent and a digit and a non-hex character");
        Check.equal("%u12", run("unescape", MetaTestSupport.s("%u12")), "incomplete %u escape");
        Check.equal("%u12G4", run("unescape", MetaTestSupport.s("%u12G4")), "%u with a non-hex digit");
        Check.equal("a b", run("unescape", MetaTestSupport.s("a%20b")), "space");
        Check.equal("100%", run("unescape", MetaTestSupport.s("100%")), "trailing percent");
        Check.equal("A", run("unescape", MetaTestSupport.s("%u0041")), "%u form of an ASCII letter");
        Check.equal("%A", run("unescape", MetaTestSupport.s("%%41")), "a percent before a valid escape: the first percent is kept");
    }

    /**
     * Arguments are converted to strings and escape/unescape are inverse functions of each other.
     * <p>
     * 인수는 문자열로 바뀌며 escape 와 unescape 는 서로의 역함수이다.
     */
    public void testConversionAndRoundTrip() {
        Check.equal("123", run("escape", MetaTestSupport.n(123)), "number argument");
        Check.equal("true", run("escape", Js.TRUE), "boolean argument");
        Check.equal("undefined", run("escape", Js.UNDEFINED), "undefined argument");
        Check.equal("null", run("unescape", Js.NULL), "null argument");
        String sample = "Hello, 한글 world 😀 100% [ok] éñ\n\t~";
        Check.equal(sample, run("unescape", MetaTestSupport.s(run("escape", MetaTestSupport.s(sample)))), "round trip");
        Check.equal("undefined", ((JsString) org.duckdns.hjow.swingwebpanel.js.Ops.call(e.global("escape"), Js.UNDEFINED, Js.NO_ARGS)).getValue(), "no argument means undefined");
    }
}
