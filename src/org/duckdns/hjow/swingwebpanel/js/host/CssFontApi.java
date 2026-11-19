package org.duckdns.hjow.swingwebpanel.js.host;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.css.cssom.CssRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.FontFaceRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.GroupRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.StyleSheet;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssEscape;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.EventTarget;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsArray;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.js.dom.CssomMembers;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.dom.HostObject;

/**
 * <code>document.fonts</code> (<code>FontFaceSet</code>) and <code>FontFace</code> of the CSS Font Loading Module Level 3
 * (<code>https://www.w3.org/TR/css-font-loading-3/</code>). The set lists the <code>@font-face</code> rules of the style sheets of the page (as
 * <code>FontFace</code> objects that are connected to their rule) followed by the faces that a script added; <code>document.fonts.ready</code>,
 * <code>check()</code>, <code>load()</code>, <code>status</code>, <code>size</code> and the set methods work on it. A face that a script adds
 * (<code>new FontFace("Name", "url(font.woff2)")</code>, <code>document.fonts.add(face)</code>) becomes a <code>@font-face</code> rule of a style sheet that
 * the page adopts, so the web font loader of the engine (the same one that loads the fonts of style sheets: the requests of the page, the formats
 * TrueType and WOFF) loads it and the text is laid out again when it arrives. <b>Limits:</b> the loading is done by the layout when text needs the
 * face, so <code>load()</code> does not wait for the bytes: it sets the status to <code>loaded</code> at once (a face with a source that cannot be parsed
 * is <code>error</code> with a rejected <code>loaded</code> promise); faces from style sheets count as loaded; binary sources
 * (<code>ArrayBuffer</code>) are accepted but not applied; the events <code>loading</code>, <code>loadingdone</code>, <code>loadingerror</code> are
 * never fired; <code>unicodeRange</code>, <code>variant</code> and <code>featureSettings</code> are stored and written to the rule but not checked.
 * <p>
 * CSS Font Loading Module Level 3(<code>https://www.w3.org/TR/css-font-loading-3/</code>)의 <code>document.fonts</code>(<code>FontFaceSet</code>)와
 * <code>FontFace</code>. 집합은 페이지 스타일 시트의 <code>@font-face</code> 규칙(규칙에 연결된 <code>FontFace</code> 객체)에 이어 스크립트가 더한 글꼴 면을 나열하며,
 * <code>document.fonts.ready</code>, <code>check()</code>, <code>load()</code>, <code>status</code>, <code>size</code> 와 집합 메서드가 그 위에서 동작한다. 스크립트가 더한 글꼴 면
 * (<code>new FontFace("Name", "url(font.woff2)")</code>, <code>document.fonts.add(face)</code>)은 페이지가 채택하는 스타일 시트의 <code>@font-face</code> 규칙이 되므로 엔진의 웹
 * 글꼴 로더(스타일 시트의 글꼴을 읽는 것과 같은 것: 페이지의 요청, TrueType 과 WOFF 형식)가 그것을 읽고 도착하면 텍스트를 다시 배치한다. <b>한계:</b> 읽기는 텍스트가 글꼴 면을
 * 필요로 할 때 레이아웃이 하므로 <code>load()</code> 는 바이트를 기다리지 않고 상태를 곧바로 <code>loaded</code> 로 바꾼다(해석할 수 없는 출처를 가진 글꼴 면은
 * <code>error</code> 이며 <code>loaded</code> 프라미스가 reject 된다). 스타일 시트의 글꼴 면은 읽힌 것으로 센다. 이진 출처(<code>ArrayBuffer</code>)는 받아들이지만 적용하지
 * 않는다. <code>loading</code>, <code>loadingdone</code>, <code>loadingerror</code> 이벤트는 발생시키지 않는다. <code>unicodeRange</code>, <code>variant</code>,
 * <code>featureSettings</code> 는 저장하고 규칙에 쓰지만 검사하지 않는다.
 */
final class CssFontApi {

    /**
     * The target of a <code>FontFace</code> object: the descriptors, the status and the promise <code>loaded</code>.
     * <p>
     * <code>FontFace</code> 객체의 대상: 디스크립터, 상태, <code>loaded</code> 프라미스.
     */
    public static final class FaceObject {
        /** The family name. / 글꼴 계열 이름. */
        private String family;
        /** The <code>font-style</code> descriptor. / <code>font-style</code> 디스크립터. */
        private String style = "normal";
        /** The <code>font-weight</code> descriptor. / <code>font-weight</code> 디스크립터. */
        private String weight = "normal";
        /** The <code>font-stretch</code> descriptor. / <code>font-stretch</code> 디스크립터. */
        private String stretch = "normal";
        /** The <code>unicode-range</code> descriptor. / <code>unicode-range</code> 디스크립터. */
        private String unicodeRange = "U+0-10FFFF";
        /** The <code>font-variant</code> descriptor. / <code>font-variant</code> 디스크립터. */
        private String variant = "normal";
        /** The <code>font-feature-settings</code> descriptor. / <code>font-feature-settings</code> 디스크립터. */
        private String featureSettings = "normal";
        /** The <code>font-display</code> descriptor. / <code>font-display</code> 디스크립터. */
        private String display = "auto";
        /** The status: <code>unloaded</code>, <code>loading</code>, <code>loaded</code> or <code>error</code>. / 상태. */
        private String status = "unloaded";
        /** The source text (<code>src</code> descriptor), or null for binary data. / 출처 텍스트(<code>src</code> 디스크립터). 이진 데이터이면 null. */
        private String source;
        /** True for a face that stands for a <code>@font-face</code> rule of a style sheet, false for a face that a script made. / 스타일 시트의 <code>@font-face</code> 규칙을 나타내는 글꼴 면이면 true, 스크립트가 만든 것이면 false. */
        private boolean fromRule;
        /** The promise <code>loaded</code>, created with the object. / <code>loaded</code> 프라미스. 객체와 함께 만든다. */
        private JsObject loadedPromise;
        /** The resolve function of the promise. / 프라미스의 resolve 함수. */
        private JsValue resolveFn;
        /** The reject function of the promise. / 프라미스의 reject 함수. */
        private JsValue rejectFn;

