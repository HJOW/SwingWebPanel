package org.duckdns.hjow.swingwebpanel.css.cssom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.duckdns.hjow.swingwebpanel.css.CssDiagnostics;
import org.duckdns.hjow.swingwebpanel.css.media.MediaQueryList;
import org.duckdns.hjow.swingwebpanel.css.selector.NamespaceMap;
import org.duckdns.hjow.swingwebpanel.dom.DomException;

/**
 * A style sheet: its rules in order, its origin (user agent, user or author), the URL it came from (the base of the relative urls inside it), the
 * media queries that decide whether it applies at all, and a title and a disabled flag as the owning <code>&lt;style&gt;</code> or <code>&lt;link&gt;</code>
 * element gives them. A parsed sheet is the same object that the style resolver reads and that scripts see as a <code>CSSStyleSheet</code> (CSSOM):
 * its rules can be inserted and deleted ({@link #insertRule}, {@link #deleteRule}), the rules and their declaration blocks tell the sheet when they change,
 * and the sheet passes that on to the listener of the CSS engine, which drops what it computed from the sheet. <code>@import</code> rules carry their
 * imported sheets, and an imported sheet knows its importing sheet and rule.
 * <p>
 * 스타일 시트: 순서대로 놓인 규칙, 출처(사용자 에이전트, 사용자, 작성자), 가져온 URL(그 안의 상대 url 의 기준), 시트 전체의 적용 여부를 정하는 미디어 질의,
 * 소유한 <code>&lt;style&gt;</code> 이나 <code>&lt;link&gt;</code> 요소가 주는 제목과 비활성 플래그. 파싱한 시트는 스타일 리졸버가 읽는 객체이자 스크립트가
 * <code>CSSStyleSheet</code>(CSSOM)로 보는 객체와 같다: 규칙을 넣고 지울 수 있고({@link #insertRule}, {@link #deleteRule}), 규칙과 선언 블록이 바뀌면 시트에
 * 알리며, 시트는 그것을 CSS 엔진의 리스너에 전달하여 엔진이 그 시트로 계산한 것을 버리게 한다. <code>@import</code> 규칙은 가져온 시트를 가지며 가져온 시트는
 * 자신을 가져온 시트와 규칙을 안다.
 */
public final class StyleSheet {

    /** Origin: the user agent default style sheet. / 출처: 사용자 에이전트 기본 스타일 시트. */
    public static final int ORIGIN_USER_AGENT = 0;
    /** Origin: a style sheet of the user. / 출처: 사용자의 스타일 시트. */
    public static final int ORIGIN_USER = 1;
    /** Origin: a style sheet of the page author. / 출처: 페이지 작성자의 스타일 시트. */
    public static final int ORIGIN_AUTHOR = 2;

    /** Maximum nesting depth of <code>@import</code>. / <code>@import</code> 의 최대 중첩 깊이. */
    public static final int MAX_IMPORT_DEPTH = 8;

    /** Maximum number of style sheets one root sheet may import. / 루트 시트 하나가 가져올 수 있는 스타일 시트의 최대 수. */
    public static final int MAX_IMPORTS = 64;

    /** The media type of every style sheet that this engine reads (the CSSOM <code>type</code>). / 이 엔진이 읽는 모든 스타일 시트의 미디어 종류(CSSOM <code>type</code>). */
    public static final String MIME_TYPE = "text/css";

    /** The rules (changed through {@link #insertRule} and {@link #deleteRule}). / 규칙({@link #insertRule}, {@link #deleteRule} 로 바뀐다). */
    private final List<CssRule> rules;

    /** The read-only view of {@link #rules} that {@link #getRules()} hands out (it follows the changes). / {@link #getRules()} 가 내주는 {@link #rules} 의 읽기 전용 뷰(변경을 따라간다). */
    private final List<CssRule> rulesView;

    /** The origin. / 출처. */
    private final int origin;

    /** The URL of the sheet, or the document URL for an inline sheet, or null. / 시트의 URL. 인라인 시트이면 문서 URL. 없으면 null. */
    private final String url;

