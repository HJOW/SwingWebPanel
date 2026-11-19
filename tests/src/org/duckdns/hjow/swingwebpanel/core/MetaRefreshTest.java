package org.duckdns.hjow.swingwebpanel.core;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the parsing of the <code>content</code> of <code>meta http-equiv=refresh</code> (plan item P07-07-c, LEG-15; HTML5 REC
 * 2014-10-28 section 4.2.5.3): the examples of the specification, separators, the optional <code>url=</code>, quotes, white space,
 * a fraction, and values that have no number.
 * <p>
 * <code>meta http-equiv=refresh</code> 의 <code>content</code> 해석 시험(계획 P07-07-c, LEG-15, HTML5 REC 2014-10-28 §4.2.5.3): 규격의 예, 구분자, 선택적
 * <code>url=</code>, 따옴표, 공백, 소수, 숫자가 없는 값.
 */
public class MetaRefreshTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public MetaRefreshTest() {
    }

    /**
     * Checks one value: the seconds and the address (null for the page itself).
     * <p>
     * 값 하나를 확인한다: 초와 주소(페이지 자신이면 null).
     *
     * @param content the value of <code>content</code> / <code>content</code> 값
     * @param seconds the expected seconds / 기대하는 초
     * @param url     the expected address or null / 기대하는 주소 또는 null
     */
    private static void check(String content, int seconds, String url) {
        MetaRefresh r = MetaRefresh.parse(content);
        Check.notNull(r, "parsed: '" + content + "'");
        Check.equal(seconds, r.getSeconds(), "seconds of '" + content + "'");
        Check.equal(url, r.getUrl(), "address of '" + content + "'");
    }

    /**
     * The values of the specification and the common old forms.
     * <p>
     * 규격의 값과 흔한 옛 형태.
     */
    public void testValuesThatRefresh() {
        check("300", 300, null);
        check("20; URL=page4.html", 20, "page4.html");
        check("0;url=/b.html", 0, "/b.html");
        check("5,url=x.html", 5, "x.html");
        check("  3  ;  URL = \"a b.html\" ", 3, "a b.html");
        check("1.5;url=y", 1, "y");
        check("2.;url=y", 2, "y");
        check("4; Url='quoted.html' trailing junk", 4, "quoted.html");
        check("4; url=a.html   ", 4, "a.html");
        check("4; url=\ta\n.\rhtml\t", 4, "a.html");
        check("5; url='http://x.test/p?a=1&b=2#f'", 5, "http://x.test/p?a=1&b=2#f");
        check("7;", 7, null);
        check("7 ", 7, null);
        check("7 abc", 7, null);
        check("3;x.html", 3, null);
        check("3; urx=y", 3, null);
        check("3; url", 3, null);
        check("3; url y", 3, null);
        check("3; url=", 3, "");
        check("3; url=''", 3, "");
        check("3; url='unterminated", 3, "unterminated");
        check("3 url=x", 3, null);
        check("0", 0, null);
        check("007", 7, null);
        check("99999999999999999999", Integer.MAX_VALUE, null);
        check("3; url = \"a\" ; url=b", 3, "a");
    }

    /**
     * Values without a number are not a refresh.
     * <p>
     * 숫자가 없는 값은 새로고침이 아니다.
     */
    public void testValuesThatDoNotRefresh() {
        Check.isNull(MetaRefresh.parse(null), "null");
        Check.isNull(MetaRefresh.parse(""), "empty");
        Check.isNull(MetaRefresh.parse("   "), "white space");
        Check.isNull(MetaRefresh.parse("abc"), "text");
        Check.isNull(MetaRefresh.parse("-1"), "a sign");
        Check.isNull(MetaRefresh.parse("+5"), "a plus sign");
        Check.isNull(MetaRefresh.parse(";url=x"), "no number before the separator");
        Check.isNull(MetaRefresh.parse(".5"), "a fraction without an integer part");
        Check.isNull(MetaRefresh.parse("url=x"), "no number");
    }
}