        /**
         * Creates a face.
         * <p>
         * 글꼴 면을 만든다.
         *
         * @param family the family name / 글꼴 계열 이름
         */
        FaceObject(String family) {
            this.family = family;
        }

        /**
         * Returns the family name (<code>family</code>).
         * <p>
         * 글꼴 계열 이름을 반환한다(<code>family</code>).
         *
         * @return the name / 이름
         */
        public String getFamily() {
            return family;
        }

        /**
         * Sets the family name.
         * <p>
         * 글꼴 계열 이름을 설정한다.
         *
         * @param v the name / 이름
         */
        public void setFamily(String v) {
            this.family = nonEmpty(v, "family");
        }

        /**
         * Returns the <code>style</code> descriptor.
         * <p>
         * <code>style</code> 디스크립터를 반환한다.
         *
         * @return the text / 텍스트
         */
        public String getStyle() {
            return style;
        }

        /**
         * Sets the <code>style</code> descriptor.
         * <p>
         * <code>style</code> 디스크립터를 설정한다.
         *
         * @param v the text / 텍스트
         */
        public void setStyle(String v) {
            this.style = nonEmpty(v, "style");
        }

        /**
         * Returns the <code>weight</code> descriptor.
         * <p>
         * <code>weight</code> 디스크립터를 반환한다.
         *
         * @return the text / 텍스트
         */
        public String getWeight() {
            return weight;
        }

        /**
         * Sets the <code>weight</code> descriptor.
         * <p>
         * <code>weight</code> 디스크립터를 설정한다.
         *
         * @param v the text / 텍스트
         */
        public void setWeight(String v) {
            this.weight = nonEmpty(v, "weight");
        }

        /**
         * Returns the <code>stretch</code> descriptor.
         * <p>
         * <code>stretch</code> 디스크립터를 반환한다.
         *
         * @return the text / 텍스트
         */
        public String getStretch() {
            return stretch;
        }

        /**
         * Sets the <code>stretch</code> descriptor.
         * <p>
         * <code>stretch</code> 디스크립터를 설정한다.
         *
         * @param v the text / 텍스트
         */
        public void setStretch(String v) {
            this.stretch = nonEmpty(v, "stretch");
        }

        /**
         * Returns the <code>unicodeRange</code> descriptor.
         * <p>
         * <code>unicodeRange</code> 디스크립터를 반환한다.
         *
         * @return the text / 텍스트
         */
        public String getUnicodeRange() {
            return unicodeRange;
        }

        /**
         * Sets the <code>unicodeRange</code> descriptor.
         * <p>
         * <code>unicodeRange</code> 디스크립터를 설정한다.
         *
         * @param v the text / 텍스트
         */
        public void setUnicodeRange(String v) {
            this.unicodeRange = nonEmpty(v, "unicodeRange");
        }

        /**
         * Returns the <code>variant</code> descriptor.
         * <p>
         * <code>variant</code> 디스크립터를 반환한다.
         *
         * @return the text / 텍스트
         */
        public String getVariant() {
            return variant;
        }

        /**
         * Sets the <code>variant</code> descriptor.
         * <p>
         * <code>variant</code> 디스크립터를 설정한다.
         *
         * @param v the text / 텍스트
         */
        public void setVariant(String v) {
            this.variant = nonEmpty(v, "variant");
        }

        /**
         * Returns the <code>featureSettings</code> descriptor.
         * <p>
         * <code>featureSettings</code> 디스크립터를 반환한다.
         *
         * @return the text / 텍스트
         */
        public String getFeatureSettings() {
            return featureSettings;
        }

        /**
         * Sets the <code>featureSettings</code> descriptor.
         * <p>
         * <code>featureSettings</code> 디스크립터를 설정한다.
         *
         * @param v the text / 텍스트
         */
        public void setFeatureSettings(String v) {
            this.featureSettings = nonEmpty(v, "featureSettings");
        }

        /**
         * Returns the <code>display</code> descriptor.
         * <p>
         * <code>display</code> 디스크립터를 반환한다.
         *
         * @return the text / 텍스트
         */
        public String getDisplay() {
            return display;
        }