    /** The namespace declarations (replaced when a constructed sheet gets new rules). / 네임스페이스 선언(생성한 시트가 새 규칙을 받으면 바뀐다). */
    private NamespaceMap namespaces;

    /** The media queries of the sheet. / 시트의 미디어 질의. */
    private MediaQueryList media = MediaQueryList.parse("");

    /** The title. / 제목. */
    private String title;

    /** True if the sheet is disabled. / 비활성이면 true. */
    private boolean disabled;

    /** The element that owns the sheet (a style or link element), or null. / 시트를 소유한 요소(style 이나 link 요소). 없으면 null. */
    private Object owner;

    /** The location the sheet was loaded from (the CSSOM <code>href</code>), or null for an inline or constructed sheet. / 시트를 읽은 위치(CSSOM <code>href</code>). 인라인이나 생성한 시트이면 null. */
    private String href;
    /** Whether the loader verified this sheet against the document's origin. / 로더가 이 시트와 문서 오리진의 관계를 확인했는지 여부. */
    private boolean originClean;

    /** The sheet that imported this one, or null. / 이 시트를 가져온 시트. 없으면 null. */
    private StyleSheet parentSheet;

    /** The <code>@import</code> rule that imported this sheet, or null. / 이 시트를 가져온 <code>@import</code> 규칙. 없으면 null. */
    private ImportRule ownerRule;

    /** Told after every change of the sheet or of its rules (set by the CSS engine on the sheets it uses), or null. / 시트나 규칙이 바뀐 뒤마다 알려 받는 쪽(CSS 엔진이 쓰는 시트에 설정). 없으면 null. */
    private Runnable listener;

    /** The loader of <code>@import</code>ed sheets that rules inserted later use, or null. / 나중에 넣은 규칙이 쓰는 <code>@import</code> 시트 로더. 없으면 null. */
    private StyleSheetLoader loader;

    /** True if the sheet was parsed in quirks mode (rules inserted later are parsed the same way). / 시트를 호환 모드로 파싱했으면 true(나중에 넣는 규칙도 같은 방식으로 파싱한다). */
    private boolean quirks;

    /** The text or address that the sheet was made from (the collector reuses the sheet while it does not change), or null. / 시트를 만든 텍스트나 주소(수집기가 이것이 바뀌지 않는 동안 시트를 재사용한다). 없으면 null. */
    private String sourceKey;

    /** True for a sheet created by <code>new CSSStyleSheet()</code>. / <code>new CSSStyleSheet()</code> 로 만든 시트이면 true. */
    private boolean constructed;

    /** True once a script changed the rules or the media of the sheet. / 스크립트가 시트의 규칙이나 미디어를 바꾼 뒤에는 true. */
    private boolean modified;

    /** The <code>media</code> attribute of the owner element that was last applied, or null if none was applied yet. / 마지막으로 적용한 소유 요소의 <code>media</code> 속성. 아직 적용하지 않았으면 null. */
    private String appliedMediaAttribute;

    /** The <code>title</code> attribute of the owner element that was last applied. / 마지막으로 적용한 소유 요소의 <code>title</code> 속성. */
    private String appliedTitleAttribute;

    /** The presence of the <code>disabled</code> attribute of the owner element when it was last applied. / 마지막으로 적용했을 때 소유 요소의 <code>disabled</code> 속성 유무. */
    private boolean appliedDisabledAttribute;

    /** True after the owner attributes were applied once. / 소유 요소의 속성을 한 번 적용한 뒤에는 true. */
    private boolean attributesApplied;

    /** The media list object of the CSSOM, created when it is first asked for. / CSSOM 의 미디어 목록 객체. 처음 요청할 때 만든다. */
    private MediaList mediaList;

    /** The rule list object of the CSSOM, created when it is first asked for. / CSSOM 의 규칙 목록 객체. 처음 요청할 때 만든다. */
    private CssRuleList ruleList;

