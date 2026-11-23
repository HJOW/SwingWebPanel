package org.duckdns.hjow.swingwebpanel.css;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.css.cssom.CssRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.CssStyleDeclaration;
import org.duckdns.hjow.swingwebpanel.css.cssom.DocumentStyleSheets;
import org.duckdns.hjow.swingwebpanel.css.cssom.FontFaceRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.GroupRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.ImportRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.KeyframesRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.LoadedStyleSheet;
import org.duckdns.hjow.swingwebpanel.css.cssom.NamespaceRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.StyleRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.StyleSheet;
import org.duckdns.hjow.swingwebpanel.css.cssom.StyleSheetDecoder;
import org.duckdns.hjow.swingwebpanel.css.cssom.StyleSheetLoader;
import org.duckdns.hjow.swingwebpanel.css.cssom.SupportsCondition;
import org.duckdns.hjow.swingwebpanel.css.cssom.UaStyleSheet;
import org.duckdns.hjow.swingwebpanel.css.cssom.UnknownRule;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of style sheet construction (plan items P08-02, P08-06): rule types, error recovery, <code>@import</code> with cycles and limits,
 * <code>@namespace</code>, <code>@media</code>, <code>@supports</code>, <code>@font-face</code>, <code>@keyframes</code>, the declaration block as a CSSOM
 * object, byte decoding, the user agent sheet and the collection of the sheets of a document.
 * <p>
 * 스타일 시트 구성 시험(계획 P08-02, P08-06): 규칙 종류, 오류 복구, 순환과 제한이 있는 <code>@import</code>, <code>@namespace</code>, <code>@media</code>,
 * <code>@supports</code>, <code>@font-face</code>, <code>@keyframes</code>, CSSOM 객체로서의 선언 블록, 바이트 디코딩, 사용자 에이전트 시트, 문서의 시트 수집.
 */
public class StyleSheetTest {

    /**
     * An in-memory loader that records the requests. 요청을 기록하는 메모리 로더.
     */
    private static final class MapLoader implements StyleSheetLoader {
        /** The sheets by URL. / URL 별 시트. */
        final Map<String, String> sheets = new HashMap<String, String>();
        /** The requested URLs in order. / 요청된 URL 순서. */
        final List<String> requested = new ArrayList<String>();

        /**
         * Loads from the map. 맵에서 읽는다.
         *
         * @param url the URL / URL
         * @return the sheet / 시트
         * @throws IOException if the URL is unknown / URL 을 모르면
         */
        public LoadedStyleSheet load(String url) throws IOException {
            requested.add(url);
            String t = sheets.get(url);
            if (t == null) {
                throw new IOException("404 " + url);
            }
            return new LoadedStyleSheet(t, url);
        }
    }

    /**
     * Creates the suite. 스위트를 만든다.
     */
    public StyleSheetTest() {
    }

    /**
     * Parses an author sheet. 작성자 시트를 파싱한다.
     *
     * @param css  text / 문자열
     * @param diag collector / 수집기
     * @return the sheet / 시트
     */
    private static StyleSheet sheet(String css, CssDiagnostics diag) {
        return StyleSheet.parse(css, "http://example.com/css/site.css", StyleSheet.ORIGIN_AUTHOR, null, diag);
    }

    /**
     * Rule types and the declaration blocks. 규칙 종류와 선언 블록.
     */
    public void testRules() {
        StyleSheet s = sheet("a, b > c { color: red; margin: 1px !important } /* c */ p { }", null);
        Check.equal(2, s.getRules().size(), "two rules");
        StyleRule r = (StyleRule) s.getRules().get(0);
        Check.equal("a, b > c", r.getSelectorText(), "selector text");
        Check.isTrue(r.getStyle().normal().containsKey(Integer.valueOf(Prop.COLOR)), "normal declaration");
        Check.isTrue(r.getStyle().important().containsKey(Integer.valueOf(Prop.MARGIN_TOP)), "important declaration is kept apart");
        Check.isFalse(r.getStyle().normal().containsKey(Integer.valueOf(Prop.MARGIN_TOP)), "and not duplicated in the normal map");
        Check.isTrue(((StyleRule) s.getRules().get(1)).getStyle().isEmpty(), "an empty rule is kept");
        Check.equal(CssRule.STYLE_RULE, r.getType(), "type");
        Check.equal(StyleSheet.ORIGIN_AUTHOR, s.getOrigin(), "origin");
        Check.equal("a, b > c { color: red; margin: 1px !important; }", r.getCssText(), "serialization");
        Check.equal(2, s.countStyleRules(), "count");
    }

