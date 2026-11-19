package org.duckdns.hjow.swingwebpanel.css;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.cssom.LoadedStyleSheet;
import org.duckdns.hjow.swingwebpanel.css.cssom.StyleSheetLoader;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssList;
import org.duckdns.hjow.swingwebpanel.css.values.CssUrl;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.dom.NodeList;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Integration tests of the CSS engine (plan items P08-02, P08-08, P08-09): a document with <code>&lt;style&gt;</code>, <code>&lt;link&gt;</code> and
 * <code>@import</code> sources, relative urls, media queries and DOM changes end to end, and the consistency of the selector engine
 * (<code>querySelectorAll</code>) with the cascade (the rules that actually apply to an element).
 * <p>
 * CSS 엔진의 통합 시험(계획 P08-02, P08-08, P08-09): <code>&lt;style&gt;</code>, <code>&lt;link&gt;</code>, <code>@import</code> 출처가 있는 문서, 상대 url, 미디어 질의,
 * DOM 변경을 처음부터 끝까지, 그리고 선택자 엔진(<code>querySelectorAll</code>)과 캐스케이드(요소에 실제로 적용되는 규칙)의 일관성.
 */
public class CssEngineTest {

    /** The sheets served to the loader. / 로더에 제공하는 시트. */
    private final Map<String, String> served = new HashMap<String, String>();

    /** The URLs requested from the loader. / 로더에 요청된 URL. */
    private final List<String> requested = new ArrayList<String>();

    /** An in-memory loader. / 메모리 로더. */
    private final StyleSheetLoader loader = new StyleSheetLoader() {
        public LoadedStyleSheet load(String url) throws IOException {
            requested.add(url);
            String t = served.get(url);
            if (t == null) {
                throw new IOException("404 " + url);
            }
            return new LoadedStyleSheet(t, url);
        }
    };

    /**
     * Creates the suite. 스위트를 만든다.
     */
    public CssEngineTest() {
    }

    /**
     * A complete page: style, link, import, relative urls, media and the cascade. 완전한 페이지: style, link, import, 상대 url, 미디어, 캐스케이드.
     */
    public void testPage() {
        served.put("http://example.com/site/css/main.css", "@import url(base.css); .box { background: url(../img/bg.png) no-repeat; color: navy }");
        served.put("http://example.com/site/css/base.css", "body { font: 14px/1.5 Georgia, serif } .box { color: gray; padding: 4px }");
        served.put("http://example.com/site/css/wide.css", ".box { margin-left: 100px }");
        Document doc = MiniHtml.parse("<html><head><link rel=\"stylesheet\" href=\"css/main.css\">"
                + "<link rel=\"stylesheet\" href=\"css/wide.css\" media=\"(min-width: 900px)\">"
                + "<style>.box { border: 1px solid red } #b { width: 50% }</style></head>"
                + "<body><div id=\"b\" class=\"box\" style=\"padding-top: 9px\">hello</div></body></html>");
        doc.setUrl("http://example.com/site/page.html");
        CssEngine e = new CssEngine(doc, loader, new MediaEnvironment(800, 600));
        e.loadStyleSheets();
        Check.equal(3, e.getStyleSheets().size(), "two links and one style element");
        Check.equal(3, requested.size(), "main.css, its import base.css and wide.css were loaded");
        Check.equal("http://example.com/site/css/base.css", requested.get(1), "the import is resolved against the importing sheet");
        ComputedStyle s = e.getComputedStyle(doc.getElementById("b"));
        Check.equal("rgb(0, 0, 128)", s.getPropertyValue("color"), "the later sheet wins at equal specificity: navy beats gray");
        Check.near(14, s.fontSizePx(), 1e-9, "font from the imported sheet reaches the descendants");
        Check.near(21, s.lineHeightPx() * 1.0, 1e-9, "line height 1.5 of 14px");
        Check.equal("Georgia", e.getComputedStyle(doc.getElementById("b")).fontFamilies().get(0), "font family");
        Check.near(9, s.px(Prop.PADDING_TOP), 1e-9, "the style attribute beats the sheets");
        Check.near(4, s.px(Prop.PADDING_LEFT), 1e-9, "other padding from the imported sheet");
        Check.near(1, s.px(Prop.BORDER_LEFT_WIDTH), 1e-9, "border from the style element");
        Check.equal("50%", s.get(Prop.WIDTH).toCssText(), "percentages stay percentages");
        Check.near(0, s.px(Prop.MARGIN_LEFT), 1e-9, "the media dependent link does not apply to an 800 pixel viewport");
        CssList bg = (CssList) s.get(Prop.BACKGROUND_IMAGE);
        Check.equal("http://example.com/site/img/bg.png", ((CssUrl) bg.get(0)).getResolvedUrl(), "relative url resolved against the style sheet, not the page");
        e.getResolver().setMediaEnvironment(new MediaEnvironment(1000, 600));
        Check.near(100, e.getComputedStyle(doc.getElementById("b")).px(Prop.MARGIN_LEFT), 1e-9, "the media dependent link applies after a resize");
        Check.isFalse(e.refreshIfNeeded(), "nothing changed in the sources");
        Element style = (Element) doc.getElementsByTagName("style").item(0);
        style.setTextContent(".box { color: red }");
        Check.isTrue(e.refreshIfNeeded(), "the text of the style element changed");
        Check.equal("rgb(255, 0, 0)", e.getComputedStyle(doc.getElementById("b")).getPropertyValue("color"), "the new rule applies");
        Check.near(0, e.getComputedStyle(doc.getElementById("b")).px(Prop.BORDER_LEFT_WIDTH), 1e-9, "the old rule is gone");
        e.dispose();
    }