        /**
         * Sets the <code>display</code> descriptor.
         * <p>
         * <code>display</code> 디스크립터를 설정한다.
         *
         * @param v the text / 텍스트
         */
        public void setDisplay(String v) {
            this.display = nonEmpty(v, "display");
        }

        /**
         * Returns the status of the face.
         * <p>
         * 글꼴 면의 상태를 반환한다.
         *
         * @return <code>unloaded</code>, <code>loading</code>, <code>loaded</code> or <code>error</code> / 상태
         */
        public String getStatus() {
            return status;
        }

        /**
         * Checks a descriptor text: the empty text is a syntax error (the CSS parse of a descriptor is not repeated here).
         * <p>
         * 디스크립터 텍스트를 검사한다: 빈 텍스트는 구문 오류이다(디스크립터의 CSS 파싱은 여기서 되풀이하지 않는다).
         *
         * @param v    the text / 텍스트
         * @param name the descriptor name / 디스크립터 이름
         * @return the trimmed text / 다듬은 텍스트
         * @throws DomException <code>SyntaxError</code> / <code>SyntaxError</code>
         */
        private static String nonEmpty(String v, String name) {
            String t = v == null ? "" : v.trim();
            if (t.length() == 0) {
                throw DomException.syntax("Failed to set '" + name + "' on 'FontFace': the value is not valid.");
            }
            return t;
        }
    }

    /**
     * The target of <code>document.fonts</code>: the faces that scripts added, and the access to the faces of the style sheets.
     * <p>
     * <code>document.fonts</code> 의 대상: 스크립트가 더한 글꼴 면과 스타일 시트의 글꼴 면에 대한 접근.
     */
    public static final class FontSet extends EventTarget {
        /** The installer that knows the page. / 페이지를 아는 설치기. */
        private final CssFontApi api;

        /**
         * Creates the set.
         * <p>
         * 집합을 만든다.
         *
         * @param api the installer / 설치기
         */
        FontSet(CssFontApi api) {
            this.api = api;
        }

        /**
         * Returns the number of faces (<code>size</code>).
         * <p>
         * 글꼴 면 수를 반환한다(<code>size</code>).
         *
         * @return the count / 개수
         */
        public int getSize() {
            return api.allFaces().size();
        }

        /**
         * Returns <code>loaded</code>: the set has nothing that is loading (see the limits of {@link CssFontApi}).
         * <p>
         * <code>loaded</code> 를 반환한다: 집합에는 읽는 중인 것이 없다({@link CssFontApi} 의 한계 참고).
         *
         * @return the status / 상태
         */
        public String getStatus() {
            return "loaded";
        }

        /**
         * Adds a face (<code>add</code>) and returns the set.
         * <p>
         * 글꼴 면을 더하고(<code>add</code>) 집합을 반환한다.
         *
         * @param face a <code>FontFace</code> / <code>FontFace</code>
         * @return this set / 이 집합
         */
        public FontSet add(Object face) {
            api.add(api.face(face, "add"));
            return this;
        }

        /**
         * Removes a face (<code>delete</code>).
         * <p>
         * 글꼴 면을 뺀다(<code>delete</code>).
         *
         * @param face a <code>FontFace</code> / <code>FontFace</code>
         * @return true if the set had it / 집합에 있었으면 true
         */
        public boolean delete(Object face) {
            return api.remove(api.face(face, "delete"));
        }

        /**
         * Tells whether the set has a face (<code>has</code>).
         * <p>
         * 집합에 글꼴 면이 있는지 알려 준다(<code>has</code>).
         *
         * @param face a <code>FontFace</code> / <code>FontFace</code>
         * @return true if the set has it / 있으면 true
         */
        public boolean has(Object face) {
            return api.allFaces().contains(api.face(face, "has"));
        }

        /**
         * Removes the faces that scripts added (<code>clear</code>); the faces of the style sheets stay.
         * <p>
         * 스크립트가 더한 글꼴 면을 모두 뺀다(<code>clear</code>). 스타일 시트의 글꼴 면은 남는다.
         */
        public void clear() {
            api.clearAdded();
        }

        /**
         * Tells whether the faces that match a font are all loaded (<code>check</code>).
         * <p>
         * 글꼴과 일치하는 글꼴 면이 모두 읽혔는지 알려 준다(<code>check</code>).
         *
         * @param font the font shorthand, for example <code>16px Foo</code> / 글꼴 단축 표기. 예: <code>16px Foo</code>
         * @param text the text that must be covered / 덮여야 하는 텍스트
         * @return true if nothing that matches is unloaded / 일치하는 것 중 읽히지 않은 것이 없으면 true
         * @throws DomException <code>SyntaxError</code> if the font cannot be parsed / 글꼴을 파싱할 수 없으면 <code>SyntaxError</code>
         */
        public boolean check(String font, String text) {
            List<FaceObject> matching = api.matching(font);
            // 일치하는 글꼴 면 중 읽히지 않은 것이 있으면 false 이다.
            for (int i = 0; i < matching.size(); i++) {
                if (!"loaded".equals(matching.get(i).status)) {
                    return false;
                }
            }
            return true;
        }
    }

    /** The page. / 페이지. */
    private final PageScripts page;

