package org.duckdns.hjow.swingwebpanel.css;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.cascade.StyleResolver;
import org.duckdns.hjow.swingwebpanel.css.cssom.DocumentStyleSheets;
import org.duckdns.hjow.swingwebpanel.css.cssom.StyleSheet;
import org.duckdns.hjow.swingwebpanel.css.cssom.StyleSheetLoader;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.css.selector.CssSelectorEngine;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;

/**
 * The CSS engine of one document, wired together: it installs the selector engine (<code>querySelector</code> and friends), collects the style sheets of the
 * document (<code>&lt;style&gt;</code>, <code>&lt;link rel="stylesheet"&gt;</code>, <code>@import</code>) through a {@link StyleSheetLoader}, and creates the
 * {@link StyleResolver} that computes the styles. The current DOM owner creates one engine per document, calls
 * {@link #loadStyleSheets()} during parsing or on the confirmed document queue, and shares it with layout. When the sources of the style sheets change
 * ({@link #refreshIfNeeded()}) the sheets are collected again.
 * <p>
 * 문서 하나의 CSS 엔진을 하나로 묶은 것: 선택자 엔진(<code>querySelector</code> 등)을 설치하고, {@link StyleSheetLoader} 로 문서의 스타일 시트(<code>&lt;style&gt;</code>,
 * <code>&lt;link rel="stylesheet"&gt;</code>, <code>@import</code>)를 모으고, 스타일을 계산하는 {@link StyleResolver} 를 만든다. 현재 DOM 소유자는 문서마다 엔진
 * 하나를 만들고 파싱 중 또는 확정된 문서 큐에서 {@link #loadStyleSheets()} 를 호출하며 레이아웃과 공유한다. 스타일 시트의 출처가 바뀌면({@link #refreshIfNeeded()}) 시트를 다시 모은다.
 */
public final class CssEngine {

    /** The document. / 문서. */
    private final Document document;

    /** The loader of linked and imported sheets, or null. / 링크와 가져오기 시트를 읽는 로더. 없으면 null. */
    private final StyleSheetLoader loader;

    /** The resolver. / 리졸버. */
    private final StyleResolver resolver;

    /** The diagnostics of everything the engine dropped or ignored. / 엔진이 버리거나 무시한 모든 것의 진단. */
    private final CssDiagnostics diagnostics = new CssDiagnostics();

    /** The selector engine. / 선택자 엔진. */
    private final CssSelectorEngine selectors;

    /** The style sheets of the document collected last, in document order (what <code>document.styleSheets</code> lists). / 마지막으로 모은 문서의 스타일 시트(문서 순서. <code>document.styleSheets</code> 가 나열하는 것). */
    private List<StyleSheet> sheets;

    /** The constructed style sheets that the document adopted (<code>document.adoptedStyleSheets</code>), applied after the sheets of the document. / 문서가 채택한 생성 스타일 시트(<code>document.adoptedStyleSheets</code>). 문서의 시트 다음에 적용된다. */
    private List<StyleSheet> adopted = new ArrayList<StyleSheet>();

    /** The sheets that the resolver uses: the sheets of the document and then the adopted ones. / 리졸버가 쓰는 시트: 문서의 시트에 이어 채택한 시트. */
    private List<StyleSheet> combined;

    /** The sheets of the last collection by owner element, so that a sheet whose source did not change stays the same object. / 마지막 수집의 시트(소유 요소별). 출처가 바뀌지 않은 시트가 같은 객체로 남게 한다. */
    private final Map<Element, StyleSheet> owned = new IdentityHashMap<Element, StyleSheet>();

    /** Told after a script changed a sheet (a rule, its media, its disabled flag), or null. / 스크립트가 시트(규칙, 미디어, 비활성 플래그)를 바꾼 뒤 알려 받는 쪽. 없으면 null. */
    private Runnable changeListener;

    /** The receiver that the sheets in use tell about their changes. / 쓰고 있는 시트가 변경을 알리는 받는 쪽. */
    private final Runnable sheetListener = new Runnable() {
        /**
         * A sheet changed: the resolver drops what it computed and the owner of the engine is told.
         * <p>
         * 시트가 바뀌었다: 리졸버가 계산한 것을 버리고 엔진의 소유자에 알린다.
         */
        public void run() {
            sheetChanged();
        }
    };