    /**
     * Invalid rules and declarations are dropped alone and reported. 올바르지 않은 규칙과 선언은 따로 버려지고 보고된다.
     */
    public void testErrorRecovery() {
        CssDiagnostics d = new CssDiagnostics();
        StyleSheet s = sheet("a { color: red } b:unknown { color: blue } c { color: notacolor; width: 5px; foo: bar; -webkit-x: 1; perspective: 100px }"
                + " @bogus { x } d { color: green }", d);
        Check.equal(3, s.getRules().size(), "the rule with an invalid selector is dropped");
        StyleRule c = (StyleRule) s.getRules().get(1);
        Check.equal(1, c.getStyle().normal().size(), "only the valid declaration of c is kept");
        Check.isTrue(c.getStyle().normal().containsKey(Integer.valueOf(Prop.WIDTH)), "width");
        Check.isTrue(d.has(CssDiagnostics.INVALID, "b:unknown"), "the dropped rule is reported");
        Check.isTrue(d.has(CssDiagnostics.INVALID, "color"), "the invalid value is reported");
        Check.isTrue(d.has(CssDiagnostics.UNKNOWN, "foo"), "the unknown property is reported");
        Check.isTrue(d.has(CssDiagnostics.UNSUPPORTED, "-webkit-x"), "the vendor property is reported as unsupported");
        Check.isTrue(d.has(CssDiagnostics.UNSUPPORTED, "perspective"), "an unsupported property is reported");
        Check.isTrue(d.has(CssDiagnostics.UNKNOWN, "@bogus"), "the unknown at-rule is reported");
        CssDiagnostics d2 = new CssDiagnostics();
        sheet("a { grid-template-columns: 1fr 1fr }", d2);
        Check.isTrue(d2.has(CssDiagnostics.PARSED_ONLY, "grid-template-columns"), "a parsed-only property is reported as such");
    }

    /**
     * Media and supports groups. media 와 supports 그룹.
     */
    public void testGroups() {
        StyleSheet s = sheet("@media (min-width: 600px) { a { color: red } b { color: blue } } @supports (display: grid) { c { color: red } }"
                + " @supports (foo: bar) { d { color: red } } @layer base { e { color: red } } @layer a, b;", null);
        Check.equal(5, s.getRules().size(), "four groups and the layer statement (it names layers without giving them rules)");
        GroupRule media = (GroupRule) s.getRules().get(0);
        Check.equal(2, media.getRules().size(), "rules of the media group");
        MediaEnvironment wide = new MediaEnvironment(800, 600);
        MediaEnvironment narrow = new MediaEnvironment(400, 600);
        Check.isTrue(media.applies(wide), "applies in a wide viewport");
        Check.isFalse(media.applies(narrow), "does not apply in a narrow viewport");
        Check.isTrue(((GroupRule) s.getRules().get(1)).applies(narrow), "supports with a supported declaration");
        Check.isFalse(((GroupRule) s.getRules().get(2)).applies(narrow), "supports with an unsupported declaration");
        Check.isTrue(((GroupRule) s.getRules().get(3)).applies(narrow), "layers always apply");
        Check.isTrue(((GroupRule) s.getRules().get(4)).isLayerStatement(), "the last rule is the layer statement");
        Check.equal("@layer a, b;", s.getRules().get(4).getCssText(), "serialization of the layer statement");
        Check.equal(5, s.countStyleRules(), "rules inside groups are counted");
        Check.equal("@media (min-width: 600px) {\n  a { color: red; }\n  b { color: blue; }\n}", media.getCssText(), "group serialization");
    }

