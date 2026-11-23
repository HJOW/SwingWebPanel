package org.duckdns.hjow.swingwebpanel.css;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.css.cssom.CssRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.GroupRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.ImportRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.KeyframesRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.LoadedStyleSheet;
import org.duckdns.hjow.swingwebpanel.css.cssom.MediaList;
import org.duckdns.hjow.swingwebpanel.css.cssom.StyleRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.StyleSheet;
import org.duckdns.hjow.swingwebpanel.css.cssom.StyleSheetLoader;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the Java side of the CSS Object Model (DOM-13), without scripts: the editing of rule lists with the errors of the CSSOM, the media list, the
 * change notifications that reach the engine, the identity of a sheet while the document collects its sheets again (also while an <code>@import</code> was
 * pending), and the adopted sheets of the engine.
 * <p>
 * 스크립트 없이 확인하는 CSS 객체 모델(DOM-13)의 Java 쪽 시험: CSSOM 의 오류가 있는 규칙 목록 편집, 미디어 목록, 엔진에 닿는 변경 통지, 문서가 시트를 다시 모을 때(<code>@import</code>
 * 가 대기 중이었을 때도) 시트의 동일성, 엔진의 채택한 시트.
 */
public class CssomModelTest {

    /** The sheets that the loader serves by URL; a URL without an entry fails like a request that is still pending. / 로더가 URL 별로 내놓는 시트. 항목이 없는 URL 은 아직 오지 않은 요청처럼 실패한다. */
    private final Map<String, String> served = new HashMap<String, String>();

    /** The loader of the tests. / 시험의 로더. */
    private final StyleSheetLoader loader = new StyleSheetLoader() {
        /**
         * Serves a sheet or fails.
         * <p>
         * 시트를 내놓거나 실패한다.
         *
         * @param url the URL / URL
         * @return the sheet / 시트
         * @throws IOException if there is none (yet) / 아직 없으면
         */
        public LoadedStyleSheet load(String url) throws IOException {
            String t = served.get(url);
            if (t == null) {
                throw new IOException("pending " + url);
            }
            return new LoadedStyleSheet(t, url);
        }
    };

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public CssomModelTest() {
    }

    /**
     * Parses a sheet without a loader.
     * <p>
     * 로더 없이 시트를 파싱한다.
     *
     * @param text the text / 텍스트
     * @return the sheet / 시트
     */
    private static StyleSheet sheet(String text) {
        return StyleSheet.parse(text, "http://example.test/a.css", StyleSheet.ORIGIN_AUTHOR, null, null);
    }

    /**
     * Runs an edit and returns the name of the DOM exception it throws, or "none".
     * <p>
     * 편집을 실행하고 던진 DOM 예외의 이름을 반환한다. 없으면 "none".
     *
     * @param s     the sheet / 시트
     * @param rule  the rule text / 규칙 텍스트
     * @param index the index / 위치
     * @return the name / 이름
     */
    private static String insertError(StyleSheet s, String rule, int index) {
        try {
            s.insertRule(rule, index);
            return "none";
        } catch (DomException e) {
            return e.getName();
        }
    }

    /**
     * DOM-13: the editing of a rule list: positions, the errors, the container links and the order of the rules.
     * <p>
     * DOM-13: 규칙 목록의 편집: 위치, 오류, 컨테이너 링크, 규칙 순서.
     */
    public void testEditingRuleLists() {
        StyleSheet s = sheet("a { color: red }");
        Check.equal(1, s.insertRule("b { color: blue }", 1), "the position is returned");
        Check.equal(0, s.insertRule("@media print { c { color: green } }", 0), "a group at the start");
        Check.equal(3, s.getRules().size(), "three rules");
        Check.isTrue(s.getRules().get(0) instanceof GroupRule, "the media rule is first");
        Check.same(s, s.getRules().get(2).getParentStyleSheet(), "the parent sheet of a top level rule");
        Check.isNull(s.getRules().get(2).getParentRule(), "no parent rule at the top level");
        GroupRule g = (GroupRule) s.getRules().get(0);
        Check.same(g, g.getRules().get(0).getParentRule(), "the parent rule of a nested rule");
        Check.same(s, g.getRules().get(0).getParentStyleSheet(), "the parent sheet of a nested rule is the sheet");
        Check.equal("IndexSizeError", insertError(s, "d { }", 9), "an index beyond the end");
        Check.equal("SyntaxError", insertError(s, "d { } e { }", 0), "two rules");
        Check.equal("SyntaxError", insertError(s, "@charset \"x\";", 0), "@charset");
        Check.equal("SyntaxError", insertError(s, "", 0), "nothing");
        Check.equal("HierarchyRequestError", insertError(s, "@import url(x.css);", 1), "@import after a rule");
        Check.equal("InvalidStateError", insertError(s, "@namespace x url(http://x.test/);", 0), "@namespace into a sheet with rules");
        Check.equal(3, s.getRules().size(), "the failed edits changed nothing");
        CssRule removed = s.getRules().get(1);
        s.deleteRule(1);
        Check.isNull(removed.getParentStyleSheet(), "a deleted rule has no parent sheet");
        Check.equal(2, s.getRules().size(), "two rules after the deletion");
        Check.equal(2, s.getCssRules().getLength(), "the rule list follows");
        Check.same(s.getRules().get(1), s.getCssRules().item(1), "item");
        Check.isNull(s.getCssRules().item(2), "item beyond the end");
        Check.same(s.getCssRules(), s.getCssRules(), "the same rule list object every time");
        StyleSheet n = sheet("@import url(x.css);\n@namespace svg url(http://www.w3.org/2000/svg);");
        Check.equal(2, n.getRules().size(), "an import and a namespace");
        Check.equal("none", insertError(n, "@namespace html url(http://www.w3.org/1999/xhtml);", 2), "another namespace at the end");
        Check.equal("none", insertError(n, "@import url(y.css);", 0), "an import at the start");
        Check.equal("HierarchyRequestError", insertError(n, "@import url(y.css);", 3), "an import after a namespace");
        Check.equal("HierarchyRequestError", insertError(n, "a { }", 0), "a style rule before the prologue");
        Check.equal("none", insertError(n, "a { }", 4), "a style rule after the prologue");
        Check.equal("InvalidStateError", deleteError(n, 2), "deleting a namespace while there are other rules");
        Check.equal("IndexSizeError", deleteError(n, 9), "deleting beyond the end");
    }

