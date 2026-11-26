package org.duckdns.hjow.swingwebpanel.js.host;

import java.util.ArrayList;
import java.util.List;

import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.css.cssom.CssStyleDeclaration;
import org.duckdns.hjow.swingwebpanel.css.cssom.StyleSheet;
import org.duckdns.hjow.swingwebpanel.css.media.MediaQueryList;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.js.IteratorHelper;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsArray;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.dom.CssomInterfaces;
import org.duckdns.hjow.swingwebpanel.js.dom.CssomMembers;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.dom.HostObject;
import org.duckdns.hjow.swingwebpanel.js.dom.IndexedObject;
import org.duckdns.hjow.swingwebpanel.js.dom.StyleObject;
import org.duckdns.hjow.swingwebpanel.net.Origin;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;

/**
 * The page part of the CSS Object Model (CSSOM, <code>https://www.w3.org/TR/cssom-1/</code>; Constructable Stylesheets, CSS Object Model section
 * "Constructing a CSSStyleSheet"): it installs the interfaces of {@link CssomInterfaces} and connects them to the page. <code>document.styleSheets</code>
 * is the live list of the style sheets of the CSS engine of the page (the engine collects <code>&lt;style&gt;</code>, <code>&lt;link&gt;</code>
 * and <code>@import</code> sheets in document order, without the user agent sheet), <code>style.sheet</code> and <code>link.sheet</code> are the sheet of
 * the element, <code>new CSSStyleSheet(options)</code> with <code>replace()</code>, <code>replaceSync()</code> and <code>document.adoptedStyleSheets</code>
 * make constructed sheets that the document applies after its own sheets, and <code>rule.style</code> is the live declaration block that the style
 * resolver reads. A style sheet from another origin cannot be read (<code>SecurityError</code>, section 6.2 of the CSSOM). Every change a script makes
 * reaches the CSS engine, which computes the styles again and lets the view draw the page again.
 * <p>
 * CSS 객체 모델(CSSOM, <code>https://www.w3.org/TR/cssom-1/</code>; 생성 가능한 스타일 시트, CSSOM 의 "Constructing a CSSStyleSheet")의 페이지 쪽 부분: {@link CssomInterfaces} 의
 * 인터페이스를 설치하고 페이지에 연결한다. <code>document.styleSheets</code> 는 페이지의 CSS 엔진이 가진 스타일 시트의 실시간 목록이다(엔진이
 * <code>&lt;style&gt;</code>, <code>&lt;link&gt;</code>, <code>@import</code> 시트를 문서 순서로 모으며 사용자 에이전트 시트는 없다). <code>style.sheet</code>,
 * <code>link.sheet</code> 는 요소의 시트이고, <code>new CSSStyleSheet(options)</code> 와 <code>replace()</code>, <code>replaceSync()</code>,
 * <code>document.adoptedStyleSheets</code> 는 문서가 자신의 시트 다음에 적용하는 생성 시트를 만들며, <code>rule.style</code> 은 스타일 리졸버가 읽는 실시간 선언 블록이다.
 * 다른 오리진의 스타일 시트는 읽을 수 없다(<code>SecurityError</code>, CSSOM 6.2 절). 스크립트가 하는 모든 변경은 CSS 엔진에 닿아 스타일을 다시 계산하고 뷰가 페이지를
 * 다시 그리게 한다.
 */
final class CssomBindings {

    /**
     * The target of <code>document.styleSheets</code>, the live list of the style sheets of the document.
     * <p>
     * <code>document.styleSheets</code> 의 대상으로 문서의 스타일 시트의 실시간 목록이다.
     */
    public static final class SheetList {
        /** The bindings of the page that know where the sheets are. / 시트가 어디 있는지 아는 페이지의 바인딩. */
        private final CssomBindings owner;

        /**
         * Creates the list.
         * <p>
         * 목록을 만든다.
         *
         * @param owner the bindings of the page / 페이지의 바인딩
         */
        SheetList(CssomBindings owner) {
            this.owner = owner;
        }