    /**
     * Creates the engine of a document and installs the selector engine on it. The style sheets are not collected yet.
     * <p>
     * 문서의 엔진을 만들고 그 문서에 선택자 엔진을 설치한다. 스타일 시트는 아직 모으지 않는다.
     *
     * @param document the document / 문서
     * @param loader   loads linked and imported style sheets, or null to skip them / 링크와 가져오기 스타일 시트를 읽는다. 건너뛰려면 null
     * @param env      the media environment / 미디어 환경
     */
    public CssEngine(Document document, StyleSheetLoader loader, MediaEnvironment env) {
        this.document = document;
        this.loader = loader;
        this.selectors = CssSelectorEngine.install(document);
        this.resolver = new StyleResolver(document, env);
        this.resolver.setDiagnostics(diagnostics);
    }

    /**
     * Collects the style sheets of the document and gives them to the resolver. It may block on the loader, so call it on a loading thread.
     * <p>
     * 문서의 스타일 시트를 모아 리졸버에 준다. 로더에서 막힐 수 있으므로 로딩 스레드에서 호출한다.
     */
    public void loadStyleSheets() {
        List<StyleSheet> fresh = DocumentStyleSheets.collect(document, loader, diagnostics, owned);
        // 문서에서 빠진 시트는 더 이상 엔진에 변경을 알리지 않는다.
        for (int i = 0; sheets != null && i < sheets.size(); i++) {
            if (!containsSheet(fresh, sheets.get(i))) {
                sheets.get(i).setChangeListener(null);
            }
        }
        owned.clear();
        // 새 목록의 시트를 소유 요소와 연결하고 변경 통지를 건다.
        for (int i = 0; i < fresh.size(); i++) {
            StyleSheet s = fresh.get(i);
            if (s.getOwner() instanceof Element) {
                owned.put((Element) s.getOwner(), s);
            }
            s.setChangeListener(sheetListener);
        }
        sheets = Collections.unmodifiableList(fresh);
        applySheets();
        resolver.consumeSheetSourceChange();
    }

    /**
     * Tells whether a list holds a sheet object.
     * <p>
     * 목록에 시트 객체가 있는지 알려 준다.
     *
     * @param list  the list / 목록
     * @param sheet the sheet / 시트
     * @return true if the same object is in the list / 같은 객체가 목록에 있으면 true
     */
    private static boolean containsSheet(List<StyleSheet> list, StyleSheet sheet) {
        // 목록을 훑어 같은 객체를 찾는다.
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == sheet) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gives the resolver the sheets of the document followed by the adopted sheets.
     * <p>
     * 리졸버에 문서의 시트와 이어서 채택한 시트를 준다.
     */
    private void applySheets() {
        List<StyleSheet> all = new ArrayList<StyleSheet>(sheets == null ? Collections.<StyleSheet>emptyList() : sheets);
        all.addAll(adopted);
        combined = Collections.unmodifiableList(all);
        resolver.setStyleSheets(all);
    }

    /**
     * Replaces the constructed style sheets that the document adopts (<code>document.adoptedStyleSheets</code>); they apply after the sheets of the
     * document, in the given order, and the styles are computed again.
     * <p>
     * 문서가 채택하는 생성 스타일 시트를 바꾼다(<code>document.adoptedStyleSheets</code>). 문서의 시트 다음에 주어진 순서로 적용되며 스타일을 다시 계산한다.
     *
     * @param list the adopted sheets in order / 순서대로의 채택한 시트
     */
    public void setAdoptedStyleSheets(List<StyleSheet> list) {
        // 더는 채택하지 않는 시트의 통지를 끊고 채택하는 시트에 통지를 건다.
        for (int i = 0; i < adopted.size(); i++) {
            if (!containsSheet(list, adopted.get(i))) {
                adopted.get(i).setChangeListener(null);
            }
        }
        adopted = new ArrayList<StyleSheet>(list);
        // 채택한 시트마다 변경 통지를 건다.
        for (int i = 0; i < adopted.size(); i++) {
            adopted.get(i).setChangeListener(sheetListener);
        }
        applySheets();
        Runnable l = changeListener;
        if (l != null) {
            l.run();
        }
    }

