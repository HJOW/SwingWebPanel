package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.net.SchemePolicy;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the script and style compatibility of old documents (plan item P07-07-c, LEG-13): the script that is wrapped in an HTML
 * comment, the <code>type</code> and the <code>language</code> attributes of a <code>script</code> element with the MIME types that
 * HTML5 REC 2014-10-28 section 4.11.1.1 lists, the <code>for</code> and <code>event</code> attributes (only <code>window</code> and
 * <code>onload</code> run the script), CDATA markers in comments, the style that is wrapped in an HTML comment, the style types, and the
 * <code>javascript:</code> address that is never followed.
 * <p>
 * 오래된 문서의 스크립트와 스타일 호환 시험(계획 P07-07-c, LEG-13): HTML 주석으로 감싼 스크립트, <code>script</code> 요소의 <code>type</code> 과 <code>language</code> 속성과
 * HTML5 REC 2014-10-28 §4.11.1.1 이 나열하는 MIME 타입, <code>for</code> 와 <code>event</code> 속성(<code>window</code> 와 <code>onload</code> 일 때만 실행),
 * 주석 안의 CDATA 표시, HTML 주석으로 감싼 스타일, 스타일 타입, 절대 따라가지 않는 <code>javascript:</code> 주소.
 */
public class LegacyScriptCompatTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public LegacyScriptCompatTest() {
    }

    /**
     * Loads a page with one script element that counts its runs and returns the number of runs.
     * <p>
     * 실행 횟수를 세는 script 요소 하나가 있는 페이지를 로드하고 실행 횟수를 반환한다.
     *
     * @param attributes the attributes of the script element / script 요소의 속성
     * @return how many times the script ran / 스크립트가 실행된 횟수
     */
    private static String runs(String attributes) {
        PageFixture f = new PageFixture().load("<!doctype html><script " + attributes + ">window.runs = (window.runs || 0) + 1;</script>");
        return f.scripts.evaluate("String(window.runs || 0)", "t");
    }

    /**
     * A script that hides itself in an HTML comment runs: the opening <code>&lt;!--</code> and a closing <code>--&gt;</code> at the start
     * of a line are comments of the script language (ECMA-262 Annex B), and a CDATA marker inside a comment is harmless.
     * <p>
     * HTML 주석으로 자신을 숨긴 스크립트는 실행된다: 여는 <code>&lt;!--</code> 와 줄 시작의 닫는 <code>--&gt;</code> 는 스크립트 언어의 주석이고(ECMA-262 부록 B) 주석 안의 CDATA
     * 표시는 해롭지 않다.
     */
    public void testScriptWrappedInHtmlComment() {
        PageFixture a = new PageFixture().load("<!doctype html><script><!--\nwindow.out = 'a';\n//--></script>");
        Check.equal("a", a.scripts.evaluate("window.out", "t"), "<!-- and //-->");
        PageFixture b = new PageFixture().load("<!doctype html><script type='text/javascript'>\n<!--\nwindow.out = 'b';\n// -->\n</script>");
        Check.equal("b", b.scripts.evaluate("window.out", "t"), "with a blank line and a space before the closing marker");
        PageFixture c = new PageFixture().load("<!doctype html><script> <!-- hidden from old browsers\nwindow.out = 'c';\n  --> </script>");
        Check.equal("c", c.scripts.evaluate("window.out", "t"), "text after the opening marker, and a closing marker at the start of a line");
        PageFixture d = new PageFixture().load("<!doctype html><script>//<![CDATA[\nwindow.out = 'd';\n//]]></script>");
        Check.equal("d", d.scripts.evaluate("window.out", "t"), "CDATA markers in comments");
        PageFixture e = new PageFixture().load("<!doctype html><script><!--\nvar s = '<\\/script>'; window.out = s;\n//--></script>");
        Check.equal("</script>", e.scripts.evaluate("window.out", "t"), "an escaped end tag inside a string");
        PageFixture g = new PageFixture().load("<!doctype html><script><!--\nwindow.out = 'x';\n--></script><script>window.out2 = window.out + 'y';</script>");
        Check.equal("xy", g.scripts.evaluate("window.out2", "t"), "the next script runs after one that ends with --> and the comment has no effect on it");
        PageFixture h = new PageFixture().load("<!doctype html><script>\nvar n = 5;\nvar r = n --> 3 ? 'big' : 'small'; window.out = r;</script>");
        Check.equal("big", h.scripts.evaluate("window.out", "t"), "--> inside a line is the operators -- and >");
    }

    /**
     * The type of the script block: the type attribute, else the language attribute with "text/", else JavaScript; only the MIME types
     * that HTML5 2014 lists are JavaScript, compared without regard to case.
     * <p>
     * 스크립트 블록의 타입: type 속성, 없으면 "text/" 를 붙인 language 속성, 없으면 JavaScript. HTML5 2014 가 나열하는 MIME 타입만 JavaScript 이며 대소문자를 구분하지 않고 비교한다.
     */
    public void testScriptTypeAndLanguage() {
        String[] runs = { "", "type='text/javascript'", "type='TEXT/JavaScript'", "type='application/javascript'", "type='application/ecmascript'",
            "type='application/x-javascript'", "type='application/x-ecmascript'", "type='text/ecmascript'", "type='text/x-ecmascript'",
            "type='text/x-javascript'", "type='text/jscript'", "type='text/livescript'", "type='text/javascript1.0'", "type='text/javascript1.1'",
            "type='text/javascript1.2'", "type='text/javascript1.3'", "type='text/javascript1.4'", "type='text/javascript1.5'", "type=''",
            "type='  text/javascript  '", "language='JavaScript'", "language='javascript1.2'", "language='JScript'", "language='ecmascript'",
            "language='LiveScript'", "language='x-javascript'", "language='x-ecmascript'", "language=''", "language='JAVASCRIPT1.5'",
            "type='text/javascript' language='vbscript'", "type='' language='vbscript'", "type='text/javascript; charset=utf-8'" };
        // 실행해야 하는 속성을 하나씩 확인한다.
        for (int i = 0; i < runs.length; i++) {
            Check.equal("1", runs(runs[i]), "runs: " + runs[i]);
        }
        String[] skips = { "type='text/vbscript'", "type='text/plain'", "type='text/html'", "type='text/xml'", "type='application/xml'",
            "type='application/xhtml+xml'", "type='image/svg+xml'", "type='application/octet-stream'", "type='application/json'",
            "type='text/template'", "type='text/x-handlebars'", "type='text/javascript1.6'", "language='vbscript'", "language='perl'",
            "language='javascript2'", "type='text/plain' language='javascript'", "type='application/javascript1.2'" };
        // 실행하면 안 되는 속성을 하나씩 확인한다.
        for (int i = 0; i < skips.length; i++) {
            Check.equal("0", runs(skips[i]), "does not run: " + skips[i]);
        }
        Check.equal("1", runs("type='module'"), "a module script runs (the module goal)");
    }

    /**
     * A script with <code>for</code> and <code>event</code> attributes runs only for <code>window</code> and <code>onload</code>
     * (HTML5 2014 section 4.11.1.1: the old Internet Explorer form of an event handler); all other pairs are ignored.
     * <p>
     * <code>for</code> 와 <code>event</code> 속성이 있는 스크립트는 <code>window</code> 와 <code>onload</code> 일 때만 실행한다(HTML5 2014 §4.11.1.1: 이벤트 핸들러의 옛 Internet
     * Explorer 형태). 다른 쌍은 모두 무시한다.
     */
    public void testForAndEventAttributes() {
        Check.equal("1", runs("for=window event=onload"), "window + onload");
        Check.equal("1", runs("for=window event='onload()'"), "window + onload()");
        Check.equal("1", runs("for=WINDOW event=ONLOAD"), "case-insensitive");
        Check.equal("1", runs("for=' window ' event=' onload '"), "surrounding white space");
        Check.equal("1", runs("for=window"), "no event attribute: an ordinary script");
        Check.equal("1", runs("event=onload"), "no for attribute: an ordinary script");
        Check.equal("0", runs("for=document event=onclick"), "document + onclick");
        Check.equal("0", runs("for=window event=onclick"), "window + onclick");
        Check.equal("0", runs("for=button1 event=onclick"), "an element name");
        Check.equal("0", runs("for=document event=onload"), "document + onload");
        Check.equal("0", runs("for='' event=onload"), "an empty for");
        Check.equal("0", runs("for=window event=''"), "an empty event");
        Check.equal("0", runs("for=window event='onload(event)'"), "an event with arguments");
    }

    /**
     * A script that a script inserts follows the same rules: a type that is not JavaScript and a <code>for</code> that is not
     * <code>window</code> do not run it.
     * <p>
     * 스크립트가 넣은 스크립트도 같은 규칙을 따른다: JavaScript 가 아닌 type 과 <code>window</code> 가 아닌 <code>for</code> 는 실행하지 않는다.
     */
    public void testInsertedScriptsFollowTheSameRules() {
        PageFixture f = new PageFixture().load("<!doctype html><body><script>window.runs = 0; function add(attrs) { var s = document.createElement('script');"
                + " for (var k in attrs) { s.setAttribute(k, attrs[k]); } s.text = 'window.runs++'; document.body.appendChild(s); }"
                + " add({}); add({type: 'text/vbscript'}); add({language: 'vbscript'}); add({'for': 'document', event: 'onclick'});"
                + " add({language: 'JavaScript'}); add({'for': 'window', event: 'onload'}); add({type: 'text/plain'});</script>");
        Check.equal("3", f.scripts.evaluate("String(window.runs)", "t"), "plain, language=JavaScript, and window + onload run");
    }

    /**
     * The style that hides itself in an HTML comment applies: the CSS tokens of the comment markers are skipped; a CDATA marker in CSS
     * comments is harmless; a style with another type or a media that does not match does not apply.
     * <p>
     * HTML 주석으로 자신을 숨긴 스타일은 적용된다: 주석 표시의 CSS 토큰은 건너뛴다. CSS 주석 안의 CDATA 표시는 해롭지 않다. 다른 타입의 스타일이나 맞지 않는 미디어의 스타일은
     * 적용되지 않는다.
     */
    public void testStyleCompat() {
        String[] styles = { "<style><!-- #d { color: rgb(1, 2, 3) } --></style>", "<style type='text/css'><!--\n#d { color: rgb(1, 2, 3) }\n--></style>",
            "<style type='TEXT/CSS'>#d { color: rgb(1, 2, 3) }</style>", "<style type=''>#d { color: rgb(1, 2, 3) }</style>",
            "<style>/* <![CDATA[ */ #d { color: rgb(1, 2, 3) } /* ]]> */</style>", "<style media='screen'>#d { color: rgb(1, 2, 3) }</style>",
            "<style><!--\n#x { color: red }\n--> #d { color: rgb(1, 2, 3) }</style>" };
        // 적용되어야 하는 스타일을 하나씩 확인한다.
        for (int i = 0; i < styles.length; i++) {
            PageFixture f = CssomSheetsTest.page("<!doctype html>" + styles[i] + "<div id=d>x</div>");
            Check.equal("rgb(1, 2, 3)", f.scripts.evaluate("getComputedStyle(document.getElementById('d')).color", "t"), "applies: " + styles[i]);
        }
        String[] skips = { "<style type='text/less'>#d { color: rgb(1, 2, 3) }</style>", "<style type='text/plain'>#d { color: rgb(1, 2, 3) }</style>",
            "<style media='print'>#d { color: rgb(1, 2, 3) }</style>", "<style type='text/css; charset=utf-8'>#d { color: rgb(1, 2, 3) }</style>" };
        // 적용되면 안 되는 스타일을 하나씩 확인한다.
        for (int i = 0; i < skips.length; i++) {
            PageFixture f = CssomSheetsTest.page("<!doctype html>" + skips[i] + "<div id=d>x</div>");
            Check.equal("rgb(0, 0, 0)", f.scripts.evaluate("getComputedStyle(document.getElementById('d')).color", "t"), "does not apply: " + skips[i]);
        }
    }

    /**
     * The <code>javascript:</code> address is never a target of navigation, nor are the other schemes that old pages used (project
     * policy; plan item P01-12).
     * <p>
     * <code>javascript:</code> 주소는 이동의 대상이 되지 않으며 옛 페이지가 쓰던 다른 스킴도 마찬가지이다(프로젝트 정책, 계획 P01-12).
     */
    public void testJavascriptUrlsAreBlocked() {
        String[] blocked = { "javascript:alert(1)", "JavaScript:void(0)", "  javascript:history.back()", "vbscript:msgbox(1)", "livescript:x", "data:text/html,<p>x</p>",
            "file:///c:/x.html", "mailto:a@b.c", "about:text", "blob:http://a/b" };
        // 허용하지 않는 주소를 하나씩 확인한다.
        for (int i = 0; i < blocked.length; i++) {
            Check.isFalse(SchemePolicy.isNavigationAllowed(blocked[i]), "blocked: " + blocked[i]);
        }
        Check.isTrue(SchemePolicy.isNavigationAllowed("http://a.test/x"), "http");
        Check.isTrue(SchemePolicy.isNavigationAllowed("about:blank"), "about:blank");
    }
}