        /**
         * Returns the number of style sheets (CSSOM <code>length</code>).
         * <p>
         * 스타일 시트 수를 반환한다(CSSOM <code>length</code>).
         *
         * @return the count / 개수
         */
        public int getLength() {
            return owner.documentSheets().size();
        }

        /**
         * Returns the style sheet at an index (CSSOM <code>item</code>).
         * <p>
         * 위치의 스타일 시트를 반환한다(CSSOM <code>item</code>).
         *
         * @param index the position / 위치
         * @return the sheet, or null if the index is out of range / 시트. 위치가 범위를 벗어나면 null
         */
        public StyleSheet item(int index) {
            List<StyleSheet> l = owner.documentSheets();
            return index >= 0 && index < l.size() ? l.get(index) : null;
        }
    }

    /**
     * The array that <code>document.adoptedStyleSheets</code> returns: an ordinary array that tells the page after every change (an observable
     * array), so that <code>adoptedStyleSheets.push(sheet)</code> adopts the sheet at once.
     * <p>
     * <code>document.adoptedStyleSheets</code> 가 반환하는 배열: 변경할 때마다 페이지에 알리는 보통 배열(관찰 가능 배열). 그래서
     * <code>adoptedStyleSheets.push(sheet)</code> 가 시트를 바로 채택한다.
     */
    static final class ObservableSheets extends JsArray {
        /** Told after every change of the array, or null. / 배열이 바뀐 뒤마다 알려 받는 쪽. 없으면 null. */
        private Runnable listener;

        /**
         * Creates an empty array.
         * <p>
         * 빈 배열을 만든다.
         *
         * @param proto the prototype (<code>Array.prototype</code>) / 프로토타입(<code>Array.prototype</code>)
         */
        ObservableSheets(JsObject proto) {
            super(proto);
        }

        /**
         * Sets the receiver of the change notifications.
         * <p>
         * 변경 통지를 받는 쪽을 설정한다.
         *
         * @param l the receiver / 받는 쪽
         */
        void setListener(Runnable l) {
            this.listener = l;
        }

        /**
         * Tells the receiver that the array changed.
         * <p>
         * 배열이 바뀌었다고 받는 쪽에 알린다.
         */
        private void changed() {
            Runnable l = listener;
            if (l != null) {
                l.run();
            }
        }

        /**
         * Defines an element or the length, then notifies.
         * <p>
         * 요소나 length 를 정의한 뒤 알린다.
         *
         * @param key  the key / 키
         * @param desc the descriptor / 기술자
         * @return true on success / 성공하면 true
         */
        public boolean defineOwnProperty(Object key, PropertyDescriptor desc) {
            boolean ok = super.defineOwnProperty(key, desc);
            changed();
            return ok;
        }

        /**
         * Deletes an element, then notifies.
         * <p>
         * 요소를 지운 뒤 알린다.
         *
         * @param key the key / 키
         * @return true if gone afterwards / 삭제 후 없으면 true
         */
        public boolean delete(Object key) {
            boolean ok = super.delete(key);
            changed();
            return ok;
        }

        /**
         * Overwrites an element in place, then notifies.
         * <p>
         * 요소를 제자리에서 덮어쓴 뒤 알린다.
         *
         * @param i the index / 인덱스
         * @param v the value / 값
         * @return true if written / 썼으면 true
         */
        public boolean setOwnElement(int i, JsValue v) {
            boolean ok = super.setOwnElement(i, v);
            if (ok) {
                changed();
            }
            return ok;
        }

        /**
         * Appends an element, then notifies.
         * <p>
         * 요소를 덧붙인 뒤 알린다.
         *
         * @param v the element / 요소
         */
        public void add(JsValue v) {
            super.add(v);
            changed();
        }
    }

    /** The page. / 페이지. */
    private final PageScripts page;

    /** The target of <code>document.styleSheets</code>. / <code>document.styleSheets</code> 의 대상. */
    private final SheetList sheetList = new SheetList(this);