    /**
     * Creates a sheet (use {@link #parse} instead).
     * <p>
     * 시트를 만든다({@link #parse} 를 대신 쓴다).
     *
     * @param rules      the rules / 규칙
     * @param origin     the origin / 출처
     * @param url        the URL or null / URL 또는 null
     * @param namespaces the namespace declarations / 네임스페이스 선언
     */
    StyleSheet(List<CssRule> rules, int origin, String url, NamespaceMap namespaces) {
        this.rules = new ArrayList<CssRule>(rules);
        this.rulesView = Collections.unmodifiableList(this.rules);
        this.origin = origin;
        this.url = url;
        this.namespaces = namespaces;
        // 규칙마다 이 시트에 속한다고 알려 컨테이너 링크와 변경 통지를 연결한다.
        for (int i = 0; i < this.rules.size(); i++) {
            this.rules.get(i).attach(this, null);
        }
    }

    /**
     * Parses a style sheet.
     * <p>
     * 스타일 시트를 파싱한다.
     *
     * @param text   the CSS text / CSS 문자열
     * @param url    the URL of the sheet (the base for relative urls) or null / 시트의 URL(상대 url 의 기준) 또는 null
     * @param origin the origin constant / 출처 상수
     * @param loader loads <code>@import</code>ed sheets, or null to skip imports / <code>@import</code> 한 시트를 읽는다. null 이면 가져오기를 건너뜀
     * @param diag   collector or null / 수집기 또는 null
     * @return the sheet / 스타일 시트
     */
    public static StyleSheet parse(String text, String url, int origin, StyleSheetLoader loader, CssDiagnostics diag) {
        return parse(text, url, origin, loader, diag, false);
    }

    /**
     * Parses a style sheet, optionally in quirks mode: the declarations (also those of imported sheets) accept unitless lengths and
     * hashless hex colors in the properties that the Quirks Mode Standard names.
     * <p>
     * 스타일 시트를 파싱한다. 호환 모드로 파싱할 수 있다: 선언(가져온 시트의 선언도)이 호환 모드 표준이 정한 속성에서 단위 없는 길이와 # 없는 16진수 색을 받는다.
     *
     * @param text   the CSS text / CSS 문자열
     * @param url    the URL of the sheet (the base for relative urls) or null / 시트의 URL(상대 url 의 기준) 또는 null
     * @param origin the origin constant / 출처 상수
     * @param loader loads <code>@import</code>ed sheets, or null to skip imports / <code>@import</code> 한 시트를 읽는다. null 이면 가져오기를 건너뜀
     * @param diag   collector or null / 수집기 또는 null
     * @param quirks true to parse in quirks mode / 호환 모드로 파싱하면 true
     * @return the sheet / 스타일 시트
     */
    public static StyleSheet parse(String text, String url, int origin, StyleSheetLoader loader, CssDiagnostics diag, boolean quirks) {
        StyleSheet s = new StyleSheetBuilder(url, origin, loader, diag, new ArrayList<String>(), new int[] {0}, quirks).build(text);
        s.loader = loader;
        s.quirks = quirks;
        return s;
    }

    /**
     * Creates an empty sheet that is not part of a document, for <code>new CSSStyleSheet()</code> (constructable style sheets): an author sheet
     * without owner and without <code>href</code>, whose relative urls resolve against a base URL.
     * <p>
     * 문서에 속하지 않는 빈 시트를 만든다(<code>new CSSStyleSheet()</code>, 생성 가능한 스타일 시트): 소유자와 <code>href</code> 가 없는 작성자 시트이며 그 안의
     * 상대 url 은 기준 URL 로 해석한다.
     *
     * @param baseUrl the base URL or null / 기준 URL 또는 null
     * @return the sheet / 시트
     */
    public static StyleSheet createConstructed(String baseUrl) {
        StyleSheet s = new StyleSheet(new ArrayList<CssRule>(), ORIGIN_AUTHOR, baseUrl, new NamespaceMap());
        s.constructed = true;
        return s;
    }