    /**
     * Deletes a rule and returns the name of the exception, or "none".
     * <p>
     * 규칙을 지우고 예외 이름을 반환한다. 없으면 "none".
     *
     * @param s     the sheet / 시트
     * @param index the index / 위치
     * @return the name / 이름
     */
    private static String deleteError(StyleSheet s, int index) {
        try {
            s.deleteRule(index);
            return "none";
        } catch (DomException e) {
            return e.getName();
        }
    }

    /**
     * DOM-13: a change of a sheet or of one of its rules reaches the receiver of the engine, and nothing does after the sheet left the engine.
     * <p>
     * DOM-13: 시트나 그 규칙의 변경은 엔진의 받는 쪽에 닿고, 시트가 엔진을 떠난 뒤에는 닿지 않는다.
     */
    public void testChangeNotifications() {
        Document doc = org.duckdns.hjow.swingwebpanel.css.MiniHtml.parse("<html><head><style id=s>a { color: red } @media print { b { color: blue } } @keyframes k { from { opacity: 0 } }</style></head><body></body></html>");
        doc.setUrl("http://example.test/");
        CssEngine e = new CssEngine(doc, loader, new MediaEnvironment());
        e.loadStyleSheets();
        final int[] count = new int[1];
        e.setChangeListener(new Runnable() {
            /**
             * Counts the notifications.
             * <p>
             * 통지를 센다.
             */
            public void run() {
                count[0]++;
            }
        });
        StyleSheet s = e.getDocumentStyleSheets().get(0);
        StyleRule a = (StyleRule) s.getRules().get(0);
        a.getStyle().setProperty("color", "blue", "");
        Check.equal(1, count[0], "a declaration changed");
        a.setSelectorText("a.x");
        Check.equal(2, count[0], "the selector changed");
        a.setSelectorText("a!b");
        Check.equal(2, count[0], "an invalid selector changed nothing and told nobody");
        s.setDisabled(true);
        s.setDisabled(true);
        Check.equal(3, count[0], "the disabled flag told once for one change");
        s.insertRule("c { margin: 0 }", 0);
        Check.equal(4, count[0], "an inserted rule");
        GroupRule m = (GroupRule) s.getRules().get(2);
        m.insertRule("d { margin: 0 }", 0);
        Check.equal(5, count[0], "a rule inserted into a group");
        m.getMediaList().setMediaText("screen");
        Check.equal(6, count[0], "the media of a group");
        ((KeyframesRule) s.getRules().get(3)).appendRule("to { opacity: 1 }");
        Check.equal(7, count[0], "a keyframe");
        ((KeyframesRule) s.getRules().get(3)).getKeyframes().get(1).getStyle().setProperty("opacity", "0.5", "");
        Check.equal(8, count[0], "the declarations of a keyframe");
        CssRule gone = s.getRules().get(0);
        s.deleteRule(0);
        Check.equal(9, count[0], "a deleted rule");
        ((StyleRule) gone).getStyle().setProperty("color", "green", "");
        Check.equal(9, count[0], "a deleted rule no longer tells the sheet");
        Check.isTrue(s.isModified(), "the sheet knows that it was changed by a script");
        e.dispose();
        s.setDisabled(false);
        Check.equal(9, count[0], "a sheet of a disposed engine tells nobody");
    }