    /** The bindings of the page that apply adopted sheets. / 채택한 시트를 적용하는 페이지의 바인딩. */
    private final CssomBindings bindings;

    /** The set that <code>document.fonts</code> returns. / <code>document.fonts</code> 가 반환하는 집합. */
    private final FontSet set = new FontSet(this);

    /** The faces that scripts added, in order. / 스크립트가 더한 글꼴 면(순서대로). */
    private final List<FaceObject> added = new ArrayList<FaceObject>();

    /** The faces that stand for the <code>@font-face</code> rules of the style sheets. / 스타일 시트의 <code>@font-face</code> 규칙을 나타내는 글꼴 면. */
    private final Map<FontFaceRule, FaceObject> ruleFaces = new WeakHashMap<FontFaceRule, FaceObject>();

    /** The style sheet that holds the rules of the faces that scripts added (adopted by the page while it has rules), or null. / 스크립트가 더한 글꼴 면의 규칙을 담은 스타일 시트(규칙이 있는 동안 페이지가 채택). 없으면 null. */
    private StyleSheet faceSheet;

    /** The promise <code>document.fonts.ready</code>, resolved. / <code>document.fonts.ready</code> 프라미스. 이행되어 있다. */
    private JsObject readyPromise;

    /**
     * Creates the installer of a page.
     * <p>
     * 페이지의 설치기를 만든다.
     *
     * @param page     the page / 페이지
     * @param bindings the CSSOM bindings of the page / 페이지의 CSSOM 바인딩
     */
    CssFontApi(PageScripts page, CssomBindings bindings) {
        this.page = page;
        this.bindings = bindings;
    }

    /**
     * Returns the style sheet that carries the faces that scripts added, or null while there is none; the page adopts it.
     * <p>
     * 스크립트가 더한 글꼴 면을 싣는 스타일 시트를 반환한다. 없는 동안은 null. 페이지가 이것을 채택한다.
     *
     * @return the sheet or null / 시트 또는 null
     */
    StyleSheet faceSheet() {
        return faceSheet != null && !faceSheet.getRules().isEmpty() ? faceSheet : null;
    }

    /**
     * Collects the <code>@font-face</code> rules of the style sheets of the page (also the ones in groups that are not conditional on this engine).
     * <p>
     * 페이지의 스타일 시트의 <code>@font-face</code> 규칙을 모은다(조건부가 아닌 그룹 안의 것도).
     *
     * @return the rules / 규칙
     */
    private List<FontFaceRule> sheetRules() {
        List<FontFaceRule> out = new ArrayList<FontFaceRule>();
        CssEngine e = page.getEnvironment().getCssEngine();
        List<StyleSheet> sheets = e == null ? null : e.getStyleSheets();
        // 시트마다 규칙을 훑는다(스크립트가 더한 글꼴 면의 시트는 add 한 글꼴 면이 따로 보여 주므로 건너뛴다).
        for (int i = 0; sheets != null && i < sheets.size(); i++) {
            if (sheets.get(i) != faceSheet) {
                collect(sheets.get(i).getRules(), out);
            }
        }
        return out;
    }

    /**
     * Adds the font face rules of a rule list to a list.
     * <p>
     * 규칙 목록의 글꼴 규칙을 목록에 더한다.
     *
     * @param rules the rules / 규칙
     * @param out   receives the font face rules / 글꼴 규칙을 받는다
     */
    private static void collect(List<CssRule> rules, List<FontFaceRule> out) {
        // 규칙마다 글꼴 규칙이면 더하고 그룹이면 그 안을 훑는다.
        for (int i = 0; i < rules.size(); i++) {
            if (rules.get(i) instanceof FontFaceRule) {
                out.add((FontFaceRule) rules.get(i));
            } else if (rules.get(i) instanceof GroupRule) {
                collect(((GroupRule) rules.get(i)).getRules(), out);
            }
        }
    }

    /**
     * Returns the faces of the set: the faces of the style sheets, then the faces that scripts added.
     * <p>
     * 집합의 글꼴 면을 반환한다: 스타일 시트의 글꼴 면, 이어서 스크립트가 더한 글꼴 면.
     *
     * @return the faces / 글꼴 면
     */
    List<FaceObject> allFaces() {
        List<FaceObject> out = new ArrayList<FaceObject>();
        List<FontFaceRule> rules = sheetRules();
        // 규칙마다 연결된 글꼴 면 객체를 (없으면 만들어) 모은다.
        for (int i = 0; i < rules.size(); i++) {
            FaceObject f = ruleFaces.get(rules.get(i));
            if (f == null) {
                f = fromRule(rules.get(i));
                ruleFaces.put(rules.get(i), f);
            }
            out.add(f);
        }
        out.addAll(added);
        return out;
    }

