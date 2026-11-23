package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The DOM of the old link and metadata attributes (plan item P07-07-c, LEG-15; HTML5 REC 2014-10-28 sections 4.2.4, 4.2.5, 4.5.1 and 4.11.1
 * and the list of obsolete features): <code>rev</code>, <code>charset</code> and <code>target</code> of <code>link</code>, the old
 * attributes of <code>a</code> (<code>charset</code>, <code>coords</code>, <code>name</code>, <code>rev</code>, <code>shape</code>), the
 * <code>scheme</code> of <code>meta</code>, <code>event</code> and <code>htmlFor</code> of <code>script</code>, the link types of
 * <code>rel</code> and the <code>text</code> of an anchor. They reflect the content attributes; they have no other effect, as the
 * specification says.
 * <p>
 * 옛 링크와 메타데이터 속성의 DOM(계획 P07-07-c, LEG-15, HTML5 REC 2014-10-28 §4.2.4, §4.2.5, §4.5.1, §4.11.1, 폐기된 기능 목록): <code>link</code> 의 <code>rev</code>,
 * <code>charset</code>, <code>target</code>, <code>a</code> 의 옛 속성(<code>charset</code>, <code>coords</code>, <code>name</code>, <code>rev</code>,
 * <code>shape</code>), <code>meta</code> 의 <code>scheme</code>, <code>script</code> 의 <code>event</code> 와 <code>htmlFor</code>, <code>rel</code> 의 링크 타입, 앵커의
 * <code>text</code>. 콘텐츠 속성을 반영하며 규격대로 다른 효과는 없다.
 */
public class LegacyLinkMetaTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public LegacyLinkMetaTest() {
    }

    /**
     * The old attributes reflect the content attributes in both directions and are empty strings by default.
     * <p>
     * 옛 속성은 콘텐츠 속성을 양방향으로 반영하며 기본값은 빈 문자열이다.
     */
    public void testObsoleteAttributesReflect() {
        PageFixture f = new PageFixture().load("<!doctype html><head><link id=\"l\" rel=\"stylesheet\" rev=\"made\" charset=\"iso-8859-1\" target=\"_blank\" href=\"x.css\">"
                + "<meta id=\"m\" name=\"author\" content=\"c\" scheme=\"ISBN\"><script id=\"s\" for=\"window\" event=\"onload\"></script></head>"
                + "<body><a id=\"a\" name=\"top\" rev=\"toc\" charset=\"utf-8\" coords=\"1,2,3\" shape=\"rect\" href=\"p.html\">anchor <b>text</b></a><a id=\"plain\"></a>");
        Check.equal("made|iso-8859-1|_blank", f.scripts.evaluate("var l = document.getElementById('l'); [l.rev, l.charset, l.target].join('|')", "t"), "link");
        Check.equal("ISBN", f.scripts.evaluate("document.getElementById('m').scheme", "t"), "meta scheme");
        Check.equal("onload|window", f.scripts.evaluate("var s = document.getElementById('s'); [s.event, s.htmlFor].join('|')", "t"), "script event and htmlFor");
        Check.equal("top|toc|utf-8|1,2,3|rect", f.scripts.evaluate("var a = document.getElementById('a'); [a.name, a.rev, a.charset, a.coords, a.shape].join('|')", "t"), "a");
        Check.equal("||||", f.scripts.evaluate("var p = document.getElementById('plain'); [p.name, p.rev, p.charset, p.coords, p.shape].join('|')", "t"), "defaults are empty strings");
        Check.equal("a|ISO|c|d|e|f", f.scripts.evaluate("a.name = 'a'; a.rev = 'ISO'; l.charset = 'c'; l.target = 'd'; document.getElementById('m').scheme = 'e'; s.htmlFor = 'f';"
                + " [a.getAttribute('name'), a.getAttribute('rev'), l.getAttribute('charset'), l.getAttribute('target'), document.getElementById('m').getAttribute('scheme'),"
                + " s.getAttribute('for')].join('|')", "t"), "setting reflects");
        Check.equal("anchor text", f.scripts.evaluate("a.text", "t"), "the text of an anchor is its text content");
    }

    /**
     * The link types of <code>rel</code>: the tokens are compared without regard to case and a token list reflects the attribute; a
     * <code>link</code> with <code>rev</code> only is not a style sheet.
     * <p>
     * <code>rel</code> 의 링크 타입: 토큰은 대소문자를 구분하지 않고 비교하며 토큰 목록이 속성을 반영한다. <code>rev</code> 만 있는 <code>link</code> 는 스타일 시트가 아니다.
     */
    public void testLinkTypes() {
        java.util.Map<String, String> files = new java.util.HashMap<String, String>();
        files.put("http://example.test/dir/a.css", "p { color: red }");
        files.put("http://example.test/dir/b.css", "p { color: green }");
        files.put("http://example.test/dir/c.css", "p { color: blue }");
        PageFixture f = CssomSheetsTest.page("<!doctype html><link id=\"l\" rel=\"Alternate  STYLESHEET\" href=\"a.css\" title=\"x\"><link id=\"r\" rev=\"stylesheet\" href=\"b.css\">"
                + "<link rel=\"STYLESHEET\" href=\"c.css\"><a id=\"a\" href=\"x\" rel=\"nofollow Noopener\">a</a><div id=\"d\">x</div>", files);
        Check.equal("true|true|false|2", f.scripts.evaluate("var l = document.getElementById('l'); [l.relList.contains('Alternate'), l.relList.contains('STYLESHEET'), l.relList.contains('alternate'), l.relList.length].join('|')", "t"),
                "the tokens of a token list are case-sensitive and repeated spaces do not make tokens (the link types are compared without regard to case when the page is processed)");
        Check.equal("true|true|nofollow Noopener", f.scripts.evaluate("var a = document.getElementById('a'); [a.relList.contains('nofollow'), a.relList.contains('Noopener'), a.rel].join('|')", "t"),
                "the relList of an anchor");
        Check.equal("true|false", f.scripts.evaluate("a.relList.add('external'); [a.getAttribute('rel').indexOf('external') > 0, a.relList.contains('missing')].join('|')", "t"), "the list reflects the attribute");
        Check.equal("1|http://example.test/dir/c.css", f.scripts.evaluate("[document.styleSheets.length, document.styleSheets[0].href].join('|')", "t"),
                "only the link with rel=stylesheet makes a sheet: not the alternate one, and not rev=stylesheet (the old reverse link type)");
    }
}