    /**
     * Diagnostics tell what was dropped. 진단은 버려진 것을 알려 준다.
     */
    public void testDiagnostics() {
        Document doc = MiniHtml.parse("<html><head><style>a { color: ; foo: 1; width: 10px } p::bogus { x: y } @unknown { } b { grid-gap: 1px; display: grid }"
                + "</style><link rel=\"stylesheet\" href=\"missing.css\"></head><body></body></html>");
        doc.setUrl("http://example.com/index.html");
        CssEngine e = new CssEngine(doc, loader, new MediaEnvironment());
        e.loadStyleSheets();
        CssDiagnostics d = e.getDiagnostics();
        Check.isTrue(d.has(CssDiagnostics.UNKNOWN, "foo"), "unknown property");
        Check.isTrue(d.has(CssDiagnostics.INVALID, "color"), "empty value");
        Check.isTrue(d.has(CssDiagnostics.UNKNOWN, "@unknown"), "unknown at-rule");
        Check.isTrue(d.count(CssDiagnostics.INVALID) >= 3, "the invalid selector and the failing link are reported too: " + d.getEntries());
        Check.equal(1, e.getStyleSheets().size(), "the style element was collected, the failing link was not");
        e.dispose();
    }

    /**
     * The selector engine and the cascade agree: an element gets a marker declaration if and only if querySelectorAll finds it. 선택자 엔진과 캐스케이드가
     * 일치한다: 요소는 querySelectorAll 이 찾는 경우에만 표식 선언을 얻는다.
     */
    public void testSelectorsAgreeWithTheCascade() {
        String page = "<html><body><div id=\"root\" class=\"a b\" lang=\"en\"><ul id=\"u\"><li class=\"i\">1</li><li class=\"i on\">2</li><li>3</li><li class=\"i\">4</li></ul>"
                + "<p id=\"p1\" class=\"x\"><span>s1</span><em>e</em><span class=\"on\">s2</span></p><p id=\"p2\" title=\"Hello World\"></p>"
                + "<a href=\"/h\">l</a><input type=\"checkbox\" checked><input disabled><table><tbody><tr><td>1</td><td>2</td></tr><tr><td>3</td></tr></tbody></table>"
                + "<section><div><p class=\"deep\">d</p></div></section></div><p id=\"tail\">t</p></body></html>";
        String[] selectors = {"li", "li.i", "li:nth-child(odd)", "li:nth-child(2 of .i)", "li:not(.i)", "ul > li:last-child", "li.i + li", "li ~ li.i", "p span", "p > span:first-of-type",
            "p:empty", "p:has(> em)", "p:has(span.on)", "div > :is(ul, p)", ":is(div, section) p", "[title]", "[title^=Hello]", "[title*=\"o W\"]", "a:link", "a:any-link",
            "input:checked", "input:disabled", "input:enabled", "td:nth-child(2)", "tr:first-child td", "tr:last-child > td:only-child", "#root > *", "div:lang(en) p",
            "p.x, p#p2", "*:not(div):not(p):not(li)", "section div p.deep", "html > body > div:first-child", ":root", "body :not(p) > span", "td:nth-last-child(1)"};
        // 선택자마다 표식 선언 z-index: 4242 를 주는 규칙을 만들어 querySelectorAll 의 결과와 비교한다.
        for (int i = 0; i < selectors.length; i++) {
            Document doc = MiniHtml.parse(page);
            CssEngine e = new CssEngine(doc, null, new MediaEnvironment());
            e.getResolver().setStyleSheets(java.util.Arrays.asList(org.duckdns.hjow.swingwebpanel.css.cssom.StyleSheet.parse(selectors[i] + " { z-index: 4242 }", null,
                    org.duckdns.hjow.swingwebpanel.css.cssom.StyleSheet.ORIGIN_AUTHOR, null, null)));
            NodeList found = doc.querySelectorAll(selectors[i]);
            Set<Node> expected = new HashSet<Node>(found.toList());
            Set<Node> actual = new HashSet<Node>();
            // 문서의 모든 요소에서 표식을 가진 것을 모은다.
            for (Node n = doc; n != null; n = Node.nextInTree(n, doc)) {
                if (n instanceof Element && e.getComputedStyle((Element) n).number(Prop.Z_INDEX) == 4242) {
                    actual.add(n);
                }
            }
            Check.equal(expected.size(), actual.size(), "selector '" + selectors[i] + "' matches the same number of elements in both engines");
            Check.isTrue(expected.equals(actual), "selector '" + selectors[i] + "' matches the same elements in both engines");
            e.dispose();
        }
    }
}