    /**
     * Returns the rules in order. The list is a read-only view that follows later insertions and deletions.
     * <p>
     * 순서대로 규칙을 반환한다. 목록은 이후의 삽입과 삭제를 따라가는 읽기 전용 뷰이다.
     *
     * @return unmodifiable view / 수정할 수 없는 뷰
     */
    public List<CssRule> getRules() {
        return rulesView;
    }

    /**
     * Returns the origin.
     * <p>
     * 출처를 반환한다.
     *
     * @return an origin constant / 출처 상수
     */
    public int getOrigin() {
        return origin;
    }

    /**
     * Returns the URL.
     * <p>
     * URL 을 반환한다.
     *
     * @return the URL or null / URL 또는 null
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns the namespace declarations of the sheet.
     * <p>
     * 시트의 네임스페이스 선언을 반환한다.
     *
     * @return the map / 맵
     */
    public NamespaceMap getNamespaces() {
        return namespaces;
    }

    /**
     * Returns the media queries of the sheet.
     * <p>
     * 시트의 미디어 질의를 반환한다.
     *
     * @return the list, <code>all</code> by default / 목록. 기본은 <code>all</code>
     */
    public MediaQueryList getMedia() {
        return media;
    }

    /**
     * Sets the media queries of the sheet (the style resolver is told).
     * <p>
     * 시트의 미디어 질의를 설정한다(스타일 리졸버에 알린다).
     *
     * @param m the list / 목록
     */
    public void setMedia(MediaQueryList m) {
        this.media = m;
        changed();
    }

    /**
     * Returns the title.
     * <p>
     * 제목을 반환한다.
     *
     * @return the title or null / 제목 또는 null
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title.
     * <p>
     * 제목을 설정한다.
     *
     * @param t the title / 제목
     */
    public void setTitle(String t) {
        this.title = t;
    }

    /**
     * Tells whether the sheet is disabled.
     * <p>
     * 시트가 비활성인지 알려 준다.
     *
     * @return true if disabled / 비활성이면 true
     */
    public boolean isDisabled() {
        return disabled;
    }

    /**
     * Enables or disables the sheet (the style resolver is told and computes the styles again).
     * <p>
     * 시트를 켜거나 끈다(스타일 리졸버에 알리며 스타일을 다시 계산한다).
     *
     * @param d true to disable / 끄려면 true
     */
    public void setDisabled(boolean d) {
        boolean was = this.disabled;
        this.disabled = d;
        if (was != d) {
            changed();
        }
    }

    /**
     * Returns the owner element.
     * <p>
     * 소유 요소를 반환한다.
     *
     * @return the element or null / 요소 또는 null
     */
    public Object getOwner() {
        return owner;
    }

    /**
     * Returns the owner element (the CSSOM <code>ownerNode</code>); an imported or constructed sheet has none.
     * <p>
     * 소유 요소를 반환한다(CSSOM <code>ownerNode</code>). 가져온 시트나 생성한 시트에는 없다.
     *
     * @return the element or null / 요소 또는 null
     */
    public Object getOwnerNode() {
        return owner;
    }

    /**
     * Sets the owner element.
     * <p>
     * 소유 요소를 설정한다.
     *
     * @param o the element / 요소
     */
    public void setOwner(Object o) {
        this.owner = o;
    }

    /**
     * Returns the media type of the sheet (the CSSOM <code>type</code>), always <code>text/css</code>.
     * <p>
     * 시트의 미디어 종류를 반환한다(CSSOM <code>type</code>). 항상 <code>text/css</code>.
     *
     * @return <code>text/css</code> / <code>text/css</code>
     */
    public String getMimeType() {
        return MIME_TYPE;
    }

    /**
     * Returns the location that the sheet was loaded from (the CSSOM <code>href</code>).
     * <p>
     * 시트를 읽은 위치를 반환한다(CSSOM <code>href</code>).
     *
     * @return the absolute URL, or null for an inline or constructed sheet / 절대 URL. 인라인이나 생성한 시트이면 null
     */
    public String getHref() {
        return href;
    }