    /**
     * Returns the constructed sheets that the document adopted.
     * <p>
     * 문서가 채택한 생성 시트를 반환한다.
     *
     * @return the sheets in order / 순서대로의 시트
     */
    public List<StyleSheet> getAdoptedStyleSheets() {
        return Collections.unmodifiableList(adopted);
    }

    /**
     * Registers the receiver that is told after a script changed a style sheet that the engine uses (the owner invalidates the layout and the view).
     * <p>
     * 스크립트가 엔진이 쓰는 스타일 시트를 바꾼 뒤 알려 받는 쪽을 등록한다(소유자가 레이아웃과 뷰를 무효화한다).
     *
     * @param l the receiver or null / 받는 쪽 또는 null
     */
    public void setChangeListener(Runnable l) {
        this.changeListener = l;
    }

    /**
     * A style sheet that the engine uses changed: the resolver drops what it computed and the owner is told.
     * <p>
     * 엔진이 쓰는 스타일 시트가 바뀌었다: 리졸버가 계산한 것을 버리고 소유자에 알린다.
     */
    private void sheetChanged() {
        resolver.invalidate();
        Runnable l = changeListener;
        if (l != null) {
            l.run();
        }
    }

    /**
     * Collects the style sheets again if a <code>style</code> or <code>link</code> element changed.
     * <p>
     * <code>style</code> 이나 <code>link</code> 요소가 바뀌었으면 스타일 시트를 다시 모은다.
     *
     * @return true if the sheets were collected again / 다시 모았으면 true
     */
    public boolean refreshIfNeeded() {
        if (resolver.consumeSheetSourceChange()) {
            loadStyleSheets();
            return true;
        }
        return false;
    }

    /**
     * Returns the computed style of an element.
     * <p>
     * 요소의 계산 스타일을 반환한다.
     *
     * @param e the element / 요소
     * @return the style / 스타일
     */
    public ComputedStyle getComputedStyle(Element e) {
        return resolver.getComputedStyle(e);
    }

    /**
     * Returns the resolver.
     * <p>
     * 리졸버를 반환한다.
     *
     * @return the resolver / 리졸버
     */
    public StyleResolver getResolver() {
        return resolver;
    }

    /**
     * Returns the selector engine installed on the document.
     * <p>
     * 문서에 설치된 선택자 엔진을 반환한다.
     *
     * @return the engine / 엔진
     */
    public CssSelectorEngine getSelectorEngine() {
        return selectors;
    }

    /**
     * Returns the diagnostics collected so far.
     * <p>
     * 지금까지 모은 진단을 반환한다.
     *
     * @return the collector / 수집기
     */
    public CssDiagnostics getDiagnostics() {
        return diagnostics;
    }

    /**
     * Returns all the author style sheets that the engine applies: the sheets collected last, followed by the adopted constructed sheets.
     * <p>
     * 엔진이 적용하는 모든 작성자 스타일 시트를 반환한다: 마지막으로 모은 시트, 이어서 채택한 생성 시트.
     *
     * @return the sheets, or null before {@link #loadStyleSheets()} / 시트. {@link #loadStyleSheets()} 전에는 null
     */
    public List<StyleSheet> getStyleSheets() {
        return combined;
    }

    /**
     * Returns the style sheets of the document collected last, in document order, without the adopted sheets (what <code>document.styleSheets</code>
     * lists).
     * <p>
     * 마지막으로 모은 문서의 스타일 시트를 채택한 시트 없이 문서 순서로 반환한다(<code>document.styleSheets</code> 가 나열하는 것).
     *
     * @return the sheets, or null before {@link #loadStyleSheets()} / 시트. {@link #loadStyleSheets()} 전에는 null
     */
    public List<StyleSheet> getDocumentStyleSheets() {
        return sheets;
    }

    /**
     * Releases the resolver (it stops listening to the document). The selector engine stays installed on the document.
     * <p>
     * 리졸버를 해제한다(문서 구독을 멈춘다). 선택자 엔진은 문서에 설치된 채로 남는다.
     */
    public void dispose() {
        // 쓰던 시트가 더 이상 엔진을 붙잡지 않도록 통지를 끊는다.
        for (int i = 0; combined != null && i < combined.size(); i++) {
            combined.get(i).setChangeListener(null);
        }
        owned.clear();
        changeListener = null;
        resolver.dispose();
    }
}