    /**
     * Makes the face object of a <code>@font-face</code> rule.
     * <p>
     * <code>@font-face</code> 규칙의 글꼴 면 객체를 만든다.
     *
     * @param r the rule / 규칙
     * @return the face, whose status is <code>loaded</code> / 글꼴 면. 상태는 <code>loaded</code>
     */
    private FaceObject fromRule(FontFaceRule r) {
        FaceObject f = new FaceObject(r.getFamily() == null ? "" : r.getFamily());
        f.fromRule = true;
        f.status = "loaded";
        f.source = r.getDescriptor("src");
        String v = r.getDescriptor("font-style");
        f.style = v != null ? v : f.style;
        v = r.getDescriptor("font-weight");
        f.weight = v != null ? v : f.weight;
        v = r.getDescriptor("font-stretch");
        f.stretch = v != null ? v : f.stretch;
        v = r.getDescriptor("unicode-range");
        f.unicodeRange = v != null ? v : f.unicodeRange;
        v = r.getDescriptor("font-display");
        f.display = v != null ? v : f.display;
        return f;
    }

    /**
     * Checks that an argument is a <code>FontFace</code> object.
     * <p>
     * 인수가 <code>FontFace</code> 객체인지 확인한다.
     *
     * @param o      the converted argument / 변환된 인수
     * @param method the method name for the message / 메시지용 메서드 이름
     * @return the face / 글꼴 면
     */
    FaceObject face(Object o, String method) {
        if (!(o instanceof FaceObject)) {
            throw JsException.typeError("Failed to execute '" + method + "' on 'FontFaceSet': parameter 1 is not of type 'FontFace'.");
        }
        return (FaceObject) o;
    }

    /**
     * Adds a face that a script made to the set (it applies to the page through the style sheet of the faces).
     * <p>
     * 스크립트가 만든 글꼴 면을 집합에 더한다(글꼴 면의 스타일 시트를 통해 페이지에 적용된다).
     *
     * @param f the face / 글꼴 면
     */
    void add(FaceObject f) {
        if (f.fromRule || added.contains(f)) {
            return;
        }
        added.add(f);
        rebuildSheet();
    }

    /**
     * Removes a face that a script added.
     * <p>
     * 스크립트가 더한 글꼴 면을 뺀다.
     *
     * @param f the face / 글꼴 면
     * @return true if it was in the set / 집합에 있었으면 true
     */
    boolean remove(FaceObject f) {
        boolean had = added.remove(f);
        if (had) {
            rebuildSheet();
        }
        return had;
    }

    /**
     * Removes all the faces that scripts added.
     * <p>
     * 스크립트가 더한 모든 글꼴 면을 뺀다.
     */
    void clearAdded() {
        if (!added.isEmpty()) {
            added.clear();
            rebuildSheet();
        }
    }

    /**
     * Writes the faces that scripts added as <code>@font-face</code> rules into the style sheet of the faces and lets the page adopt it.
     * <p>
     * 스크립트가 더한 글꼴 면을 <code>@font-face</code> 규칙으로 글꼴 면의 스타일 시트에 쓰고 페이지가 그것을 채택하게 한다.
     */
    private void rebuildSheet() {
        if (faceSheet == null) {
            faceSheet = StyleSheet.createConstructed(page.getDocument().getUrl());
        }
        StringBuilder css = new StringBuilder();
        // 글꼴 면마다 출처가 있는 것만 @font-face 규칙으로 쓴다(이진 출처는 적용하지 않는다).
        for (int i = 0; i < added.size(); i++) {
            FaceObject f = added.get(i);
            if (f.source == null || "error".equals(f.status)) {
                continue;
            }
            css.append("@font-face { font-family: ").append(CssEscape.string(f.family)).append("; src: ").append(f.source).append("; font-style: ").append(f.style)
                    .append("; font-weight: ").append(f.weight).append("; font-stretch: ").append(f.stretch).append("; unicode-range: ").append(f.unicodeRange)
                    .append("; font-display: ").append(f.display).append("; }\n");
        }
        faceSheet.replaceRules(css.toString());
        bindings.applyAdoptedSheets();
    }

    /**
     * Parses the family list of a font shorthand (<code>[style] [weight] size[/line-height] family, ...</code>) as far as the font loading needs it.
     * <p>
     * 글꼴 단축 표기(<code>[style] [weight] size[/line-height] family, ...</code>)의 글꼴 계열 목록을 글꼴 로딩이 필요한 만큼 파싱한다.
     *
     * @param font the shorthand / 단축 표기
     * @return the family names in lower case / 소문자 글꼴 계열 이름
     * @throws DomException <code>SyntaxError</code> if there is no size or family / 크기나 글꼴 계열이 없으면 <code>SyntaxError</code>
     */
    static List<String> families(String font) {
        String s = font == null ? "" : font.trim();
        String[] tokens = s.split("\\s+");
        int sizeAt = -1;
        // 크기 토큰(숫자와 단위, 또는 크기 키워드, 줄 높이가 붙을 수 있다)을 찾는다.
        for (int i = 0; i < tokens.length && sizeAt < 0; i++) {
            String t = tokens[i].toLowerCase(Locale.ENGLISH);
            int slash = t.indexOf('/');
            String size = slash >= 0 ? t.substring(0, slash) : t;
            if (size.matches("[0-9]*\\.?[0-9]+(px|pt|pc|em|rem|ex|ch|vw|vh|vmin|vmax|cm|mm|in|q|%)?") && !size.matches("[0-9]+")
                    || size.matches("xx-small|x-small|small|medium|large|x-large|xx-large|smaller|larger")) {
                sizeAt = i;
            }
        }
        if (sizeAt < 0 || sizeAt == tokens.length - 1) {
            throw DomException.syntax("The font shorthand '" + font + "' is not valid.");
        }
        StringBuilder rest = new StringBuilder();
        // 크기 뒤의 토큰을 이어 글꼴 계열 목록을 만든다.
        for (int i = sizeAt + 1; i < tokens.length; i++) {
            rest.append(rest.length() > 0 ? " " : "").append(tokens[i]);
        }
        List<String> out = new ArrayList<String>();
        String[] names = rest.toString().split(",");
        // 쉼표로 나눈 이름의 따옴표를 벗기고 소문자로 만든다.
        for (int i = 0; i < names.length; i++) {
            String n = names[i].trim();
            if (n.length() >= 2 && (n.charAt(0) == '"' || n.charAt(0) == '\'') && n.charAt(n.length() - 1) == n.charAt(0)) {
                n = n.substring(1, n.length() - 1);
            }
            if (n.length() > 0) {
                out.add(n.toLowerCase(Locale.ENGLISH));
            }
        }
        return out;
    }