    /** The array of <code>document.adoptedStyleSheets</code>, created when the interfaces are installed. / <code>document.adoptedStyleSheets</code> 의 배열. 인터페이스를 설치할 때 만든다. */
    private ObservableSheets adoptedArray;

    /** The adopted sheets that were given to the engine last. / 마지막으로 엔진에 준 채택한 시트. */
    private List<StyleSheet> adoptedApplied = new ArrayList<StyleSheet>();

    /** The font loading interfaces of the page (their faces are adopted as a hidden style sheet), created when the interfaces are installed. / 페이지의 글꼴 로딩 인터페이스(그 글꼴 면은 숨은 스타일 시트로 채택된다). 인터페이스를 설치할 때 만든다. */
    private CssFontApi fonts;

    /**
     * Creates the installer of a page.
     * <p>
     * 페이지의 설치기를 만든다.
     *
     * @param page the page / 페이지
     */
    CssomBindings(PageScripts page) {
        this.page = page;
    }

    /**
     * Returns the CSS engine of the page, or null.
     * <p>
     * 페이지의 CSS 엔진을 반환한다. 없으면 null.
     *
     * @return the engine or null / 엔진 또는 null
     */
    private CssEngine engine() {
        return page.getEnvironment().getCssEngine();
    }

    /**
     * Returns the style sheets of the document in document order (empty if the page has no CSS engine yet).
     * <p>
     * 문서의 스타일 시트를 문서 순서로 반환한다(페이지에 CSS 엔진이 아직 없으면 빈 목록).
     *
     * @return the sheets / 시트
     */
    List<StyleSheet> documentSheets() {
        CssEngine e = engine();
        List<StyleSheet> l = e == null ? null : e.getDocumentStyleSheets();
        return l == null ? new ArrayList<StyleSheet>() : l;
    }