    /**
     * Imports are loaded in place, relative to the importing sheet. 가져오기는 가져오는 시트 기준으로 그 자리에서 읽힌다.
     */
    public void testImports() {
        MapLoader l = new MapLoader();
        l.sheets.put("http://example.com/css/a.css", "@import 'sub/b.css'; a { color: red }");
        l.sheets.put("http://example.com/css/sub/b.css", "b { color: blue } i { background: url(img.png) }");
        CssDiagnostics d = new CssDiagnostics();
        StyleSheet s = StyleSheet.parse("@charset \"utf-8\"; @import url(a.css) screen and (min-width: 1px); p { }", "http://example.com/css/site.css",
                StyleSheet.ORIGIN_AUTHOR, l, d);
        ImportRule imp = (ImportRule) s.getRules().get(0);
        Check.equal("http://example.com/css/a.css", imp.getResolvedUrl(), "import resolved against the sheet URL");
        Check.notNull(imp.getStyleSheet(), "the sheet was loaded");
        Check.equal("screen and (min-width: 1px)", imp.getStyleSheet().getMedia().toCssText(), "the import media is on the imported sheet");
        ImportRule nested = (ImportRule) imp.getStyleSheet().getRules().get(0);
        Check.equal("http://example.com/css/sub/b.css", nested.getResolvedUrl(), "nested import resolved against the imported sheet");
        Check.equal(3, imp.getStyleSheet().countStyleRules(), "rules of the imported sheets are counted");
        StyleRule i = (StyleRule) nested.getStyleSheet().getRules().get(1);
        org.duckdns.hjow.swingwebpanel.css.values.CssList images = (org.duckdns.hjow.swingwebpanel.css.values.CssList) i.getStyle().normal().get(Integer.valueOf(Prop.BACKGROUND_IMAGE));
        Check.equal("http://example.com/css/sub/img.png", ((org.duckdns.hjow.swingwebpanel.css.values.CssUrl) images.get(0)).getResolvedUrl(),
                "urls are resolved against the sheet that contains them");
        Check.equal(2, l.requested.size(), "two sheets were requested");
        Check.equal("http://example.com/css/a.css", l.requested.get(0), "request order");
    }

    /**
     * Import cycles, limits, misplaced imports and failures. 가져오기의 순환, 제한, 잘못된 위치, 실패.
     */
    public void testImportLimits() {
        MapLoader l = new MapLoader();
        l.sheets.put("http://example.com/a.css", "@import 'b.css'; a { }");
        l.sheets.put("http://example.com/b.css", "@import 'a.css'; b { }");
        CssDiagnostics d = new CssDiagnostics();
        StyleSheet s = StyleSheet.parse("@import 'a.css';", "http://example.com/root.css", StyleSheet.ORIGIN_AUTHOR, l, d);
        ImportRule a = (ImportRule) s.getRules().get(0);
        ImportRule b = (ImportRule) a.getStyleSheet().getRules().get(0);
        Check.notNull(b.getStyleSheet(), "b is loaded");
        Check.isNull(((ImportRule) b.getStyleSheet().getRules().get(0)).getStyleSheet(), "the import of a from b is a cycle and is not loaded");
        Check.isTrue(d.count(CssDiagnostics.INVALID) >= 1, "the cycle is reported");
        Check.equal(2, l.requested.size(), "a cycle does not request anything twice");
        MapLoader deep = new MapLoader();
        // 아홉 겹으로 서로 다른 시트를 가져오게 하여 깊이 제한을 시험한다.
        for (int i = 0; i < 12; i++) {
            deep.sheets.put("http://example.com/d" + i + ".css", "@import 'd" + (i + 1) + ".css'; x { }");
        }
        CssDiagnostics dd = new CssDiagnostics();
        StyleSheet.parse("@import 'd0.css';", "http://example.com/root.css", StyleSheet.ORIGIN_AUTHOR, deep, dd);
        Check.isTrue(deep.requested.size() <= StyleSheet.MAX_IMPORT_DEPTH, "the depth limit stops the chain: " + deep.requested.size());
        Check.isTrue(dd.count(CssDiagnostics.INVALID) >= 1, "the limit is reported");
        MapLoader m = new MapLoader();
        m.sheets.put("http://example.com/x.css", "z { }");
        CssDiagnostics md = new CssDiagnostics();
        StyleSheet late = StyleSheet.parse("a { } @import 'x.css';", "http://example.com/root.css", StyleSheet.ORIGIN_AUTHOR, m, md);
        Check.equal(1, late.getRules().size(), "an @import after a rule is dropped");
        Check.equal(0, m.requested.size(), "and not loaded");
        StyleSheet missing = StyleSheet.parse("@import 'gone.css'; a { }", "http://example.com/root.css", StyleSheet.ORIGIN_AUTHOR, m, md);
        Check.isNull(((ImportRule) missing.getRules().get(0)).getStyleSheet(), "a failed load leaves the rule without a sheet");
        Check.equal(2, missing.getRules().size(), "and the other rules are kept");
        StyleSheet noLoader = StyleSheet.parse("@import 'x.css'; a { }", "http://example.com/root.css", StyleSheet.ORIGIN_AUTHOR, null, md);
        Check.isNull(((ImportRule) noLoader.getRules().get(0)).getStyleSheet(), "without a loader imports are skipped");
        StyleSheet sup = StyleSheet.parse("@import 'x.css' supports(foo: bar);", "http://example.com/root.css", StyleSheet.ORIGIN_AUTHOR, m, md);
        Check.equal("not all", ((ImportRule) sup.getRules().get(0)).getMedia().toCssText(), "an unsupported supports() condition disables the import");
    }