    /**
     * Sets the location that the sheet was loaded from.
     * <p>
     * 시트를 읽은 위치를 설정한다.
     *
     * @param h the absolute URL or null / 절대 URL 또는 null
     */
    public void setHref(String h) {
        this.href = h;
    }

    /** Returns the explicit origin access granted when this sheet was read. / 시트를 읽을 때 명시적으로 허용한 오리진 접근을 반환한다. */
    public boolean isOriginClean() {
        return originClean;
    }

    /**
     * Records the loader's origin access decision for use by CSSOM.
     * <p>
     * CSSOM에서 사용할 로더의 오리진 접근 판정을 기록한다.
     *
     * @param clean true for a confirmed same-origin sheet / 동일 오리진으로 확인한 시트이면 true
     */
    void setOriginClean(boolean clean) {
        originClean = clean;
    }

    /**
     * Returns the sheet that imported this sheet (the CSSOM <code>parentStyleSheet</code>).
     * <p>
     * 이 시트를 가져온 시트를 반환한다(CSSOM <code>parentStyleSheet</code>).
     *
     * @return the importing sheet or null / 가져온 시트 또는 null
     */
    public StyleSheet getParentStyleSheet() {
        return parentSheet;
    }

    /**
     * Returns the <code>@import</code> rule that imported this sheet (the CSSOM <code>ownerRule</code>).
     * <p>
     * 이 시트를 가져온 <code>@import</code> 규칙을 반환한다(CSSOM <code>ownerRule</code>).
     *
     * @return the rule or null / 규칙 또는 null
     */
    public ImportRule getOwnerRule() {
        return ownerRule;
    }

    /**
     * Records that an <code>@import</code> rule of another sheet imports this sheet.
     * <p>
     * 다른 시트의 <code>@import</code> 규칙이 이 시트를 가져온다고 기록한다.
     *
     * @param importing the sheet that contains the rule, or null to clear the link / 규칙을 담은 시트. 링크를 지우려면 null
     * @param rule      the rule or null / 규칙 또는 null
     */
    void setImporter(StyleSheet importing, ImportRule rule) {
        this.parentSheet = importing;
        this.ownerRule = rule;
    }

    /**
     * Tells whether the sheet was parsed in quirks mode.
     * <p>
     * 시트를 호환 모드로 파싱했는지 알려 준다.
     *
     * @return true for quirks mode / 호환 모드이면 true
     */
    public boolean isQuirks() {
        return quirks;
    }

    /**
     * Returns the loader that <code>@import</code> rules inserted into this sheet later use.
     * <p>
     * 이 시트에 나중에 넣는 <code>@import</code> 규칙이 쓰는 로더를 반환한다.
     *
     * @return the loader or null / 로더 또는 null
     */
    public StyleSheetLoader getLoader() {
        return loader;
    }

    /**
     * Replaces the loader that <code>@import</code> rules inserted later use (the collector gives a reused sheet the loader of the current scope).
     * <p>
     * 나중에 넣는 <code>@import</code> 규칙이 쓰는 로더를 바꾼다(수집기가 재사용하는 시트에 현재 범위의 로더를 준다).
     *
     * @param l the loader or null / 로더 또는 null
     */
    public void setLoader(StyleSheetLoader l) {
        this.loader = l;
    }

    /**
     * Returns the text or address that the sheet was made from.
     * <p>
     * 시트를 만든 텍스트나 주소를 반환한다.
     *
     * @return the key or null / 키 또는 null
     */
    public String getSourceKey() {
        return sourceKey;
    }

    /**
     * Records the text or address that the sheet was made from; the collector compares it with the current source to decide whether the sheet can be
     * reused.
     * <p>
     * 시트를 만든 텍스트나 주소를 기록한다. 수집기가 현재의 출처와 비교하여 시트를 재사용할 수 있는지 판단한다.
     *
     * @param key the key or null / 키 또는 null
     */
    public void setSourceKey(String key) {
        this.sourceKey = key;
    }

