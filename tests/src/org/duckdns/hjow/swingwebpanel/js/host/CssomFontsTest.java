package org.duckdns.hjow.swingwebpanel.js.host;

import java.util.List;

import org.duckdns.hjow.swingwebpanel.css.cssom.CssRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.FontFaceRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.StyleSheet;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>document.fonts</code> and <code>FontFace</code> (CSS Font Loading Module Level 3, DOM-13): the set lists the <code>@font-face</code> rules of
 * the style sheets, <code>ready</code>, <code>check()</code>, <code>load()</code> and the set methods work, a face that a script adds reaches the style
 * engine as a <code>@font-face</code> rule of an adopted sheet, and a face with a source that cannot be parsed is an error.
 * <p>
 * <code>document.fonts</code> 와 <code>FontFace</code> 의 시험(CSS Font Loading Module Level 3, DOM-13): 집합은 스타일 시트의 <code>@font-face</code> 규칙을 나열하고,
 * <code>ready</code>, <code>check()</code>, <code>load()</code>, 집합 메서드가 동작하며, 스크립트가 더한 글꼴 면은 채택한 시트의 <code>@font-face</code> 규칙으로 스타일 엔진에
 * 닿고, 해석할 수 없는 출처를 가진 글꼴 면은 오류이다.
 */