    /**
     * Returns the faces whose family occurs in a font shorthand.
     * <p>
     * 글꼴 단축 표기에 나오는 글꼴 계열을 가진 글꼴 면을 반환한다.
     *
     * @param font the shorthand / 단축 표기
     * @return the faces / 글꼴 면
     */
    List<FaceObject> matching(String font) {
        List<String> names = families(font);
        List<FaceObject> out = new ArrayList<FaceObject>();
        List<FaceObject> all = allFaces();
        // 글꼴 면마다 계열이 목록에 있으면 일치한다.
        for (int i = 0; i < all.size(); i++) {
            if (names.contains(all.get(i).family.toLowerCase(Locale.ENGLISH))) {
                out.add(all.get(i));
            }
        }
        return out;
    }

    /**
     * Checks that the descriptors of the constructor are an object or undefined.
     * <p>
     * 생성자의 디스크립터가 객체나 undefined 인지 확인한다.
     *
     * @param f       the face / 글꼴 면
     * @param options the descriptors object or undefined / 디스크립터 객체 또는 undefined
     */
    private static void applyDescriptors(FaceObject f, JsValue options) {
        if (!options.isObject()) {
            return;
        }
        JsObject o = (JsObject) options;
        String[] names = { "style", "weight", "stretch", "unicodeRange", "variant", "featureSettings", "display" };
        // 디스크립터마다 있으면(undefined 가 아니면) 설정한다.
        for (int i = 0; i < names.length; i++) {
            JsValue v = o.get(names[i]);
            if (v.isUndefined()) {
                continue;
            }
            String t = Ops.toString(v);
            if (names[i].equals("style")) {
                f.setStyle(t);
            } else if (names[i].equals("weight")) {
                f.setWeight(t);
            } else if (names[i].equals("stretch")) {
                f.setStretch(t);
            } else if (names[i].equals("unicodeRange")) {
                f.setUnicodeRange(t);
            } else if (names[i].equals("variant")) {
                f.setVariant(t);
            } else if (names[i].equals("featureSettings")) {
                f.setFeatureSettings(t);
            } else {
                f.setDisplay(t);
            }
        }
    }

    /**
     * Tells whether a <code>src</code> text lists at least one <code>url()</code> or <code>local()</code> source.
     * <p>
     * <code>src</code> 텍스트에 <code>url()</code> 이나 <code>local()</code> 출처가 하나 이상 있는지 알려 준다.
     *
     * @param src the text / 텍스트
     * @return true if it can be parsed as a source list / 출처 목록으로 파싱할 수 있으면 true
     */
    private static boolean validSource(String src) {
        String t = src.trim().toLowerCase(Locale.ENGLISH);
        return t.startsWith("url(") || t.startsWith("local(");
    }