    /**
     * Tells whether the sheet was created by <code>new CSSStyleSheet()</code>.
     * <p>
     * 시트가 <code>new CSSStyleSheet()</code> 로 만든 것인지 알려 준다.
     *
     * @return true for a constructed sheet / 생성한 시트이면 true
     */
    public boolean isConstructed() {
        return constructed;
    }

    /**
     * Tells whether a script changed the rules or the media of the sheet since it was parsed.
     * <p>
     * 파싱한 뒤 스크립트가 시트의 규칙이나 미디어를 바꿨는지 알려 준다.
     *
     * @return true if the sheet was modified / 바꿨으면 true
     */
    public boolean isModified() {
        return modified;
    }

    /**
     * Marks the sheet as changed by a script.
     * <p>
     * 시트를 스크립트가 바꿨다고 표시한다.
     */
    void markModified() {
        this.modified = true;
    }

    /**
     * Applies the <code>media</code>, <code>title</code> and <code>disabled</code> attributes of the owner element, but only the ones that differ from
     * what was applied last: a script that changed the media or the disabled flag of the sheet keeps its change until the attribute changes.
     * <p>
     * 소유 요소의 <code>media</code>, <code>title</code>, <code>disabled</code> 속성을 적용하되, 마지막으로 적용한 것과 다른 것만 적용한다: 스크립트가 시트의 미디어나
     * 비활성 플래그를 바꿨으면 속성이 바뀔 때까지 그 변경을 유지한다.
     *
     * @param mediaAttribute    the <code>media</code> attribute or null / <code>media</code> 속성 또는 null
     * @param titleAttribute    the <code>title</code> attribute or null / <code>title</code> 속성 또는 null
     * @param disabledAttribute true if the element has the <code>disabled</code> attribute / 요소에 <code>disabled</code> 속성이 있으면 true
     */
    public void applyOwnerAttributes(String mediaAttribute, String titleAttribute, boolean disabledAttribute) {
        String m = mediaAttribute == null ? "" : mediaAttribute;
        // 처음이거나 속성이 바뀐 것만 시트에 반영한다.
        if (!attributesApplied || !m.equals(appliedMediaAttribute)) {
            this.media = MediaQueryList.parse(m);
            this.appliedMediaAttribute = m;
        }
        if (!attributesApplied || (titleAttribute == null ? appliedTitleAttribute != null : !titleAttribute.equals(appliedTitleAttribute))) {
            this.title = titleAttribute;
            this.appliedTitleAttribute = titleAttribute;
        }
        if (!attributesApplied || disabledAttribute != appliedDisabledAttribute) {
            this.disabled = disabledAttribute;
            this.appliedDisabledAttribute = disabledAttribute;
        }
        this.attributesApplied = true;
    }

    /**
     * Returns the media list object of the CSSOM (the same object every time).
     * <p>
     * CSSOM 의 미디어 목록 객체를 반환한다(매번 같은 객체).
     *
     * @return the media list / 미디어 목록
     */
    public MediaList getMediaList() {
        if (mediaList == null) {
            mediaList = new MediaList(this);
        }
        return mediaList;
    }

    /**
     * Returns the rule list object of the CSSOM, <code>cssRules</code> (the same object every time, following later changes).
     * <p>
     * CSSOM 의 규칙 목록 객체 <code>cssRules</code> 를 반환한다(매번 같은 객체이며 이후의 변경을 따라간다).
     *
     * @return the rule list / 규칙 목록
     */
    public CssRuleList getCssRules() {
        if (ruleList == null) {
            ruleList = new CssRuleList(rulesView);
        }
        return ruleList;
    }

    /**
     * Registers the receiver that is told after every change of the sheet or of one of its rules (the CSS engine does it for the sheets it uses).
     * <p>
     * 시트나 그 규칙이 바뀐 뒤마다 알려 받는 쪽을 등록한다(CSS 엔진이 자신이 쓰는 시트에 한다).
     *
     * @param l the receiver or null / 받는 쪽 또는 null
     */
    public void setChangeListener(Runnable l) {
        this.listener = l;
    }

