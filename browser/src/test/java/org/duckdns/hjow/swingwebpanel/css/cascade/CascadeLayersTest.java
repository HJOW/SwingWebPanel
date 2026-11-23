package org.duckdns.hjow.swingwebpanel.css.cascade;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.css.cssom.LoadedStyleSheet;
import org.duckdns.hjow.swingwebpanel.css.cssom.StyleSheetLoader;
import org.duckdns.hjow.swingwebpanel.css.cssom.SupportsCondition;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of cascade layers (plan item P10-08, CSS Cascading and Inheritance Level 5, https://www.w3.org/TR/css-cascade-5/, section 6):
 * <code>@layer</code> blocks and statements, the order of the layers by their first appearance, the nested and the dotted layers and the
 * anonymous layers, an unlayered rule that beats a layered one whatever its specificity, the reversed order of <code>!important</code>
 * declarations, the <code>style</code> attribute, and the conditions of <code>@supports selector()</code>.
 * <p>
 * 캐스케이드 레이어 시험(계획 P10-08, CSS Cascading and Inheritance Level 5, https://www.w3.org/TR/css-cascade-5/, 6 절): <code>@layer</code> 블록과 문장, 처음 나온 순서의
 * 레이어 순서, 중첩 레이어와 점으로 이은 레이어와 이름 없는 레이어, 구체성에 상관없이 레이어 있는 규칙을 이기는 레이어 없는 규칙, 뒤집힌 <code>!important</code> 순서,
 * <code>style</code> 속성, <code>@supports selector()</code> 조건.
 */
public class CascadeLayersTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public CascadeLayersTest() {
    }

    /**
     * Returns the computed color of the element <code>d</code> of a page with a style sheet.
     * <p>
     * 스타일시트가 있는 페이지의 요소 <code>d</code> 의 계산된 색을 반환한다.
     *
     * @param css       the style sheet / 스타일시트
     * @param attribute the extra attributes of the element / 요소의 추가 속성
     * @return the color as 0xRRGGBB / 0xRRGGBB 색
     */
    private static int color(String css, String attribute) {
        Document doc = HtmlParser.parseDocument("<!DOCTYPE html><html><head><style>" + css + "</style></head><body><div id=d class=k " + attribute
                + ">x</div></body></html>", "http://t.test/dir/page.html", null, true);
        CssEngine engine = new CssEngine(doc, null, new MediaEnvironment(300, 200));
        engine.loadStyleSheets();
        ComputedStyle s = engine.getComputedStyle(doc.getElementById("d"));
        return s.color(Prop.COLOR) & 0xFFFFFF;
    }

    /** The color red used by the tests. 시험에서 쓰는 빨강. */
    private static final int RED = 0xFF0000;
    /** The color blue used by the tests. 시험에서 쓰는 파랑. */
    private static final int BLUE = 0x0000FF;
    /** The color green used by the tests. 시험에서 쓰는 초록. */
    private static final int GREEN = 0x008000;

    /**
     * An unlayered rule beats a layered rule, whatever the specificity and the order of the two.
     * <p>
     * 레이어 없는 규칙은 두 규칙의 구체성과 순서에 상관없이 레이어 있는 규칙을 이긴다.
     */
    public void testUnlayeredBeatsLayered() {
        Check.equal(BLUE, color("@layer base { #d.k { color: red } } div { color: blue }", ""), "an unlayered rule of low specificity wins");
        Check.equal(BLUE, color("div { color: blue } @layer base { #d.k { color: red } }", ""), "also when it comes first");
        Check.equal(RED, color("@layer base { div { color: blue } div { color: red } }", ""), "inside a layer the order of the rules decides");
        Check.equal(RED, color("@layer base { div { color: blue } #d { color: red } }", ""), "and the specificity");
        Check.equal(0x00FF00, color("@layer base { #d { color: red } }", "style='color:#0f0'"), "the style attribute is unlayered and wins");
    }

    /**
     * The layers are ordered by the first appearance of their names, also by a statement; a later layer beats an earlier one.
     * <p>
     * 레이어는 이름이 처음 나온 순서(문장 포함)로 정렬되며 나중 레이어가 앞선 레이어를 이긴다.
     */
    public void testLayerOrder() {
        Check.equal(RED, color("@layer a { div { color: blue } } @layer b { div { color: red } }", ""), "b comes after a");
        Check.equal(BLUE, color("@layer b { div { color: red } } @layer a { div { color: blue } }", ""), "without a statement b is named first, so a is above b");
        Check.equal(RED, color("@layer a, b; @layer b { div { color: red } } @layer a { div { color: blue } }", ""), "the statement fixes the order: b is above a although a is later in the source");
    }

    /**
     * Nested layers: the sub-layers of a layer come before the rules of the layer itself; dotted names make sub-layers; anonymous layers
     * are ordered by their appearance.
     * <p>
     * 중첩 레이어: 레이어의 하위 레이어는 그 레이어 자신의 규칙보다 앞서고, 점으로 이은 이름은 하위 레이어를 만들며, 이름 없는 레이어는 나온 순서로 정렬된다.
     */
    public void testNestedAndAnonymous() {
        Check.equal(BLUE, color("@layer outer { @layer inner { div { color: red } } div { color: blue } }", ""), "the rules of the outer layer beat its sub-layer");
        Check.equal(BLUE, color("@layer x { @layer p { div { color: red } } @layer q { div { color: blue } } }", ""), "the later sub-layer wins");
        Check.equal(RED, color("@layer a { div { color: red } } @layer a.b { div { color: blue } }", ""), "a dotted name is a sub-layer, below the rules of a");
        Check.equal(BLUE, color("@layer { div { color: red } } @layer { div { color: blue } }", ""), "the later anonymous layer wins");
        Check.equal(RED, color("@layer { div { color: blue } } div { color: red }", ""), "an anonymous layer is layered");
        Check.equal(BLUE, color("@layer a { @layer b { div { color: red } } } @layer c { div { color: blue } }", ""), "layers of the top level are compared by the top level");
        Check.equal(RED, color("@layer c, a; @layer a { @layer b { div { color: red } } } @layer c { div { color: blue } }", ""), "a statement before fixes the top level order c, a");
    }

    /**
     * A loader with sheets in memory.
     * <p>
     * 시트를 메모리에 가진 로더.
     */
    private static final class MapLoader implements StyleSheetLoader {
        /** The sheets by absolute URL. 절대 URL 별 시트. */
        final Map<String, String> sheets = new HashMap<String, String>();

        /**
         * Loads a sheet.
         * <p>
         * 시트를 읽는다.
         *
         * @param url the URL / URL
         * @return the sheet / 시트
         * @throws IOException if the URL is unknown / URL 을 모르면
         */
        public LoadedStyleSheet load(String url) throws IOException {
            String s = sheets.get(url);
            if (s == null) {
                throw new IOException("404 " + url);
            }
            return new LoadedStyleSheet(s, url);
        }
    }

    /**
     * Returns the computed color of the element <code>d</code> of a page whose style element imports sheets.
     * <p>
     * style 요소가 시트를 가져오는 페이지의 요소 <code>d</code> 의 계산된 색을 반환한다.
     *
     * @param css    the style sheet text of the style element / style 요소의 스타일시트 문자열
     * @param imp the text of the imported sheet <code>imp.css</code> / 가져온 시트 <code>imp.css</code> 의 문자열
     * @return the color as 0xRRGGBB / 0xRRGGBB 색
     */
    private static int imported(String css, String imp) {
        MapLoader loader = new MapLoader();
        loader.sheets.put("http://t.test/dir/imp.css", imp);
        Document doc = HtmlParser.parseDocument("<!DOCTYPE html><html><head><style>" + css + "</style></head><body><div id=d>x</div></body></html>",
                "http://t.test/dir/page.html", null, true);
        CssEngine engine = new CssEngine(doc, loader, new MediaEnvironment(300, 200));
        engine.loadStyleSheets();
        return engine.getComputedStyle(doc.getElementById("d")).color(Prop.COLOR) & 0xFFFFFF;
    }

    /**
     * <code>@import ... layer(name)</code> puts the imported sheet into a layer; without <code>layer</code> the import is unlayered.
     * <p>
     * <code>@import ... layer(name)</code> 는 가져온 시트를 레이어에 넣는다. <code>layer</code> 가 없으면 가져온 시트는 레이어가 없다.
     */
    public void testImportIntoLayer() {
        Check.equal(RED, imported("@layer a, b; @import url(imp.css) layer(b); @layer a { div { color: blue } }", "div { color: red }"), "the import is in the layer b, above a");
        Check.equal(BLUE, imported("@layer b, a; @import url(imp.css) layer(b); @layer a { div { color: blue } }", "div { color: red }"), "with the order b, a the layer a wins");
        Check.equal(RED, imported("@import url(imp.css); @layer a { div { color: blue } }", "div { color: red }"), "an import without layer() is unlayered and beats a layer");
        Check.equal(BLUE, imported("@import url(imp.css) layer; @layer { div { color: blue } }", "div { color: red }"), "an anonymous import layer comes before the later anonymous layer");
    }

    /**
     * <code>!important</code> reverses the order: an earlier layer wins, and an unlayered important declaration loses to a layered one.
     * <p>
     * <code>!important</code> 는 순서를 뒤집는다: 앞선 레이어가 이기고 레이어 없는 중요한 선언은 레이어 있는 것에 진다.
     */
    public void testImportantIsReversed() {
        Check.equal(RED, color("@layer a, b; @layer a { div { color: red !important } } @layer b { div { color: blue !important } }", ""), "the earlier layer wins for !important");
        Check.equal(RED, color("@layer a { div { color: red !important } } div { color: green !important }", ""), "a layered !important beats an unlayered !important");
        Check.equal(RED, color("@layer a { div { color: red !important } }", "style='color:#0f0'"), "a layered !important beats the style attribute");
        Check.equal(GREEN, color("@layer a { div { color: red } } div { color: green !important }", ""), "an unlayered !important beats a layered normal declaration");
        Check.equal(BLUE, color("div { color: red } @layer a { div { color: blue !important } }", ""), "an important layered declaration beats a normal unlayered one");
    }

    /**
     * <code>@supports selector()</code>: true for a selector that parses, false for one that does not, and with not, and, or.
     * <p>
     * <code>@supports selector()</code>: 파싱되는 선택자는 참, 아니면 거짓이며 not, and, or 와 함께 쓸 수 있다.
     */
    public void testSupportsSelector() {
        Check.isTrue(SupportsCondition.evaluate("selector(:is(a, b))"), "selector(:is()) parses");
        Check.isTrue(SupportsCondition.evaluate("selector(div > p + span)"), "combinators");
        Check.isTrue(SupportsCondition.evaluate("selector(a:not(.x):hover)"), "pseudo-classes");
        Check.isFalse(SupportsCondition.evaluate("selector(div >)"), "a dangling combinator");
        Check.isFalse(SupportsCondition.evaluate("selector(:bogus-class)"), "an unknown pseudo-class");
        Check.isTrue(SupportsCondition.evaluate("not selector(:bogus-class)"), "not");
        Check.isTrue(SupportsCondition.evaluate("selector(a) and (display: block)"), "and");
        Check.isTrue(SupportsCondition.evaluate("selector(:bogus) or selector(p)"), "or");
        Check.equal(RED, color("@supports selector(div.k) { div { color: red } }", ""), "the rules of a true @supports apply");
        Check.equal(0, color("@supports selector(:bogus) { div { color: red } }", ""), "the rules of a false @supports are dropped (the color stays black)");
    }
}
