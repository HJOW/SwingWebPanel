package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Checks duplicate regular expression capture names and their observable results.
 * <p>
 * 정규식 중복 캡처 이름과 스크립트에서 관찰하는 결과를 검증한다.
 */
public class Es2026RegExpNamedGroupsTest {

    /**
     * Permits a shared name across alternatives and preserves indexed captures and ordered named properties.
     * <p>
     * 다른 대안에서 동일 이름을 허용하고 번호 캡처와 이름 프로퍼티 순서를 보존한다.
     */
    public void testDuplicateNamesAndIndices() {
        String script = "const re=/^(?:(?<x>a)(?<y>1)|(?<z>2)(?<x>b)|c)\\k<x>$/d;"
                + "const a=re.exec('a1a'), b=re.exec('2bb'), c=re.exec('c'), bare=/(a)/d.exec('a');"
                + "[a.groups.x,b.groups.x,a[1],b[4],a[4]===undefined,b[1]===undefined,"
                + "a.indices.groups.x===a.indices[1],b.indices.groups.x===b.indices[4],"
                + "b.indices.groups.x.join(),Object.keys(b.groups).join(),Object.keys(b.indices.groups).join(),"
                + "Object.getPrototypeOf(b.groups)===null,Object.getPrototypeOf(b.indices.groups)===null,"
                + "Object.hasOwn(c.groups,'x'), c.groups.x===undefined,c.indices.groups.x===undefined,"
                + "Object.hasOwn(bare,'groups'),Object.hasOwn(bare.indices,'groups')].join('|')";
        Check.equal("a|b|a|b|true|true|true|true|1,2|x,y,z|x,y,z|true|true|true|true|true|true|true", evaluate(script),
                "named properties retain first declaration order and use the participating capture");
    }

    /**
     * Checks rejection of same-name captures that can participate along the same alternative path.
     * <p>
     * 같은 대안 경로에 함께 참여할 수 있는 동일 이름 캡처를 거부한다.
     */
    public void testGroupPathEarlyErrors() {
        String script = "const conflict=['(?<x>a)(?<x>b)','(?<x>(?<x>a)|b)','(?<x>a)(?:b|(?<x>c))',"
                + "'(?:(?<x>a)|b)(?:c|(?<x>d))','(?=(?<x>a))(?<x>a)','(?<x>a){0}(?<x>b)',"
                + "'(?<other>a)\\\\k<x>'];"
                + "function invalid(work){try{work();return false;}catch(e){return e.name==='SyntaxError';}}"
                + "[conflict.every(p=>invalid(()=>new RegExp(p,'u'))),"
                + "invalid(()=>Function('return /(?<x>a)(?<x>b)/v')),"
                + "Function('return /(?<x>a)|(?<x>b)/v')().test('b'),"
                + "/^(?:(?<x>a)|(?:b|(?<x>c))|(?<x>d))$/.exec('d').groups.x,"
                + "/^(?:(?<\\u0078>a)|(?<x>b))\\k<\\u0078>$/u.test('bb')].join()";
        Check.equal("true,true,true,d,true", evaluate(script), "duplicate capture validation uses syntactic alternative paths");
    }

    /**
     * Checks that backtracking, repeated alternatives, assertions and replacement select the participating capture.
     * <p>
     * 역추적, 반복 대안, 단언 및 치환이 참여한 캡처를 선택하는지 확인한다.
     */
    public void testBacktrackingAndReplacement() {
        String script = "const r=/^(?:(?<x>a)|(?<x>b))+\\k<x>$/d, m=r.exec('abb');"
                + "const look=/^(?:(?<x>a)(?<=\\k<x>)|(?<x>b)(?<=\\k<x>))\\k<x>$/d.exec('bb');"
                + "const unmatched=/^(?:(?<x>a)|(?<x>b)|c)\\k<x>$/.exec('c');"
                + "const replacement=/((?<n>a)|(?<n>b))/g;"
                + "[m.groups.x,m[1]===undefined,m.indices.groups.x.join(),r.test('aba'),look.groups.x,"
                + "unmatched.groups.x===undefined,'aba'.replace(replacement,'<$<n>>'),"
                + "'aba'.replace(replacement,(m,a,b,c,off,input,groups)=>'['+groups.n+']')].join('|')";
        Check.equal("b|true|1,2|false|b|true|<a><b><a>|[a][b][a]", evaluate(script),
                "backreferences and replacements use the active named capture");
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
        return Ops.toString(JsRealm.create().evaluateScript(script, "regexp-named-groups.js"));
    }
}