    /**
     * Tells the receiver of the root sheet that something in this sheet changed (an imported sheet reports through the sheets that import it).
     * <p>
     * 이 시트 안의 무언가가 바뀌었다고 루트 시트의 받는 쪽에 알린다(가져온 시트는 자신을 가져온 시트를 거쳐 보고한다).
     */
    void changed() {
        StyleSheet s = this;
        // 가져온 시트이면 위로 올라가 리스너가 있는 시트에 알린다.
        for (int depth = 0; s != null && depth <= MAX_IMPORT_DEPTH + 1; depth++) {
            if (s.listener != null) {
                s.listener.run();
                return;
            }
            s = s.parentSheet;
        }
    }

    /**
     * Inserts a rule given as text (CSSOM <code>insertRule</code>).
     * <p>
     * 텍스트로 주어진 규칙을 넣는다(CSSOM <code>insertRule</code>).
     *
     * @param text  one rule, for example <code>a { color: red }</code> / 규칙 하나. 예: <code>a { color: red }</code>
     * @param index the position, from 0 to the number of rules / 위치. 0 부터 규칙 수까지
     * @return the position of the new rule / 새 규칙의 위치
     * @throws DomException <code>IndexSizeError</code>, <code>SyntaxError</code>, <code>HierarchyRequestError</code> or <code>InvalidStateError</code>
     *                      as the CSSOM defines / CSSOM 이 정한 <code>IndexSizeError</code>, <code>SyntaxError</code>, <code>HierarchyRequestError</code>, <code>InvalidStateError</code>
     */
    public int insertRule(String text, int index) {
        return RuleEditor.insert(this, null, rules, text, index);
    }

    /**
     * Deletes the rule at an index (CSSOM <code>deleteRule</code>).
     * <p>
     * 위치의 규칙을 지운다(CSSOM <code>deleteRule</code>).
     *
     * @param index the position / 위치
     * @throws DomException <code>IndexSizeError</code> or <code>InvalidStateError</code> as the CSSOM defines / CSSOM 이 정한 <code>IndexSizeError</code> 또는 <code>InvalidStateError</code>
     */
    public void deleteRule(int index) {
        RuleEditor.delete(this, rules, index);
    }

    /**
     * The legacy <code>addRule(selector, style, index)</code>: inserts <code>selector { style }</code> at the position (the end if the index is negative) and
     * returns -1.
     * <p>
     * 옛 <code>addRule(selector, style, index)</code>: <code>selector { style }</code> 을 위치에 넣고(음수이면 끝) -1 을 반환한다.
     *
     * @param selector the selector text / 선택자 문자열
     * @param style    the declarations / 선언
     * @param index    the position, or a negative number for the end / 위치. 끝이면 음수
     * @return -1 / -1
     * @throws DomException as {@link #insertRule} / {@link #insertRule} 와 같다
     */
    public int addRule(String selector, String style, int index) {
        insertRule(selector + " { " + style + " }", index < 0 ? rules.size() : index);
        return -1;
    }

    /**
     * Replaces all the rules of a constructed sheet by the rules of a text (CSSOM <code>replaceSync</code>): <code>@import</code> rules are ignored.
     * <p>
     * 생성한 시트의 모든 규칙을 텍스트의 규칙으로 바꾼다(CSSOM <code>replaceSync</code>). <code>@import</code> 규칙은 무시한다.
     *
     * @param text the style sheet text / 스타일 시트 텍스트
     */
    public void replaceRules(String text) {
        StyleSheet parsed = new StyleSheetBuilder(url, origin, null, null, new ArrayList<String>(), new int[] {0}, new NamespaceMap(), quirks).build(text == null ? "" : text);
        this.namespaces = parsed.namespaces;
        List<CssRule> fresh = new ArrayList<CssRule>(parsed.rules);
        // 이전 규칙은 시트에서 떼고 새 규칙을 붙인다.
        for (int i = 0; i < rules.size(); i++) {
            rules.get(i).detach();
        }
        rules.clear();
        // 가져오기 규칙은 생성한 시트에서 무시한다.
        for (int i = 0; i < fresh.size(); i++) {
            if (!(fresh.get(i) instanceof ImportRule)) {
                rules.add(fresh.get(i));
                fresh.get(i).attach(this, null);
            }
        }
        modified = true;
        changed();
    }