    /**
     * Returns the sheet that an element owns (<code>style.sheet</code>, <code>link.sheet</code>), or null while it has none.
     * <p>
     * 요소가 소유한 시트를 반환한다(<code>style.sheet</code>, <code>link.sheet</code>). 아직 없으면 null.
     *
     * @param owner the <code>style</code> or <code>link</code> element / <code>style</code> 또는 <code>link</code> 요소
     * @return the sheet or null / 시트 또는 null
     */
    private StyleSheet sheetOf(Element owner) {
        List<StyleSheet> l = documentSheets();
        // 문서의 시트 중 이 요소가 소유한 것을 찾는다.
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).getOwner() == owner) {
                return l.get(i);
            }
        }
        return null;
    }

    /**
     * Checks that the page may read the rules of a sheet: a sheet that was loaded from another origin than the document is not readable
     * (CSSOM, "origin-clean flag").
     * <p>
     * 페이지가 시트의 규칙을 읽을 수 있는지 검사한다: 문서와 다른 오리진에서 읽은 시트는 읽을 수 없다(CSSOM, "origin-clean flag").
     *
     * @param sheet the sheet / 시트
     */
    private void checkReadable(StyleSheet sheet) {
        String href = sheet.getHref();
        // 인라인 시트나 생성한 시트는 항상 읽을 수 있다.
        if (href == null || sheet.isOriginClean()) {
            return;
        }
        Origin doc = Origin.of(page.getDocument().getOriginUrl());
        if (!doc.isSameOrigin(Origin.of(href))) {
            throw DomException.security("Cannot access rules of a style sheet from another origin: " + href);
        }
    }

    /**
     * Returns the prototype of an interface.
     * <p>
     * 인터페이스의 프로토타입을 반환한다.
     *
     * @param name the interface name / 인터페이스 이름
     * @return the prototype / 프로토타입
     */
    private JsObject proto(String name) {
        return page.getBridge().getInterface(name).getPrototype();
    }

    /**
     * Installs everything.
     * <p>
     * 모두 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    void install(JsObject g) {
        final DomBridge bridge = page.getBridge();
        final JsRealm realm = page.getRealm();
        CssomInterfaces.install(bridge, new CssomInterfaces.Guard() {
            /** Refuses the rules of a sheet from another origin. / 다른 오리진의 시트의 규칙을 거부한다. */
            public void check(StyleSheet sheet) {
                checkReadable(sheet);
            }
        });
        installDeclarations(bridge);
        installSheetList(bridge);
        installElementSheets(bridge);
        installConstructable(bridge, realm);
        new CssNamespaceApi(page).install(g);
        fonts = new CssFontApi(page, this);
        fonts.install(g);
    }

    /**
     * Connects the declaration blocks of rules to <code>CSSStyleDeclaration</code> (the wrapper is a {@link StyleObject} that edits the block in
     * place) and adds <code>parentRule</code>.
     * <p>
     * 규칙의 선언 블록을 <code>CSSStyleDeclaration</code> 에 연결하고(래퍼는 블록을 제자리에서 편집하는 {@link StyleObject}) <code>parentRule</code> 을 더한다.
     *
     * @param bridge the bridge / 브리지
     */
    private void installDeclarations(DomBridge bridge) {
        bridge.setFactory("CSSStyleDeclaration", new DomBridge.Factory() {
            /** Creates the script object of a declaration block. / 선언 블록의 스크립트 객체를 만든다. */
            public HostObject create(DomBridge b, JsObject proto, Object target) {
                return new StyleObject(b, proto, (CssStyleDeclaration) target);
            }
        });
        JsObject styleProto = proto("CSSStyleDeclaration");
        final JsRealm realm = page.getRealm();
        styleProto.defineAccessor("parentRule", realm.newFunction("get parentRule", 0, new NativeFunction.Call() {
            /** Returns the rule that owns the block, or null. / 블록을 가진 규칙을 반환한다. 없으면 null. */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                if (!(self instanceof StyleObject)) {
                    throw JsException.typeError("Illegal invocation");
                }
                return page.getBridge().toJs(((StyleObject) self).getParentRule());
            }
        }), null, true, true);
    }

    /**
     * Installs <code>StyleSheetList</code> and <code>document.styleSheets</code>.
     * <p>
     * <code>StyleSheetList</code> 와 <code>document.styleSheets</code> 를 설치한다.
     *
     * @param bridge the bridge / 브리지
     */
    private void installSheetList(final DomBridge bridge) {
        bridge.addClassifier(new DomBridge.Classifier() {
            /** Chooses the interface of the sheet list. / 시트 목록의 인터페이스를 고른다. */
            public String classify(Object o) {
                return o instanceof SheetList ? "StyleSheetList" : null;
            }
        });
        bridge.defineInterface("StyleSheetList", null, SheetList.class, new String[] { "a:length", "m:item(int)" });
        bridge.setIndexed("StyleSheetList", new IndexedObject.Source() {
            /** Returns the number of sheets. / 시트 수를 반환한다. */
            public int length(Object target) {
                return ((SheetList) target).getLength();
            }

            /** Returns a sheet. / 시트를 반환한다. */
            public Object item(Object target, int index) {
                return ((SheetList) target).item(index);
            }

            /** The list has no named items. / 목록에는 이름 있는 항목이 없다. */
            public Object named(Object target, String name) {
                return null;
            }
        });
        CssomMembers.accessor(bridge, bridge.getInterface("Document"), "styleSheets", new CssomMembers.Op() {
            /** Returns the list of the style sheets of the document. / 문서의 스타일 시트 목록을 반환한다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                return b.toJs(sheetList);
            }
        }, null);
    }

    /**
     * Installs <code>sheet</code> and <code>disabled</code> of <code>style</code> and <code>link</code> elements: <code>disabled</code> reads and
     * sets the disabled flag of the sheet of the element (the content attribute only while the element has no sheet).
     * <p>
     * <code>style</code> 과 <code>link</code> 요소의 <code>sheet</code>, <code>disabled</code> 를 설치한다: <code>disabled</code> 는 요소의 시트의 비활성 플래그를 읽고 쓴다
     * (요소에 시트가 없는 동안에만 콘텐츠 속성).
     *
     * @param bridge the bridge / 브리지
     */
    private void installElementSheets(DomBridge bridge) {
        String[] names = { "HTMLStyleElement", "HTMLLinkElement" };
        // 두 인터페이스에 같은 멤버를 설치한다.
        for (int i = 0; i < names.length; i++) {
            DomBridge.InterfaceInfo info = bridge.getInterface(names[i]);
            CssomMembers.accessor(bridge, info, "sheet", new CssomMembers.Op() {
                /** Returns the style sheet of the element, or null. / 요소의 스타일 시트를 반환한다. 없으면 null. */
                public JsValue run(DomBridge b, Object target, JsValue[] args) {
                    return b.toJs(sheetOf((Element) target));
                }
            }, null);
            CssomMembers.accessor(bridge, info, "disabled", new CssomMembers.Op() {
                /** Reads the disabled flag. / 비활성 플래그를 읽는다. */
                public JsValue run(DomBridge b, Object target, JsValue[] args) {
                    StyleSheet s = sheetOf((Element) target);
                    return Js.bool(s != null ? s.isDisabled() : ((Element) target).hasAttribute("disabled"));
                }
            }, new CssomMembers.Op() {
                /** Sets the disabled flag. / 비활성 플래그를 설정한다. */
                public JsValue run(DomBridge b, Object target, JsValue[] args) {
                    boolean d = Ops.toBoolean(Js.arg(args, 0));
                    StyleSheet s = sheetOf((Element) target);
                    if (s != null) {
                        s.setDisabled(d);
                    } else if (d) {
                        ((Element) target).setAttribute("disabled", "");
                    } else {
                        ((Element) target).removeAttribute("disabled");
                    }
                    return Js.UNDEFINED;
                }
            });
        }
    }

    /**
     * Returns the sheets of a list of script values, which must be constructed style sheets of this page.
     * <p>
     * 스크립트 값 목록의 시트를 반환한다. 이 페이지의 생성 스타일 시트여야 한다.
     *
     * @param values the values / 값
     * @param strict true to throw for a value that is not a constructed sheet, false to skip it / 생성 시트가 아닌 값에서 던지려면 true, 건너뛰려면 false
     * @return the sheets / 시트
     */
    private List<StyleSheet> constructedSheets(List<JsValue> values, boolean strict) {
        List<StyleSheet> out = new ArrayList<StyleSheet>();
        // 값마다 생성 시트인지 확인하고 모은다.
        for (int i = 0; i < values.size(); i++) {
            Object t = DomBridge.targetOf(values.get(i), StyleSheet.class);
            if (t != null && ((StyleSheet) t).isConstructed()) {
                out.add((StyleSheet) t);
            } else if (strict && t == null) {
                // 시트가 아닌 값은 WebIDL 변환이 TypeError 로 거부한다.
                throw JsException.typeError("Failed to set the 'adoptedStyleSheets' property on 'Document': Failed to convert value to 'CSSStyleSheet'.");
            } else if (strict) {
                // 생성하지 않은 시트는 CSSOM 의 "set an indexed value" 알고리즘이 NotAllowedError 로 거부한다.
                throw page.getBridge().domError(new DomException("NotAllowedError", (short) 0,
                        "Failed to set the 'adoptedStyleSheets' property on 'Document': Can't adopt non-constructed stylesheets."));
            }
        }
        return out;
    }

    /**
     * Gives the CSS engine the sheets that the <code>adoptedStyleSheets</code> array holds now (only when they differ from the ones it has).
     * <p>
     * <code>adoptedStyleSheets</code> 배열이 지금 가진 시트를 CSS 엔진에 준다(가진 것과 다를 때만).
     */
    private void applyAdopted() {
        List<JsValue> values = new ArrayList<JsValue>();
        long n = adoptedArray.getLength();
        // 배열의 요소를 차례로 읽는다.
        for (int i = 0; i < n; i++) {
            values.add(adoptedArray.get(String.valueOf(i)));
        }
        List<StyleSheet> now = constructedSheets(values, false);
        StyleSheet faces = fonts == null ? null : fonts.faceSheet();
        // 스크립트가 더한 글꼴 면의 숨은 시트는 채택한 시트의 끝에 놓는다.
        if (faces != null) {
            now.add(faces);
        }
        if (now.equals(adoptedApplied)) {
            return;
        }
        adoptedApplied = now;
        CssEngine e = engine();
        if (e != null) {
            e.setAdoptedStyleSheets(now);
        }
    }

    /**
     * Gives the CSS engine the adopted sheets again (the font loading API calls it after it changed the faces that scripts added).
     * <p>
     * CSS 엔진에 채택한 시트를 다시 준다(글꼴 로딩 API 가 스크립트가 더한 글꼴 면을 바꾼 뒤 부른다).
     */
    void applyAdoptedSheets() {
        if (adoptedArray != null) {
            applyAdopted();
        }
    }

    /**
     * Creates a promise that is settled at once with a value or a reason.
     * <p>
     * 값이나 이유로 바로 확정된 프라미스를 만든다.
     *
     * @param value  the value or the reason / 값 또는 이유
     * @param reject true to reject / reject 하려면 true
     * @return the promise / 프라미스
     */
    private JsObject settledPromise(final JsValue value, final boolean reject) {
        final JsRealm realm = page.getRealm();
        return Ops.construct(realm.getIntrinsic("%Promise%"), new JsValue[] { realm.newFunction("", 2, new NativeFunction.Call() {
            /** Settles the promise with the value. / 값으로 프라미스를 확정한다. */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Ops.call(Js.arg(args, reject ? 1 : 0), Js.UNDEFINED, new JsValue[] { value });
                return Js.UNDEFINED;
            }
        }) });
    }

    /**
     * Installs <code>new CSSStyleSheet(options)</code>, <code>replace()</code>, <code>replaceSync()</code> and
     * <code>document.adoptedStyleSheets</code>.
     * <p>
     * <code>new CSSStyleSheet(options)</code>, <code>replace()</code>, <code>replaceSync()</code>, <code>document.adoptedStyleSheets</code> 를 설치한다.
     *
     * @param bridge the bridge / 브리지
     * @param realm  the realm / realm
     */
    private void installConstructable(final DomBridge bridge, final JsRealm realm) {
        final DomBridge.InterfaceInfo sheet = bridge.getInterface("CSSStyleSheet");
        bridge.setConstructor("CSSStyleSheet", new NativeFunction(realm, "CSSStyleSheet", 0, new NativeFunction.Call() {
            /** Calling without new is an error. / new 없이 부르면 오류이다. */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                throw JsException.typeError("Failed to construct 'CSSStyleSheet': Please use the 'new' operator, this DOM object constructor cannot be "
                        + "called as a function.");
            }
        }, new NativeFunction.Construct() {
            /** Creates a constructed style sheet from the options. / 옵션으로 생성 스타일 시트를 만든다. */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                try {
                    HostObject w = bridge.wrap(constructSheet(Js.arg(args, 0)));
                    w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, sheet.getPrototype()));
                    return w;
                } catch (DomException e) {
                    throw bridge.domError(e);
                }
            }
        }));
        CssomMembers.method(bridge, sheet, "replaceSync", 1, new CssomMembers.Op() {
            /** Replaces the rules of a constructed sheet at once. / 생성 시트의 규칙을 바로 바꾼다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                if (args.length < 1) {
                    throw JsException.typeError("Failed to execute 'replaceSync' on 'CSSStyleSheet': 1 argument required, but only 0 present.");
                }
                StyleSheet s = (StyleSheet) target;
                if (!s.isConstructed()) {
                    throw b.domError(new DomException("NotAllowedError", (short) 0, "Can't call replaceSync on non-constructed CSSStyleSheets."));
                }
                s.replaceRules(Ops.toString(args[0]));
                return Js.UNDEFINED;
            }
        });
        CssomMembers.method(bridge, sheet, "replace", 1, new CssomMembers.Op() {
            /** Replaces the rules of a constructed sheet and returns a promise for the sheet. / 생성 시트의 규칙을 바꾸고 시트에 대한 프라미스를 반환한다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                if (args.length < 1) {
                    return settledPromise(JsException.typeError("Failed to execute 'replace' on 'CSSStyleSheet': 1 argument required, but only 0 present.")
                            .getValue(realm), true);
                }
                StyleSheet s = (StyleSheet) target;
                if (!s.isConstructed()) {
                    return settledPromise(b.domError(new DomException("NotAllowedError", (short) 0, "Can't call replace on non-constructed CSSStyleSheets."))
                            .getValue(realm), true);
                }
                s.replaceRules(Ops.toString(args[0]));
                return settledPromise(b.toJs(s), false);
            }
        });
        adoptedArray = new ObservableSheets(realm.newArray().getPrototypeOf());
        adoptedArray.setListener(new Runnable() {
            /** Applies the adopted sheets after the array changed. / 배열이 바뀐 뒤 채택한 시트를 적용한다. */
            public void run() {
                applyAdopted();
            }
        });
        CssomMembers.accessor(bridge, bridge.getInterface("Document"), "adoptedStyleSheets", new CssomMembers.Op() {
            /** Returns the array of the adopted sheets. / 채택한 시트의 배열을 반환한다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                return adoptedArray;
            }
        }, new CssomMembers.Op() {
            /** Replaces the adopted sheets by the given sequence. / 채택한 시트를 주어진 시퀀스로 바꾼다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                List<StyleSheet> sheets = constructedSheets(IteratorHelper.toList(realm, Js.arg(args, 0)), true);
                adoptedArray.setListener(null);
                try {
                    adoptedArray.set("length", Js.num(0), adoptedArray);
                    // 시트를 차례로 배열에 넣는다.
                    for (int i = 0; i < sheets.size(); i++) {
                        adoptedArray.add(b.toJs(sheets.get(i)));
                    }
                } finally {
                    adoptedArray.setListener(new Runnable() {
                        /** Applies the adopted sheets after the array changed. / 배열이 바뀐 뒤 채택한 시트를 적용한다. */
                        public void run() {
                            applyAdopted();
                        }
                    });
                }
                applyAdopted();
                return Js.UNDEFINED;
            }
        });
    }

    /**
     * Creates a constructed style sheet from the options of <code>new CSSStyleSheet(options)</code>: <code>baseURL</code> (relative urls of the sheet
     * resolve against it, the base URL of the document by default), <code>media</code> (text or a media list) and <code>disabled</code>.
     * <p>
     * <code>new CSSStyleSheet(options)</code> 의 옵션으로 생성 스타일 시트를 만든다: <code>baseURL</code>(시트의 상대 url 의 기준. 기본은 문서의 기준 URL),
     * <code>media</code>(텍스트 또는 미디어 목록), <code>disabled</code>.
     *
     * @param options the options object or undefined / 옵션 객체 또는 undefined
     * @return the sheet / 시트
     */
    private StyleSheet constructSheet(JsValue options) {
        Document doc = page.getDocument();
        String base = doc.getBaseUri() != null && doc.getBaseUri().length() > 0 ? doc.getBaseUri() : doc.getUrl();
        String media = "";
        boolean disabled = false;
        if (options.isObject()) {
            JsObject o = (JsObject) options;
            JsValue b = o.get("baseURL");
            if (!b.isUndefined()) {
                try {
                    base = base == null ? Ops.toString(b) : UrlUtil.resolve(base, Ops.toString(b));
                } catch (RuntimeException e) {
                    throw new DomException("NotAllowedError", (short) 0, "Failed to construct 'CSSStyleSheet': Invalid base URL.");
                }
            }
            JsValue m = o.get("media");
            if (!m.isUndefined()) {
                media = Ops.toString(m);
            }
            disabled = Ops.toBoolean(o.get("disabled"));
        }
        StyleSheet s = StyleSheet.createConstructed(base);
        s.setMedia(MediaQueryList.parse(media));
        s.setDisabled(disabled);
        return s;
    }
}