    /**
     * DOM-13: a sheet is the same object when the document collects its sheets again, also while an <code>@import</code> was still pending, and a style
     * element with new text gets a new sheet.
     * <p>
     * DOM-13: 문서가 시트를 다시 모아도 시트는 같은 객체이고(<code>@import</code> 가 대기 중이었을 때도), 새 텍스트의 style 요소는 새 시트를 받는다.
     */
    public void testSheetIdentityAndPendingImports() {
        Document doc = org.duckdns.hjow.swingwebpanel.css.MiniHtml.parse("<html><head><style id=s>@import url(imp.css);\n.a { color: red }</style></head><body><div class=a id=d></div></body></html>");
        doc.setUrl("http://example.test/dir/page.html");
        CssEngine e = new CssEngine(doc, loader, new MediaEnvironment());
        e.loadStyleSheets();
        StyleSheet s = e.getDocumentStyleSheets().get(0);
        ImportRule imp = (ImportRule) s.getRules().get(0);
        Check.isNull(imp.getStyleSheet(), "the import has not arrived");
        Check.isTrue(s.hasPendingImports(), "the sheet knows that an import is pending");
        s.insertRule(".a { margin-top: 5px }", 2);
        served.put("http://example.test/dir/imp.css", ".a { color: green; padding-top: 2px }");
        e.loadStyleSheets();
        Check.same(s, e.getDocumentStyleSheets().get(0), "the same sheet object after the collection");
        Check.equal(3, s.getRules().size(), "the rule that was inserted is still there");
        Check.notNull(imp.getStyleSheet(), "the pending import was loaded into the rule");
        Check.same(s, imp.getStyleSheet().getParentStyleSheet(), "the imported sheet knows its parent");
        Check.same(imp, imp.getStyleSheet().getOwnerRule(), "and its owner rule");
        Check.equal("http://example.test/dir/imp.css", imp.getStyleSheet().getHref(), "and its location");
        Check.isFalse(s.hasPendingImports(), "nothing is pending any more");
        Element d = doc.getElementById("d");
        Check.equal("rgb(255, 0, 0)", e.getComputedStyle(d).getPropertyValue("color"), "the own rule comes after the import");
        Check.equal("2px", e.getComputedStyle(d).getPropertyValue("padding-top"), "the imported rule applies");
        Check.equal("5px", e.getComputedStyle(d).getPropertyValue("margin-top"), "the inserted rule applies");
        Element style = doc.getElementById("s");
        style.setAttribute("media", "print");
        e.refreshIfNeeded();
        Check.same(s, e.getDocumentStyleSheets().get(0), "an attribute change keeps the sheet");
        Check.equal("print", s.getMediaList().getMediaText(), "the media attribute was applied");
        s.getMediaList().setMediaText("all");
        style.setAttribute("title", "T");
        e.refreshIfNeeded();
        Check.equal("all", s.getMediaList().getMediaText(), "the media that a script set stays when only the title changed");
        Check.equal("T", s.getTitle(), "the title was applied");
        style.setTextContent(".a { color: blue }");
        Check.isTrue(e.refreshIfNeeded(), "the text changed");
        Check.isTrue(s != e.getDocumentStyleSheets().get(0), "new text, new sheet");
        Check.equal(1, e.getDocumentStyleSheets().get(0).getRules().size(), "with the new rules");
        e.dispose();
    }

    /**
     * DOM-13: the adopted sheets of the engine apply after the sheets of the document and are not in the list of the document.
     * <p>
     * DOM-13: 엔진의 채택한 시트는 문서의 시트 다음에 적용되며 문서의 목록에는 없다.
     */
    public void testAdoptedSheetsOfTheEngine() {
        Document doc = org.duckdns.hjow.swingwebpanel.css.MiniHtml.parse("<html><head><style>.a { color: red }</style></head><body><div class=a id=d></div></body></html>");
        doc.setUrl("http://example.test/");
        CssEngine e = new CssEngine(doc, loader, new MediaEnvironment());
        e.loadStyleSheets();
        final int[] count = new int[1];
        e.setChangeListener(new Runnable() {
            /**
             * Counts the notifications.
             * <p>
             * 통지를 센다.
             */
            public void run() {
                count[0]++;
            }
        });
        StyleSheet a = StyleSheet.createConstructed("http://example.test/");
        a.replaceRules(".a { color: blue }");
        Check.isTrue(a.isConstructed(), "constructed");
        Check.isNull(a.getOwner(), "no owner");
        Check.isNull(a.getHref(), "no location");
        List<StyleSheet> list = new ArrayList<StyleSheet>();
        list.add(a);
        e.setAdoptedStyleSheets(list);
        Check.equal(1, count[0], "adopting tells the owner of the engine");
        Check.equal(1, e.getDocumentStyleSheets().size(), "the list of the document has its own sheet only");
        Check.equal(2, e.getStyleSheets().size(), "the engine applies both");
        Check.equal("rgb(0, 0, 255)", e.getComputedStyle(doc.getElementById("d")).getPropertyValue("color"), "the adopted sheet wins");
        a.insertRule(".a { color: green }", 1);
        Check.equal(2, count[0], "a change of an adopted sheet tells the owner");
        Check.equal("rgb(0, 128, 0)", e.getComputedStyle(doc.getElementById("d")).getPropertyValue("color"), "and applies");
        e.setAdoptedStyleSheets(new ArrayList<StyleSheet>());
        Check.equal("rgb(255, 0, 0)", e.getComputedStyle(doc.getElementById("d")).getPropertyValue("color"), "nothing adopted any more");
        int before = count[0];
        a.insertRule(".a { color: orange }", 0);
        Check.equal(before, count[0], "a sheet that is no longer adopted tells nobody");
        e.dispose();
    }