    /**
     * Loads the <code>@import</code> rules that could not be loaded when the sheet was parsed (the resource was still arriving): a rule whose sheet is
     * still missing asks the loader again and, if the sheet is there now, gets it, so that the sheet object (and the changes a script made to it) stays
     * the same while its imports arrive.
     * <p>
     * 시트를 파싱할 때 읽지 못한(리소스가 아직 오는 중이던) <code>@import</code> 규칙을 읽는다: 시트가 아직 없는 규칙은 로더에 다시 물어 시트가 이제 있으면 받는다.
     * 그러므로 가져오기가 도착하는 동안에도 시트 객체(와 스크립트가 거기 한 변경)는 그대로이다.
     *
     * @param l    the loader of the current scope, or null to use the loader the sheet has / 현재 범위의 로더. null 이면 시트가 가진 로더
     * @param diag collector or null / 수집기 또는 null
     * @return true if an import was loaded now / 이번에 가져오기를 읽었으면 true
     */
    public boolean resolvePendingImports(StyleSheetLoader l, CssDiagnostics diag) {
        if (l != null) {
            this.loader = l;
        }
        return StyleSheetBuilder.resolveImports(this, diag);
    }

    /**
     * Tells whether the sheet has an <code>@import</code> rule that was not loaded (yet), directly or in an imported sheet.
     * <p>
     * 시트에 (아직) 읽지 못한 <code>@import</code> 규칙이 있는지(직접 또는 가져온 시트 안에) 알려 준다.
     *
     * @return true if an import is missing / 가져오기가 빠져 있으면 true
     */
    public boolean hasPendingImports() {
        // 최상위 가져오기 규칙을 훑어 시트가 없는 것이 있으면 true, 가져온 시트도 같은 검사를 한다.
        for (int i = 0; i < rules.size(); i++) {
            CssRule r = rules.get(i);
            if (r instanceof ImportRule) {
                ImportRule ir = (ImportRule) r;
                if (ir.getStyleSheet() == null ? ir.getResolvedUrl() != null : ir.getStyleSheet().hasPendingImports()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Counts the style rules of the sheet, including the ones in groups and imports.
     * <p>
     * 그룹과 가져온 시트를 포함해 시트의 스타일 규칙 수를 센다.
     *
     * @return the number of style rules / 스타일 규칙 수
     */
    public int countStyleRules() {
        return count(rules);
    }

    /**
     * Counts style rules in a rule list.
     * <p>
     * 규칙 목록의 스타일 규칙 수를 센다.
     *
     * @param list the rules / 규칙
     * @return the count / 개수
     */
    private static int count(List<CssRule> list) {
        int n = 0;
        // 규칙 종류에 따라 스타일 규칙이면 하나, 그룹이면 안쪽, 가져오기이면 가져온 시트를 센다.
        for (int i = 0; i < list.size(); i++) {
            CssRule r = list.get(i);
            if (r instanceof StyleRule) {
                n++;
            } else if (r instanceof GroupRule) {
                n += count(((GroupRule) r).getRules());
            } else if (r instanceof ImportRule && ((ImportRule) r).getStyleSheet() != null) {
                n += ((ImportRule) r).getStyleSheet().countStyleRules();
            }
        }
        return n;
    }

    /**
     * Serializes the sheet.
     * <p>
     * 시트를 직렬화한다.
     *
     * @return the CSS text of all rules / 모든 규칙의 CSS 문자열
     */
    public String toCssText() {
        StringBuilder sb = new StringBuilder();
        // 규칙을 한 줄씩 이어 쓴다.
        for (int i = 0; i < rules.size(); i++) {
            sb.append(rules.get(i).getCssText()).append('\n');
        }
        return sb.toString();
    }
}