    /**
     * Namespace declarations and prefixes. 네임스페이스 선언과 접두사.
     */
    public void testNamespaces() {
        CssDiagnostics d = new CssDiagnostics();
        StyleSheet s = sheet("@namespace svg url(http://www.w3.org/2000/svg); @namespace url(http://www.w3.org/1999/xhtml);"
                + " svg|rect { fill: red } a { color: red } bad|x { color: red }", d);
        Check.equal(CssRule.NAMESPACE_RULE, s.getRules().get(0).getType(), "namespace rule type");
        Check.equal("svg", ((NamespaceRule) s.getRules().get(0)).getPrefix(), "prefix");
        Check.equal("http://www.w3.org/2000/svg", ((NamespaceRule) s.getRules().get(0)).getNamespaceUri(), "uri");
        Check.equal("", ((NamespaceRule) s.getRules().get(1)).getPrefix(), "default namespace");
        Check.equal(4, s.getRules().size(), "bad|x with an undeclared prefix is dropped");
        Check.equal("svg|rect", ((StyleRule) s.getRules().get(2)).getSelectorText(), "prefixed selector");
        StyleSheet late = sheet("a { } @namespace svg url(http://www.w3.org/2000/svg);", d);
        Check.equal(1, late.getRules().size(), "@namespace after a rule is dropped");
        StyleSheet afterNs = sheet("@namespace svg url(http://www.w3.org/2000/svg); @import 'x.css';", d);
        Check.equal(1, afterNs.getRules().size(), "@import after @namespace is dropped");
    }

    /**
     * Font faces. 웹 글꼴.
     */
    public void testFontFace() {
        CssDiagnostics d = new CssDiagnostics();
        StyleSheet s = sheet("@font-face { font-family: 'My Font'; src: local('Arial'), url(fonts/a.woff2) format('woff2'), url(\"b.ttf\"); font-weight: 700 }"
                + " @font-face { font-family: x } @font-face { src: url(c.ttf) }", d);
        Check.equal(1, s.getRules().size(), "faces without a family or a source are dropped");
        FontFaceRule f = (FontFaceRule) s.getRules().get(0);
        Check.equal("My Font", f.getFamily(), "family");
        Check.equal(3, f.getSources().size(), "three sources");
        Check.equal("Arial", f.getSources().get(0).getLocal(), "local source");
        Check.equal("http://example.com/css/fonts/a.woff2", f.getSources().get(1).getResolvedUrl(), "relative url resolved against the sheet");
        Check.equal("woff2", f.getSources().get(1).getFormat(), "format hint");
        Check.equal("http://example.com/css/b.ttf", f.getSources().get(2).getResolvedUrl(), "quoted url");
        Check.equal("700", f.getDescriptor("font-weight"), "descriptor");
        Check.isNull(f.getDescriptor("font-style"), "missing descriptor");
        Check.equal(CssRule.FONT_FACE_RULE, f.getType(), "type");
        Check.isTrue(d.count(CssDiagnostics.INVALID) >= 2, "dropped faces are reported");
    }