    /**
     * DOM-13: the media list: the empty list, the serialization, comparison and the errors.
     * <p>
     * DOM-13: 미디어 목록: 빈 목록, 직렬화, 비교, 오류.
     */
    public void testMediaList() {
        StyleSheet s = sheet("a { }");
        MediaList m = s.getMediaList();
        Check.same(m, s.getMediaList(), "the same media list object");
        Check.equal("", m.getMediaText(), "no medium");
        Check.equal(0, m.getLength(), "length 0");
        Check.isNull(m.item(0), "no item");
        m.appendMedium("screen and (min-width: 600px)");
        Check.equal("screen and (min-width: 600px)", m.getMediaText(), "appended");
        m.appendMedium("print");
        m.appendMedium("PRINT");
        Check.equal(2, m.getLength(), "no duplicate, whatever the case");
        Check.equal("print", m.item(1), "item");
        m.appendMedium("a, b");
        Check.equal(2, m.getLength(), "two queries are not one medium");
        m.deleteMedium("print");
        Check.equal("screen and (min-width: 600px)", m.getMediaText(), "deleted");
        String name = "none";
        try {
            m.deleteMedium("tv");
        } catch (DomException e) {
            name = e.getName();
        }
        Check.equal("NotFoundError", name, "deleting a medium the list does not have");
        m.setMediaText("not a valid ((");
        Check.equal(1, m.getLength(), "an invalid query is kept as one query");
        m.setMediaText(null);
        Check.equal("", m.getMediaText(), "null is the empty list");
        Check.isTrue(s.getMedia().matches(new MediaEnvironment()), "an empty list matches");
    }

    /**
     * DOM-13: the keyframes of a rule: key texts, finding, appending and deleting.
     * <p>
     * DOM-13: 규칙의 키프레임: 키 텍스트, 찾기, 덧붙이기, 지우기.
     */
    public void testKeyframesModel() {
        StyleSheet s = sheet("@keyframes k { from { opacity: 0 } 50%, 75% { opacity: 0.5 } to { opacity: 1 } }");
        KeyframesRule k = (KeyframesRule) s.getRules().get(0);
        Check.equal("0%", k.getKeyframes().get(0).getKeyText(), "from is 0%");
        Check.equal("50%, 75%", k.getKeyframes().get(1).getKeyText(), "a list");
        Check.same(k.getKeyframes().get(2), k.findRule("100%"), "to is found as 100%");
        Check.same(k.getKeyframes().get(0), k.findRule("from"), "from is found");
        Check.isNull(k.findRule("nonsense"), "an invalid key text finds nothing");
        k.appendRule("25% { opacity: 0.25 }");
        Check.equal(4, k.getCssRules().getLength(), "appended");
        Check.same(k, k.getKeyframes().get(3).getParentRule(), "the keyframe knows its rule");
        Check.same(s, k.getKeyframes().get(3).getParentStyleSheet(), "and its sheet");
        k.deleteRule("25%");
        Check.equal(3, k.getCssRules().getLength(), "deleted");
        String name = "none";
        try {
            k.getKeyframes().get(0).setKeyText("bogus");
        } catch (DomException e) {
            name = e.getName();
        }
        Check.equal("SyntaxError", name, "an invalid key text");
        k.getKeyframes().get(0).setKeyText("10%");
        Check.equal("10%", k.getKeyframes().get(0).getKeyText(), "a new key text");
        k.setName("other");
        Check.equal("@keyframes other {\n  10% { opacity: 0; }\n  50%, 75% { opacity: 0.5; }\n  100% { opacity: 1; }\n}", k.getCssText(), "serialization");
    }
}
