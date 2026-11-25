package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Checks modern regular expression modifiers and literal escaping.
 * <p>
 * 최신 정규식 수정자와 리터럴 이스케이프를 확인한다.
 */
public class Es2026RegExpModifiersAndEscapeTest {

    /**
     * Validates the early error rules for inline flags and their separation from the observable outer flags.
     * <p>
     * 인라인 플래그의 조기 오류와 외부 플래그 조회 동작을 검증한다.
     */
    public void testModifierSyntax() {
        String script = "function bad(work){try{work();return false;}catch(e){return e.name==='SyntaxError';}}"
                + "const patterns=['(?-:a)','(?ii:a)','(?-mm:a)','(?mi-sm:a)','(?i-i:a)','(?i--m:a)',"
                + "'(?i:a','(?g:a)','(?u:a)','(?v:a)','(?y:a)','(?d:a)','(? i:a)','(?i)a'];"
                + "const re=/(?smi-:^.)/gy;"
                + "[patterns.every(p=>bad(()=>new RegExp(p))),bad(()=>Function('return /(?i-i:a)/')), "
                + "re.flags,re.source,re.global,re.sticky,re.ignoreCase,re.multiline,re.dotAll,"
                + "/(?i-:a)/.test('A'),/(?:(?-i:a))/.test('A')].join('|')";
        Check.equal("true|true|gy|(?smi-:^.)|true|true|false|false|false|true|false", evaluate(script),
                "only disjoint inline i, m and s are allowed");
    }

    /**
     * Checks nested flag changes, line matching, dot matching, search optimization and repetition backtracking.
     * <p>
     * 중첩 플래그 변경, 줄 일치, 점 일치, 시작 위치 검색 및 반복 역추적을 확인한다.
     */
    public void testNestedScopeAndBacktracking() {
        String script = "const a=/(?i:a(?-i:b)c)d/;"
                + "const b=/(?mi-s:^ab$)/s;"
                + "const c=/^(?i:a+?)(?-i:X)$/i;"
                + "const d=/^(?s:.+)(?-s:.)$/s;"
                + "[a.test('AbCd'),a.test('ABCd'),a.test('AbCD'),b.test('x\\nAB\\nz'),"
                + "/(?m:^b$)/.exec('a\\nb\\nc').index,/(?m:^x)(?-m:$)/m.test('y\\nx\\nz'),"
                + "c.test('aAAX'),c.test('aAAx'),d.test('\\na'),d.test('a\\n'),"
                + "/^(?i:[a]+?)(?-i:b)$/.test('AAb'),/^(?i:a|ab)c$/.test('ABc'),"
                + "/(?i:(?<=a))b/.test('Ab'),/(?i:(?<=a))b/.test('AB')].join()";
        Check.equal("true,false,false,true,2,false,true,false,true,false,true,true,true,false", evaluate(script),
                "inline scopes are restored at the original instructions after backtracking");
    }

    /**
     * Checks case-sensitive and folded versions of backreferences, boundaries, and Unicode Sets.
     * <p>
     * 역참조, 단어 경계 및 Unicode Sets의 대소문자 구분·접기 동작을 확인한다.
     */
    public void testCaseScopedComparisons() {
        String script = "const kelvin=String.fromCodePoint(0x212A), r=/^(a)(?i:\\1)(?-i:\\1)$/i;"
                + "const named=/^(?:(?<x>a)|(?<x>b))(?i:\\k<x>)$/v;"
                + "[r.test('aAa'),r.test('aAA'),named.test('bB'),named.test('bA'),"
                + "/(?i:\\b\\u212A\\b)/u.test(kelvin),/(?i:\\b\\u212A\\b)/.test(kelvin),"
                + "/(?-i:\\b\\u212A\\b)/iu.test(kelvin),/(?i:[a&&A])/v.test('A'),"
                + "/(?-i:[a&&A])/iv.test('A'),/(?i:\\P{Lowercase_Letter})/v.test('A'),"
                + "/(?-i:\\P{Lowercase_Letter})/iv.test('A'),"
                + "/(?i:[\\q{ab}&&\\q{AB}])/v.test('Ab'),/(?i:[\\q{ab}&&\\q{AB}])x/v.test('AbX')].join()";
        Check.equal("true,false,true,false,true,false,false,true,false,false,true,true,false", evaluate(script),
                "comparison mode is taken from the local pattern scope");
    }

    /**
     * Evaluates a test in its own realm.
     * <p>
     * 독립 realm에서 시험 코드를 실행한다.
     *
     * @param script source text / 스크립트 원문
     * @return completion as text / 문자열 완료 값
     */
    private static String evaluate(String script) {
        return Ops.toString(JsRealm.create().evaluateScript(script, "regexp-modifiers.js"));
    }
}