    /**
     * Keyframe rules and offsets. 키프레임 규칙과 오프셋.
     */
    public void testKeyframes() {
        CssDiagnostics d = new CssDiagnostics();
        StyleSheet s = sheet("@keyframes spin { from { transform: rotate(0deg) } 50%, 75% { opacity: 0.5 !important; width: 1px } to { transform: rotate(360deg) }"
                + " 150% { width: 2px } foo { width: 3px } }", d);
        KeyframesRule k = (KeyframesRule) s.getRules().get(0);
        Check.equal("spin", k.getName(), "name");
        Check.equal(3, k.getKeyframes().size(), "invalid keyframe selectors are dropped");
        Check.equal(0.0, k.getKeyframes().get(0).getOffsets().get(0).doubleValue(), "from");
        Check.equal(2, k.getKeyframes().get(1).getOffsets().size(), "two offsets");
        Check.equal(0.75, k.getKeyframes().get(1).getOffsets().get(1).doubleValue(), "percentage");
        Check.equal(1.0, k.getKeyframes().get(2).getOffsets().get(0).doubleValue(), "to");
        Check.isFalse(k.getKeyframes().get(1).getStyle().normal().containsKey(Integer.valueOf(Prop.OPACITY)), "!important is ignored in keyframes");
        Check.isTrue(k.getKeyframes().get(1).getStyle().normal().containsKey(Integer.valueOf(Prop.WIDTH)), "other declarations are kept");
        StyleSheet unnamed = sheet("@keyframes { from { width: 1px } }", d);
        Check.equal(0, unnamed.getRules().size(), "a name is required");
        StyleSheet prefixed = sheet("@-webkit-keyframes x { to { width: 1px } }", d);
        Check.equal(1, prefixed.getRules().size(), "prefixed keyframes");
    }

    /**
     * Other at-rules. 그 밖의 at-규칙.
     */
    public void testOtherAtRules() {
        CssDiagnostics d = new CssDiagnostics();
        StyleSheet s = sheet("@charset 'utf-8'; @page { margin: 1cm } @property --x { syntax: '<length>'; inherits: false; initial-value: 0px } @unknown-rule foo; a { }", d);
        Check.equal(3, s.getRules().size(), "charset and the unknown rule are skipped; page and a valid property are kept, and an invalid registration is dropped");
        Check.isTrue(s.getRules().get(0) instanceof org.duckdns.hjow.swingwebpanel.css.cssom.PageRule, "page is kept as a page rule");
        Check.contains(s.getRules().get(0).getCssText(), "@page { margin: 1cm; }", "the serialization");
        Check.isTrue(d.has(CssDiagnostics.UNSUPPORTED, "@page"), "the page boxes are reported as not laid out");
        Check.isTrue(d.has(CssDiagnostics.UNKNOWN, "@unknown-rule"), "unknown reported");
    }

    /**
     * The declaration block as a CSSOM object. CSSOM 객체로서의 선언 블록.
     */
    public void testDeclarationBlock() {
        CssStyleDeclaration d = CssStyleDeclaration.parse("color: red; margin: 1px 2px; padding-top: 3px !important; --x: 5", null, null);
        Check.equal("red", d.getPropertyValue("color"), "longhand");
        Check.equal("1px 2px", d.getPropertyValue("margin"), "shorthand built from longhands");
        Check.equal("", d.getPropertyValue("padding"), "an incomplete shorthand is empty");
        Check.equal("3px", d.getPropertyValue("padding-top"), "important longhand");
        Check.equal("important", d.getPropertyPriority("padding-top"), "priority");
        Check.equal("", d.getPropertyPriority("color"), "no priority");
        Check.equal("5", d.getPropertyValue("--x"), "custom property");
        Check.equal(7, d.getLength(), "length counts longhands and custom properties");
        Check.equal("color", d.item(0), "item");
        Check.equal("", d.item(99), "item out of range");
        final int[] changes = {0};
        d.setChangeListener(new Runnable() {
            public void run() {
                changes[0]++;
            }
        });
        Check.isTrue(d.setProperty("color", "blue", ""), "setProperty");
        Check.equal("blue", d.getPropertyValue("color"), "changed value");
        Check.isFalse(d.setProperty("color", "notacolor", ""), "an invalid value changes nothing");
        Check.equal("blue", d.getPropertyValue("color"), "still blue");
        Check.isTrue(d.setProperty("margin", "7px", "important"), "setProperty with a shorthand and a priority");
        Check.equal("7px", d.getPropertyValue("margin-left"), "longhand of the shorthand");
        Check.equal("important", d.getPropertyPriority("margin"), "shorthand priority");
        Check.equal("blue", d.removeProperty("color"), "removeProperty returns the old value");
        Check.equal("", d.getPropertyValue("color"), "removed");
        Check.equal("7px", d.removeProperty("margin"), "removing a shorthand removes its longhands");
        Check.isTrue(changes[0] >= 3, "the change listener was called");
        d.setCssText("width: 10px; height: 20px");
        Check.equal("width: 10px; height: 20px;", d.getCssText(), "cssText");
        d.setCssText("margin: 1px; padding: 2px 3px");
        Check.equal("margin: 1px; padding: 2px 3px;", d.getCssText(), "shorthands are collapsed in cssText");
        d.setProperty("width", "", "");
        Check.equal(8, d.getLength(), "removing a property that is not set changes nothing");
    }

