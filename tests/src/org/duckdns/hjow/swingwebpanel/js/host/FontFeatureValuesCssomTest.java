package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>@font-feature-values</code> in the style sheet and through the CSSOM (plan item P10-08-h, CSS Fonts Level 4 sections 6.9 and 9.1, WD 12
 * November 2026): the prelude of named families, the error handling of the example of section 6.9.1, the merging of repeated blocks, the serialization,
 * <code>CSSFontFeatureValuesRule</code> with its seven maps and <code>CSSFontFeatureValuesMap</code> as a maplike whose <code>set</code> checks the indexes.
 * <p>
 * 스타일 시트와 CSSOM 의 <code>@font-feature-values</code> 시험(계획 P10-08-h, CSS Fonts Level 4 6.9, 9.1 절): 이름 있는 패밀리의 전문, 6.9.1 절 예의 오류 처리, 되풀이한 블록의
 * 합치기, 직렬화, 일곱 맵을 가진 <code>CSSFontFeatureValuesRule</code>, <code>set</code> 이 번호를 확인하는 maplike <code>CSSFontFeatureValuesMap</code>.
 */
public class FontFeatureValuesCssomTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FontFeatureValuesCssomTest() {
    }

    /**
     * The rule object, its type, its families and its maps; the error handling of the example of section 6.9.1 and the serialization.
     * <p>
     * 규칙 객체, 종류, 패밀리, 맵. 6.9.1 절 예의 오류 처리와 직렬화.
     */
    public void testRuleAndErrorHandling() {
        PageFixture f = CssomSheetsTest.page("<style>@font-feature-values Bongo { @swash { ornate: 1; } annotation { boxed: 4; } @swash { double-loops: 1; flowing: -1; } "
                + "@ornaments ; @styleset { double-W: 14; sharp-terminals: 16 1 } redrum }</style><p>x</p>");
        Check.equal("1|true|14|14|Bongo", f.scripts.evaluate("var r = document.styleSheets[0].cssRules[0]; [document.styleSheets[0].cssRules.length, r instanceof CSSFontFeatureValuesRule, "
                + "r.type, CSSRule.FONT_FEATURE_VALUES_RULE, r.fontFamily].join('|')", "t"), "the rule");
        Check.equal("ornate,double-loops|1|false|double-W,sharp-terminals|16,1|0", f.scripts.evaluate("[[...r.swash.keys()].join(','), r.swash.get('ornate'), "
                + "r.swash.has('flowing'), [...r.styleset.keys()].join(','), r.styleset.get('sharp-terminals').join(','), r.annotation.size].join('|')", "t"),
                "the merged blocks and the dropped declarations");
        Check.equal("@font-feature-values Bongo { @swash { ornate: 1; double-loops: 1; } @styleset { double-W: 14; sharp-terminals: 16 1; } }",
                f.scripts.evaluate("r.cssText", "t"), "the serialization");
    }

    /**
     * Preludes: quoted names, several families, generic families and other tokens make the rule invalid; <code>font-display</code> is kept.
     * <p>
     * 전문: 따옴표 이름, 여러 패밀리. 일반 패밀리와 다른 토큰은 규칙을 무효로 만든다. <code>font-display</code> 는 남는다.
     */
    public void testPreludesAndFontDisplay() {
        PageFixture f = CssomSheetsTest.page("<style>@font-feature-values 'Font One', Font Two { font-display: swap; @character-variant { a: 1 2; b: 3 4 5; } }"
                + " @font-feature-values serif { @swash { x: 1; } } @font-feature-values 12px { @swash { x: 1; } }</style><p>x</p>");
        Check.equal("1|Font One, Font Two|1,2|false", f.scripts.evaluate("var rs = document.styleSheets[0].cssRules; var r = rs[0]; [rs.length, r.fontFamily, "
                + "r.characterVariant.get('a').join(','), r.characterVariant.has('b')].join('|')", "t"), "named families only; three values are too many for a character variant");
        Check.isTrue(f.scripts.evaluate("r.cssText", "t").indexOf("font-display: swap;") > 0, "font-display is serialized: " + f.scripts.evaluate("r.cssText", "t"));
    }

    /**
     * <code>CSSFontFeatureValuesMap</code>: <code>set</code> with a number or a sequence, the checks of the type, <code>delete</code>, <code>clear</code>,
     * <code>forEach</code> and the iterator.
     * <p>
     * <code>CSSFontFeatureValuesMap</code>: 수나 목록으로 <code>set</code>, 종류의 검사, <code>delete</code>, <code>clear</code>, <code>forEach</code>, 반복자.
     */
    public void testValueMap() {
        PageFixture f = CssomSheetsTest.page("<style>@font-feature-values Foo { @styleset { a: 1; } }</style><p>x</p>");
        Check.equal("true|4|2,3|InvalidAccessError|InvalidAccessError", f.scripts.evaluate("var r = document.styleSheets[0].cssRules[0]; var m = r.swash; "
                + "var out = [m.set('x', 4) === m, m.get('x')[0]]; r.styleset.set('y', [2, 3]); out.push(r.styleset.get('y').join(',')); "
                + "try { m.set('z', [1, 2]) } catch (e) { out.push(e.name) } try { r.characterVariant.set('w', [1, 2, 3]) } catch (e) { out.push(e.name) } out.join('|')", "t"),
                "set with a number and a sequence, and the checks of the block types");
        Check.equal("a=1;y=2,3|a:1|2|true|0", f.scripts.evaluate("var seen = []; r.styleset.forEach(function (v, k) { seen.push(k + '=' + v.join(',')); }); "
                + "[seen.join(';'), [...r.styleset][0].join(':'), r.styleset.size, r.styleset.delete('y'), (r.styleset.clear(), r.styleset.size)].join('|')", "t"),
                "forEach, the iterator, delete and clear");
        Check.isTrue(f.scripts.evaluate("r.cssText", "t").indexOf("@swash { x: 4; }") > 0, "the change is in the serialization: " + f.scripts.evaluate("r.cssText", "t"));
    }
}