public class CssomFontsTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public CssomFontsTest() {
    }

    /**
     * Counts the <code>@font-face</code> rules that the style engine of the page has in all its sheets for a family.
     * <p>
     * 페이지의 스타일 엔진이 모든 시트에 가진 한 글꼴 계열의 <code>@font-face</code> 규칙 수를 센다.
     *
     * @param f      the fixture / 시험 도구
     * @param family the family / 글꼴 계열
     * @return the count / 개수
     */
    private static int engineFaces(PageFixture f, String family) {
        int n = 0;
        List<StyleSheet> sheets = f.env.cssEngine.getStyleSheets();
        // 엔진이 쓰는 시트(채택한 시트 포함)의 규칙에서 그 계열의 글꼴 규칙을 센다.
        for (int i = 0; i < sheets.size(); i++) {
            List<CssRule> rules = sheets.get(i).getRules();
            // 시트 안의 규칙을 훑는다.
            for (int j = 0; j < rules.size(); j++) {
                if (rules.get(j) instanceof FontFaceRule && family.equals(((FontFaceRule) rules.get(j)).getFamily())) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * DOM-13: the set lists the faces of the style sheets and has the members of a set.
     * <p>
     * DOM-13: 집합은 스타일 시트의 글꼴 면을 나열하고 집합의 멤버를 가진다.
     */
    public void testFontFaceSetOfTheStyleSheets() {
        PageFixture f = CssomSheetsTest.page("<style>@font-face { font-family: Foo; src: url(foo.woff); font-weight: bold }\n@font-face { font-family: Bar; src: local(Arial) }</style><p>x</p>");
        Check.equal("true|true|2|loaded", f.scripts.evaluate("[document.fonts instanceof FontFaceSet, document.fonts instanceof EventTarget, document.fonts.size, "
                + "document.fonts.status].join('|')", "t"), "the set");
        Check.equal("Foo|bold|true|Bar|normal", f.scripts.evaluate("var list = Array.from(document.fonts); [list[0].family, list[0].weight, list[0] instanceof FontFace, list[1].family, list[1].style].join('|')", "t"),
                "the faces of the rules");
        Check.equal("loaded|loaded", f.scripts.evaluate("[list[0].status, list[1].status].join('|')", "t"), "they count as loaded");
        Check.equal("2|2|true", f.scripts.evaluate("var n = 0, same = true; document.fonts.forEach(function (a, b, s) { n++; same = same && a === b && s === document.fonts; }); "
                + "[n, document.fonts.entries().next().value.length, same].join('|')", "t"), "forEach and entries");
        Check.equal("true|true|false", f.scripts.evaluate("[document.fonts.has(list[0]), document.fonts.has(list[1]), document.fonts.has(new FontFace('Z', 'url(z.woff)'))].join('|')", "t"),
                "has");
        Check.equal("[object FontFaceSet]|true", f.scripts.evaluate("Object.prototype.toString.call(document.fonts) + '|' + (document.fonts === document.fonts)", "t"), "the interface and identity");
    }

    /**
     * DOM-13: <code>ready</code>, <code>check()</code> and <code>load()</code>.
     * <p>
     * DOM-13: <code>ready</code>, <code>check()</code>, <code>load()</code> 의 동작.
     */
    public void testReadyCheckAndLoad() {
        PageFixture f = CssomSheetsTest.page("<style>@font-face { font-family: Foo; src: url(foo.woff) }</style><p>x</p>");
        f.scripts.evaluate("window.res = 'pending'; document.fonts.ready.then(function (s) { window.res = (s === document.fonts) + ':' + s.status; });", "t");
        Check.equal("true:loaded", f.scripts.evaluate("window.res", "t"), "ready is fulfilled with the set");
        Check.equal("true|true|true", f.scripts.evaluate("[document.fonts.check('16px Foo'), document.fonts.check('bold 12px \"Foo\", serif'), document.fonts.check('16px NoSuch')].join('|')", "t"),
                "check: faces of the sheets are loaded, no matching face is true");
        Check.equal("SyntaxError", f.scripts.evaluate("try { document.fonts.check('nonsense') } catch (e) { e.name }", "t"), "a font that cannot be parsed");
        f.scripts.evaluate("window.loadedCount = -1; document.fonts.load('16px Foo').then(function (a) { window.loadedCount = a.length + ':' + a[0].family; });", "t");
        Check.equal("1:Foo", f.scripts.evaluate("window.loadedCount", "t"), "load resolves with the matching faces");
        f.scripts.evaluate("window.none = -1; document.fonts.load('16px NoSuch').then(function (a) { window.none = a.length; });", "t");
        Check.equal("0", f.scripts.evaluate("String(window.none)", "t"), "load of an unknown family resolves with an empty array");
        Check.equal("TypeError", f.scripts.evaluate("try { document.fonts.load() } catch (e) { e.name }", "t"), "the font is required");
    }

    /**
     * DOM-13: a face that a script makes: the descriptors, the status, <code>load()</code>, adding to the set and the effect on the style engine.
     * <p>
     * DOM-13: 스크립트가 만든 글꼴 면: 디스크립터, 상태, <code>load()</code>, 집합에 더하기, 스타일 엔진에 대한 효과.
     */
    public void testScriptFontFace() {
        PageFixture f = CssomSheetsTest.page("<p>x</p>");
        Check.equal("Web|normal|normal|normal|U+0-10FFFF|normal|normal|auto|unloaded", f.scripts.evaluate("var ff = new FontFace('Web', 'url(w.woff2) format(\"woff2\")'); "
                + "[ff.family, ff.style, ff.weight, ff.stretch, ff.unicodeRange, ff.variant, ff.featureSettings, ff.display, ff.status].join('|')", "t"), "defaults");
        Check.equal("italic|700|swap", f.scripts.evaluate("var g = new FontFace('Web2', 'url(w2.woff)', { style: 'italic', weight: '700', display: 'swap' }); "
                + "[g.style, g.weight, g.display].join('|')", "t"), "descriptors");
        Check.equal("true|[object FontFace]|function", f.scripts.evaluate("[ff.loaded instanceof Promise, Object.prototype.toString.call(ff), typeof ff.load].join('|')", "t"), "the interface");
        Check.equal("false|0", f.scripts.evaluate("document.fonts.has(ff) + '|' + document.fonts.size", "t"), "not in the set yet");
        Check.equal("true|1|true", f.scripts.evaluate("[document.fonts.add(ff) === document.fonts, document.fonts.size, document.fonts.has(ff)].join('|')", "t"), "add returns the set");
        Check.equal(1, engineFaces(f, "Web"), "the face reached the style engine as a @font-face rule");
        Check.equal("0", f.scripts.evaluate("String(document.styleSheets.length)", "t"), "the hidden sheet is not part of document.styleSheets");
        Check.equal("0", f.scripts.evaluate("String(document.adoptedStyleSheets.length)", "t"), "and not of adoptedStyleSheets");
        Check.equal("false|true", f.scripts.evaluate("[document.fonts.check('16px Web'), true].join('|')", "t"), "an unloaded face makes check false");
        f.scripts.evaluate("window.state = 'pending'; ff.loaded.then(function (x) { window.state = x.status; }); ff.load();", "t");
        Check.equal("loaded|true", f.scripts.evaluate("window.state + '|' + document.fonts.check('16px Web')", "t"), "load() settles the loaded promise and the status");
        Check.equal("2", f.scripts.evaluate("document.fonts.add(g); String(document.fonts.size)", "t"), "a second face");
        Check.equal(2, engineFaces(f, "Web") + engineFaces(f, "Web2"), "both reached the engine");
        Check.equal("true|1|0", f.scripts.evaluate("[document.fonts.delete(ff), document.fonts.size, 0].join('|')", "t"), "delete");
        Check.equal(0, engineFaces(f, "Web"), "and the rule left the engine");
        Check.equal("false", f.scripts.evaluate("String(document.fonts.delete(ff))", "t"), "deleting again");
        Check.equal("0", f.scripts.evaluate("document.fonts.clear(); String(document.fonts.size)", "t"), "clear");
        Check.equal(0, engineFaces(f, "Web2"), "everything left the engine");
        Check.equal("TypeError", f.scripts.evaluate("try { document.fonts.add({}) } catch (e) { e.name }", "t"), "only faces can be added");
        Check.equal("TypeError", f.scripts.evaluate("try { new FontFace('x') } catch (e) { e.name }", "t"), "the source is required");
        Check.equal("TypeError", f.scripts.evaluate("try { FontFace('x', 'url(a)') } catch (e) { e.name }", "t"), "the constructor needs new");
        Check.equal("SyntaxError", f.scripts.evaluate("try { ff.weight = '' } catch (e) { e.name }", "t"), "an empty descriptor");
    }

    /**
     * DOM-13: a face with a source that is not a list of <code>url()</code> and <code>local()</code> is an error, and its promise is rejected.
     * <p>
     * DOM-13: <code>url()</code> 과 <code>local()</code> 의 목록이 아닌 출처를 가진 글꼴 면은 오류이며 그 프라미스는 reject 된다.
     */
    public void testFontFaceWithAnInvalidSource() {
        PageFixture f = CssomSheetsTest.page("<p>x</p>");
        Check.equal("error", f.scripts.evaluate("var bad = new FontFace('Bad', 'nonsense'); bad.status", "t"), "the status");
        f.scripts.evaluate("window.why = 'pending'; bad.loaded.then(null, function (e) { window.why = e.name; });", "t");
        Check.equal("SyntaxError", f.scripts.evaluate("window.why", "t"), "loaded is rejected with a SyntaxError");
        Check.equal("error", f.scripts.evaluate("bad.load(); bad.status", "t"), "load() does not repair it");
        Check.equal(0, engineFaces(f, "Bad"), "a face in error never reaches the engine");
        f.scripts.evaluate("document.fonts.add(bad);", "t");
        Check.equal(0, engineFaces(f, "Bad"), "even when it is added to the set");
        Check.equal("loaded", f.scripts.evaluate("var buf = new FontFace('Bin', new ArrayBuffer(8)); buf.status", "t"), "binary data is accepted as loaded");
    }
}