    /**
     * Supports conditions. supports 조건.
     */
    public void testSupports() {
        Check.isTrue(SupportsCondition.evaluate("(display: flex)"), "supported declaration");
        Check.isFalse(SupportsCondition.evaluate("(display: foo)"), "invalid value");
        Check.isFalse(SupportsCondition.evaluate("(foo: bar)"), "unknown property");
        Check.isFalse(SupportsCondition.evaluate("(perspective: 1px)"), "unsupported property");
        Check.isTrue(SupportsCondition.evaluate("(display: flex) and (color: red)"), "and");
        Check.isFalse(SupportsCondition.evaluate("(display: flex) and (foo: bar)"), "and with a false operand");
        Check.isTrue(SupportsCondition.evaluate("(foo: bar) or (color: red)"), "or");
        Check.isTrue(SupportsCondition.evaluate("not (foo: bar)"), "not");
        Check.isFalse(SupportsCondition.evaluate("not (color: red)"), "not of a true condition");
        Check.isTrue(SupportsCondition.evaluate("((foo: bar) or (color: red)) and (display: block)"), "nested");
        Check.isFalse(SupportsCondition.evaluate("(color: red) and (color: blue) or (color: green)"), "mixing and with or is invalid");
        Check.isTrue(SupportsCondition.evaluate("selector(a > b)"), "valid selector");
        Check.isFalse(SupportsCondition.evaluate("selector(a >)"), "invalid selector");
        Check.isFalse(SupportsCondition.evaluate("font-tech(color-colrv1)"), "unknown function");
        Check.isTrue(SupportsCondition.evaluate("(--custom: anything)"), "custom properties are always supported");
        Check.isFalse(SupportsCondition.evaluate(""), "empty");
        Check.isTrue(SupportsCondition.supports("color", "red"), "CSS.supports(property, value)");
        Check.isFalse(SupportsCondition.supports("color", "nothing"), "invalid value");
        Check.isTrue(SupportsCondition.evaluate("(display:grid)"), "no spaces");
    }

    /**
     * Byte decoding of style sheets. 스타일 시트의 바이트 디코딩.
     */
    public void testDecoder() throws Exception {
        String wide = String.valueOf((char) 0xC6B0);
        String acute = String.valueOf((char) 0xE9);
        String replacement = String.valueOf((char) 0xFFFD);
        byte[] utf8 = ("a { content: \"" + wide + "\" }").getBytes("UTF-8");
        Check.equal("a { content: \"" + wide + "\" }", StyleSheetDecoder.decode(utf8, null, null), "UTF-8 by default");
        byte[] bom = new byte[utf8.length + 3];
        bom[0] = (byte) 0xEF;
        bom[1] = (byte) 0xBB;
        bom[2] = (byte) 0xBF;
        System.arraycopy(utf8, 0, bom, 3, utf8.length);
        Check.equal("a { content: \"" + wide + "\" }", StyleSheetDecoder.decode(bom, "iso-8859-1", null), "a BOM wins over the header and is removed");
        String atText = "@charset \"iso-8859-1\"; a { content: \"" + acute + "\" }";
        Check.equal(atText, StyleSheetDecoder.decode(atText.getBytes("ISO-8859-1"), null, null), "@charset");
        Check.equal(acute, StyleSheetDecoder.decode(new byte[] {(byte) 0xE9}, "iso-8859-1", "utf-8"), "the header wins over the referrer");
        Check.equal(acute, StyleSheetDecoder.decode(new byte[] {(byte) 0xE9}, null, "iso-8859-1"), "the referrer is used when nothing else is given");
        Check.equal(replacement, StyleSheetDecoder.decode(new byte[] {(byte) 0xE9}, "nonexistent-charset", null),
                "an unknown header charset is ignored and invalid UTF-8 is replaced");
        byte[] u16 = "@charset \"utf-16\"; a { }".getBytes("ISO-8859-1");
        Check.equal("@charset \"utf-16\"; a { }", StyleSheetDecoder.decode(u16, null, null), "a UTF-16 @charset means UTF-8");
        byte[] be = new byte[] {(byte) 0xFE, (byte) 0xFF, 0, 'a'};
        Check.equal("a", StyleSheetDecoder.decode(be, null, null), "UTF-16BE BOM");
    }