    /**
     * Creates an already settled promise.
     * <p>
     * 이미 확정된 프라미스를 만든다.
     *
     * @param value  the value or reason / 값 또는 이유
     * @param reject true to reject / reject 하려면 true
     * @return the promise / 프라미스
     */
    private JsObject settled(final JsValue value, final boolean reject) {
        JsRealm realm = page.getRealm();
        return Ops.construct(realm.getIntrinsic("%Promise%"), new JsValue[] { realm.newFunction("", 2, new NativeFunction.Call() {
            /** Settles the promise. / 프라미스를 확정한다. */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Ops.call(Js.arg(args, reject ? 1 : 0), Js.UNDEFINED, new JsValue[] { value });
                return Js.UNDEFINED;
            }
        }) });
    }

    /**
     * Creates the pending promise <code>loaded</code> of a face and keeps its resolve and reject functions.
     * <p>
     * 글꼴 면의 대기 중인 <code>loaded</code> 프라미스를 만들고 resolve, reject 함수를 보관한다.
     *
     * @param f the face / 글꼴 면
     */
    private void createLoadedPromise(final FaceObject f) {
        JsRealm realm = page.getRealm();
        f.loadedPromise = Ops.construct(realm.getIntrinsic("%Promise%"), new JsValue[] { realm.newFunction("", 2, new NativeFunction.Call() {
            /** Keeps the functions that settle the promise. / 프라미스를 확정하는 함수를 보관한다. */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                f.resolveFn = Js.arg(args, 0);
                f.rejectFn = Js.arg(args, 1);
                return Js.UNDEFINED;
            }
        }) });
    }

    /**
     * Marks a face as loaded and fulfills its <code>loaded</code> promise (the promise does nothing again if it is settled already).
     * <p>
     * 글꼴 면을 읽힌 것으로 표시하고 <code>loaded</code> 프라미스를 이행한다(이미 확정되었으면 다시 아무것도 하지 않는다).
     *
     * @param f the face / 글꼴 면
     * @param v the script object of the face / 글꼴 면의 스크립트 객체
     */
    private void markLoaded(FaceObject f, JsValue v) {
        if (!"error".equals(f.status)) {
            f.status = "loaded";
            if (f.resolveFn != null) {
                Ops.call(f.resolveFn, Js.UNDEFINED, new JsValue[] { v });
            }
        }
    }

    /**
     * Installs <code>FontFace</code>, <code>FontFaceSet</code> and <code>document.fonts</code>.
     * <p>
     * <code>FontFace</code>, <code>FontFaceSet</code>, <code>document.fonts</code> 를 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    void install(JsObject g) {
        final DomBridge bridge = page.getBridge();
        final JsRealm realm = page.getRealm();
        bridge.addClassifier(new DomBridge.Classifier() {
            /** Chooses the interfaces of the font loading objects. / 글꼴 로딩 객체의 인터페이스를 고른다. */
            public String classify(Object o) {
                return o instanceof FaceObject ? "FontFace" : o instanceof FontSet ? "FontFaceSet" : null;
            }
        });
        final DomBridge.InterfaceInfo face = bridge.defineInterface("FontFace", null, FaceObject.class, new String[] { "a:family=", "a:style=", "a:weight=",
            "a:stretch=", "a:unicodeRange=", "a:variant=", "a:featureSettings=", "a:display=", "a:status" });
        installFace(bridge, realm, face);
        DomBridge.InterfaceInfo fontSet = bridge.defineInterface("FontFaceSet", "EventTarget", FontSet.class, new String[] { "a:size", "a:status",
            "m:add(Object)", "m:delete(Object)", "m:has(Object)", "m:clear()", "m:check(String,String*)" });
        bridge.getEventHandlers().define(fontSet.getPrototype(), FontSet.class, new String[] { "loading", "loadingdone", "loadingerror" });
        installSet(bridge, realm, fontSet);
        readyPromise = settled(bridge.toJs(set), false);
        CssomMembers.accessor(bridge, bridge.getInterface("Document"), "fonts", new CssomMembers.Op() {
            /** Returns the font face set of the document. / 문서의 글꼴 면 집합을 반환한다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                return b.toJs(set);
            }
        }, null);
    }

    /**
     * Installs the constructor and the promise members of <code>FontFace</code>.
     * <p>
     * <code>FontFace</code> 의 생성자와 프라미스 멤버를 설치한다.
     *
     * @param bridge the bridge / 브리지
     * @param realm  the realm / realm
     * @param face   the interface / 인터페이스
     */
    private void installFace(final DomBridge bridge, final JsRealm realm, final DomBridge.InterfaceInfo face) {
        bridge.setConstructor("FontFace", new NativeFunction(realm, "FontFace", 2, new NativeFunction.Call() {
            /** Calling without new is an error. / new 없이 부르면 오류이다. */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                throw JsException.typeError("Failed to construct 'FontFace': Please use the 'new' operator, this DOM object constructor cannot be called "
                        + "as a function.");
            }
        }, new NativeFunction.Construct() {
            /** Creates a font face from the family, the source and the descriptors. / 계열, 출처, 디스크립터로 글꼴 면을 만든다. */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                if (args.length < 2) {
                    throw JsException.typeError("Failed to construct 'FontFace': 2 arguments required, but only " + args.length + " present.");
                }
                FaceObject f = new FaceObject(Ops.toString(args[0]).trim());
                try {
                    applyDescriptors(f, Js.arg(args, 2));
                } catch (DomException e) {
                    throw bridge.domError(e);
                }
                createLoadedPromise(f);
                HostObject w = bridge.wrap(f);
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, face.getPrototype()));
                // 출처는 문자열이면 src 목록으로 확인하고, 이진 데이터이면 받아들이되 적용하지 않는다.
                if (args[1] instanceof JsString) {
                    String src = Ops.toString(args[1]);
                    if (f.family.length() == 0 || !validSource(src)) {
                        f.status = "error";
                        Ops.call(f.rejectFn, Js.UNDEFINED, new JsValue[] { bridge.domError(DomException.syntax("The source of the font face is not valid.")).getValue(r) });
                    } else {
                        f.source = src;
                    }
                } else {
                    f.status = "loaded";
                    Ops.call(f.resolveFn, Js.UNDEFINED, new JsValue[] { w });
                }
                return w;
            }
        }));
        CssomMembers.accessor(bridge, face, "loaded", new CssomMembers.Op() {
            /** Returns the promise that settles when the face is loaded. / 글꼴 면을 읽으면 확정되는 프라미스를 반환한다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                FaceObject f = (FaceObject) target;
                if (f.loadedPromise == null) {
                    createLoadedPromise(f);
                    markLoaded(f, b.toJs(f));
                }
                return f.loadedPromise;
            }
        }, null);
        CssomMembers.method(bridge, face, "load", 0, new CssomMembers.Op() {
            /** Loads the face: the status becomes loaded (see the limits of the class). / 글꼴 면을 읽는다: 상태가 loaded 가 된다(클래스의 한계 참고). */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                FaceObject f = (FaceObject) target;
                if (f.loadedPromise == null) {
                    createLoadedPromise(f);
                }
                if ("error".equals(f.status)) {
                    return f.loadedPromise;
                }
                markLoaded(f, b.toJs(f));
                return f.loadedPromise;
            }
        });
    }

    /**
     * Installs the promise, iteration and callback members of <code>FontFaceSet</code>.
     * <p>
     * <code>FontFaceSet</code> 의 프라미스, 반복, 콜백 멤버를 설치한다.
     *
     * @param bridge  the bridge / 브리지
     * @param realm   the realm / realm
     * @param fontSet the interface / 인터페이스
     */
    private void installSet(final DomBridge bridge, final JsRealm realm, DomBridge.InterfaceInfo fontSet) {
        CssomMembers.accessor(bridge, fontSet, "ready", new CssomMembers.Op() {
            /** Returns the promise that is fulfilled with the set. / 집합으로 이행된 프라미스를 반환한다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                return readyPromise;
            }
        }, null);
        CssomMembers.method(bridge, fontSet, "load", 1, new CssomMembers.Op() {
            /** Loads the faces that match the font and returns a promise for them. / 글꼴과 일치하는 글꼴 면을 읽고 그것에 대한 프라미스를 반환한다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                if (args.length < 1) {
                    throw JsException.typeError("Failed to execute 'load' on 'FontFaceSet': 1 argument required, but only 0 present.");
                }
                List<FaceObject> faces = matching(Ops.toString(args[0]));
                JsValue[] out = new JsValue[faces.size()];
                // 일치하는 글꼴 면을 읽힌 것으로 표시하고 스크립트 객체를 모은다.
                for (int i = 0; i < out.length; i++) {
                    JsValue w = b.toJs(faces.get(i));
                    if (faces.get(i).loadedPromise == null) {
                        createLoadedPromise(faces.get(i));
                    }
                    markLoaded(faces.get(i), w);
                    out[i] = w;
                }
                return settled(realm.newArray(out), false);
            }
        });
        CssomMembers.method(bridge, fontSet, "forEach", 1, new CssomMembers.Op() {
            /** Calls a function for every face with the face twice and the set, as a Set does. / Set 처럼 모든 글꼴 면에 대해 글꼴 면 둘과 집합으로 함수를 부른다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                JsValue cb = Js.arg(args, 0);
                if (!cb.isCallable()) {
                    throw JsException.typeError("Failed to execute 'forEach' on 'FontFaceSet': The callback provided as parameter 1 is not a function.");
                }
                List<FaceObject> faces = allFaces();
                // 글꼴 면의 스냅샷을 순서대로 콜백에 넘긴다.
                for (int i = 0; i < faces.size(); i++) {
                    JsValue w = b.toJs(faces.get(i));
                    Ops.call(cb, Js.arg(args, 1), new JsValue[] { w, w, b.toJs(set) });
                }
                return Js.UNDEFINED;
            }
        });
        CssomMembers.method(bridge, fontSet, "values", 0, new CssomMembers.Op() {
            /** Returns an iterator over the faces. / 글꼴 면에 대한 반복자를 반환한다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                return iterator(b, realm, false);
            }
        });
        fontSet.getPrototype().defineData("keys", fontSet.getPrototype().get("values"), true, false, true);
        CssomMembers.method(bridge, fontSet, "entries", 0, new CssomMembers.Op() {
            /** Returns an iterator over [face, face] pairs. / [글꼴 면, 글꼴 면] 쌍에 대한 반복자를 반환한다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                return iterator(b, realm, true);
            }
        });
        fontSet.getPrototype().defineData(WellKnownSymbols.ITERATOR, fontSet.getPrototype().get("values"), true, false, true);
    }

    /**
     * Creates the iterator of the faces by iterating an array of a snapshot of the faces.
     * <p>
     * 글꼴 면의 스냅샷 배열을 반복하여 글꼴 면의 반복자를 만든다.
     *
     * @param b       the bridge / 브리지
     * @param realm   the realm / realm
     * @param entries true for [face, face] pairs / [글꼴 면, 글꼴 면] 쌍이면 true
     * @return the iterator / 반복자
     */
    private JsValue iterator(DomBridge b, JsRealm realm, boolean entries) {
        List<FaceObject> faces = allFaces();
        JsValue[] items = new JsValue[faces.size()];
        // 글꼴 면의 스크립트 객체(또는 쌍)로 배열을 만든다.
        for (int i = 0; i < items.length; i++) {
            JsValue w = b.toJs(faces.get(i));
            items[i] = entries ? realm.newArray(new JsValue[] { w, w }) : w;
        }
        JsArray array = realm.newArray(items);
        return Ops.call(array.get("values"), array, new JsValue[0]);
    }
}