    /**
     * The user agent sheet has no invalid declaration. 사용자 에이전트 시트에는 올바르지 않은 선언이 없다.
     */
    public void testUserAgentSheet() {
        CssDiagnostics d = new CssDiagnostics();
        StyleSheet s = StyleSheet.parse(UaStyleSheet.standardsText(), null, StyleSheet.ORIGIN_USER_AGENT, null, d);
        Check.equal(0, d.count(CssDiagnostics.INVALID), "no invalid declarations or rules: " + d.getEntries());
        Check.equal(0, d.count(CssDiagnostics.UNKNOWN), "no unknown properties: " + d.getEntries());
        Check.isTrue(s.countStyleRules() > 60, "the sheet has more than 60 style rules");
        Check.same(UaStyleSheet.standards(), UaStyleSheet.standards(), "the parsed sheet is shared");
        Check.equal(StyleSheet.ORIGIN_USER_AGENT, UaStyleSheet.standards().getOrigin(), "origin");
        Check.isTrue(UaStyleSheet.quirks().countStyleRules() > 0, "quirks additions exist");
    }

    /**
     * Collecting the sheets of a document. 문서의 시트 수집.
     */
    public void testDocumentSheets() {
        Document doc = MiniHtml.parse("<html><head><style>a { color: red }</style><style media=\"print\" title=\"P\">b { }</style>"
                + "<style type=\"text/less\">c { }</style><link rel=\"stylesheet\" href=\"/x.css\" media=\"screen\">"
                + "<link rel=\"alternate stylesheet\" href=\"/alt.css\"><link rel=\"icon\" href=\"/i.png\">"
                + "<link rel=\"stylesheet\" href=\"/gone.css\"><style disabled>d { }</style></head><body><style>e { }</style></body></html>");
        doc.setUrl("http://example.com/dir/page.html");
        MapLoader l = new MapLoader();
        l.sheets.put("http://example.com/x.css", "x { color: blue }");
        CssDiagnostics d = new CssDiagnostics();
        List<StyleSheet> sheets = DocumentStyleSheets.collect(doc, l, d);
        Check.equal(5, sheets.size(), "style, style, link, disabled style, style in the body");
        Check.equal("print", sheets.get(1).getMedia().toCssText(), "media of a style element");
        Check.equal("P", sheets.get(1).getTitle(), "title");
        Check.equal("screen", sheets.get(2).getMedia().toCssText(), "media of a link element");
        Check.equal("http://example.com/x.css", sheets.get(2).getUrl(), "url of a linked sheet");
        Check.isTrue(sheets.get(3).isDisabled(), "disabled attribute");
        Check.isFalse(sheets.get(0).isDisabled(), "enabled by default");
        Check.equal(StyleSheet.ORIGIN_AUTHOR, sheets.get(0).getOrigin(), "author origin");
        Check.notNull(sheets.get(0).getOwner(), "owner element");
        Check.equal(2, l.requested.size(), "both links were requested, including the one that fails (gone.css)");
        Check.isTrue(d.count(CssDiagnostics.INVALID) >= 1, "the failing link is reported");
        List<StyleSheet> noLoader = DocumentStyleSheets.collect(doc, null, null);
        Check.equal(4, noLoader.size(), "without a loader linked sheets are skipped");
    }
}
