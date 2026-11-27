package org.duckdns.hjow.swingwebpanel.core;

import java.awt.Point;
import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.css.selector.SimpleElementState;
import org.duckdns.hjow.swingwebpanel.css.anim.AnimationClock;
import org.duckdns.hjow.swingwebpanel.css.anim.AnimationEngine;
import org.duckdns.hjow.swingwebpanel.css.cascade.StyleResolver;
import org.duckdns.hjow.swingwebpanel.css.cssom.ContextualStyleSheetLoader;
import org.duckdns.hjow.swingwebpanel.css.cssom.CssRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.FontFaceRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.GroupRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.StyleSheet;
import org.duckdns.hjow.swingwebpanel.layout.WebFontSet;
import org.duckdns.hjow.swingwebpanel.css.cssom.LoadedStyleSheet;
import org.duckdns.hjow.swingwebpanel.css.cssom.StyleSheetLoader;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.css.media.MediaQueryList;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DomMutationListener;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.MutationRecord;
import org.duckdns.hjow.swingwebpanel.js.host.MediaRequestSource;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.net.CookieStore;
import org.duckdns.hjow.swingwebpanel.net.CrossOriginPolicy;
import org.duckdns.hjow.swingwebpanel.net.CancelToken;
import org.duckdns.hjow.swingwebpanel.net.ResourceFetcher;
import org.duckdns.hjow.swingwebpanel.render.PageRenderer;
import org.duckdns.hjow.swingwebpanel.render.RenderedPage;

/**
 * DOM/CSS/resource/layout ownership for a page, from parsing through confirmed drawing. Only the parser accesses the DOM
 * before parsingFinished; afterwards the session document queue owns it. Background completions only mark invalidation.
 * Retirement cancels resources immediately and releases observers after parser ownership returns, outside the EDT.
 * <p>파싱부터 확정 후 그리기까지의 페이지 DOM·CSS·리소스·배치 소유권. parsingFinished 전에는 파서만 DOM에 접근하고 이후에는 세션 문서 큐가
 * 소유한다. 백그라운드 완료는 무효화만 표시한다. 폐기는 리소스를 즉시 취소하고 파서 소유권 반환 뒤 EDT 밖에서 관찰자를 해제한다.
 */
final class PagePresentation implements DomMutationListener, PageResources.Listener, CancelToken.Hook {
    /** DOM retained until observer cleanup. / 관찰자 해제까지 보관하는 DOM. */
    private Document document;
    /** Single resource state shared across ownership handoff. / 소유권 인계 전후 공유하는 단일 리소스 상태. */
    private final PageResources resources;
    /** The parent embedder policy for frames of this document. / 이 문서의 프레임에 적용할 부모 삽입 정책. */
    private final CrossOriginPolicy.Embedder embedderPolicy;
    /** Parser ownership barrier. / 파서 소유권 장벽. */
    private final CountDownLatch parsed = new CountDownLatch(1);
    /** Resource-driven stylesheet recollection. / 리소스로 인한 스타일시트 재수집 표시. */
    private final AtomicBoolean sheetsChanged = new AtomicBoolean(true);
    /** Connected mutations and resource completions invalidate layout. / 연결 변경과 리소스 완료가 배치를 무효화한다. */
    private final AtomicBoolean dirty = new AtomicBoolean(true);
    /** Ensures retirement is submitted exactly once. / 폐기를 정확히 한 번 제출하는 표시. */
    private final AtomicBoolean closed = new AtomicBoolean();
    /** View scheduling callback, installed only after confirmation. / 확정 뒤에만 설치하는 뷰 예약 콜백. */
    private volatile Runnable invalidation;
    /** Receiver that is told after each ended resource (the script host of the page), or null. / 리소스가 끝날 때마다 알려 받는 쪽(페이지의 스크립트 호스트). 없으면 null. */
    private volatile Runnable resourceObserver;
    /** True until parser ownership returns. / 파서 소유권 반환 전까지 true. */
    private volatile boolean parsing = true;
    /** Navigation token without a request/history reference. / 이동 요청·이력 참조 없는 이동 토큰. */
    private CancelToken parent;
    /** CSS engine confined to the owning thread and cleanup lock. / 소유 스레드와 해제 잠금 전용 CSS 엔진. */
    private CssEngine css;
    /** The transitions and animations of the page (P10-07), created with the style engine. 페이지의 전환과 애니메이션(P10-07). 스타일 엔진과 함께 만든다. */
    private AnimationEngine animations;
    /** The timer of the animation frames and of the ends of web font block periods, created when it is first needed. 애니메이션 프레임과 웹 글꼴 block 기간 끝의 타이머. 처음 필요할 때 만든다. */
    private Timer frameTimer;
    /** The end of a web font block period (milliseconds of the system clock) for which a new rendering is scheduled, 0 if none. 새 렌더링을 예약한 웹 글꼴 block 기간의 끝(시스템 시계 ms). 없으면 0. */
    private long blockWake;
    /** True while a frame is scheduled. 프레임이 예약되어 있는 동안 true. */
    private final AtomicBoolean frameQueued = new AtomicBoolean();
    /** The delay between two animation frames in milliseconds (about 60 frames a second). 애니메이션 프레임 사이의 지연(밀리초, 초당 약 60 프레임). */
    private static final long FRAME_MILLIS = 16;
    /** Viewport environment used by the engine. / 엔진이 쓰는 뷰포트 환경. */
    private MediaEnvironment environment;
    /** Parser-created sheets enabled for the initial media environment. / 초기 미디어 환경에서 활성화된 파서 생성 시트. */
    private final Map<Element, Boolean> parserSheets = new IdentityHashMap<Element, Boolean>();
    /** Temporary blocking mode used only before a parser script executes. / 파서 스크립트 실행 전만 쓰는 임시 차단 모드. */
    private boolean waitingForStyles;
    /** Script cancellation for a temporary stylesheet wait. / 임시 스타일시트 대기용 스크립트 취소. */
    private CancelToken styleWaiter;
    /** Last layout result, guarded by this object. / 이 객체로 보호하는 마지막 배치 결과. */
    private RenderedPage rendered;
    /** Width of the retained layout. / 보관한 배치의 폭. */
    private int layoutWidth;
    /** Height of the retained layout. / 보관한 배치의 높이. */
    private int layoutHeight;

    /** Creates the presentation on the loader thread before parsing. / 파싱 전 로더 스레드에서 표현 상태를 만든다.
     * @param document unshared DOM / 독립 DOM
     * @param context copied resource settings / 복사한 리소스 설정
     * @param fetcher shared fetcher / 공용 가져오기 객체
     * @param cookies component cookies / 컴포넌트 쿠키
     * @param workers resource executor / 리소스 실행기 */
    PagePresentation(Document document, PageRequestContext context, ResourceFetcher fetcher, CookieStore cookies, Executor workers) {
        this.document = document;
        embedderPolicy = context.crossOriginPolicy.getEmbedder();
        resources = new PageResources(fetcher, cookies, document.getOriginUrl(), context, workers, this);
        fonts = new WebFontSet(new WebFontSet.TimedLoader() {
            /**
             * Tells when the request of a web font started and when its bytes arrived (for <code>font-display</code>).
             * <p>
             * 웹 글꼴 요청이 언제 시작되었고 바이트가 언제 도착했는지 알려 준다(<code>font-display</code> 용).
             *
             * @param absoluteUrl the font address / 글꼴 주소
             * @return {start, arrival} in ms / {시작, 도착}(ms)
             */
            public long[] times(String absoluteUrl) {
                return resources.fontTimes(absoluteUrl);
            }

            /**
             * Reads a web font through the resources of the page.
             * <p>
             * 페이지의 리소스로 웹 글꼴을 읽는다.
             *
             * @param absoluteUrl the font address / 글꼴 주소
             * @return the bytes or null while the request runs / 바이트. 요청이 진행 중이면 null
             * @throws IOException if the request failed / 요청이 실패하면
             */
            public byte[] load(String absoluteUrl) throws IOException {
                return resources.fontBytes(absoluteUrl);
            }
        });
        document.addMutationListener(this);
    }

    /** The web fonts (<code>@font-face</code>) of the page (P10-05). 페이지의 웹 글꼴(<code>@font-face</code>, P10-05). */
    private final WebFontSet fonts;

    /**
     * Collects the <code>@font-face</code> rules of the style sheets of the page (those inside a media rule only if it applies).
     * <p>
     * 페이지 스타일 시트의 <code>@font-face</code> 규칙을 모은다(미디어 규칙 안의 것은 적용될 때만).
     *
     * @return the rules in the order of the style sheets / 스타일 시트 순서의 규칙
     */
    private List<FontFaceRule> fontFaceRules() {
        List<FontFaceRule> out = new java.util.ArrayList<FontFaceRule>();
        // 스타일 시트마다(사용 안 함 표시가 아닌 것) 규칙을 훑는다.
        for (StyleSheet sheet : css.getStyleSheets()) {
            if (!sheet.isDisabled()) {
                collectFontFaces(sheet.getRules(), out);
            }
        }
        return out;
    }

    /**
     * Adds the <code>@font-face</code> rules of a rule list (and of the groups that apply) to a list.
     * <p>
     * 규칙 목록(과 적용되는 그룹)의 <code>@font-face</code> 규칙을 목록에 더한다.
     *
     * @param rules the rules / 규칙
     * @param out   receives the font face rules / 글꼴 규칙을 받는다
     */
    private void collectFontFaces(List<CssRule> rules, List<FontFaceRule> out) {
        // 규칙마다 글꼴 규칙이면 더하고 적용되는 그룹이면 그 안을 훑는다.
        for (CssRule r : rules) {
            if (r instanceof FontFaceRule) {
                out.add((FontFaceRule) r);
            } else if (r instanceof GroupRule && ((GroupRule) r).applies(environment)) {
                collectFontFaces(((GroupRule) r).getRules(), out);
            }
        }
    }

    /** The dynamic state of the elements for <code>:hover</code>, <code>:active</code>, <code>:focus</code> and <code>:target</code>. / <code>:hover</code>, <code>:active</code>, <code>:focus</code>, <code>:target</code> 를 위한 요소의 동적 상태. */
    private final SimpleElementState elementState = new SimpleElementState();

    /** Returns the dynamic element state that the style engine consults; change it only on the document queue and then call {@link #elementStateChanged()}. / 스타일 엔진이 조회하는 동적 요소 상태를 반환한다. 문서 큐에서만 바꾸고 {@link #elementStateChanged()} 를 부른다.
     * @return the state / 상태 */
    SimpleElementState elementState() {
        return elementState;
    }

    /** Tells the style engine that the dynamic state of the elements changed, so that the affected styles are computed again and the view is redrawn. / 요소의 동적 상태가 바뀌었음을 스타일 엔진에 알려 영향받는 스타일을 다시 계산하고 뷰를 다시 그리게 한다. */
    synchronized void elementStateChanged() {
        if (css != null) {
            css.getResolver().stateChanged(null);
        }
        invalidate();
    }

    /** Hands ownership from the loader to the document queue on success or abandonment. / 성공·포기 모두에서 로더 소유권을 문서 큐로 인계한다. */
    void parsingFinished() {
        parsing = false;
        sheetsChanged.set(true);
        dirty.set(true);
        parsed.countDown();
    }

    /** Binds navigation cancellation after the script host registered its cancellation hook. / 스크립트 호스트 취소 훅 등록 뒤 이동 취소를 연결한다.
     * @param token owning navigation / 소유 이동 */
    void bindCancellation(CancelToken token) {
        parent = token;
        token.addHook(this);
        if (closed.get()) {
            token.removeHook(this);
        }
    }

    /** Retires resources when the owning navigation is cancelled. / 소유 이동 취소 시 리소스를 폐기한다. */
    public void onCancel() {
        retire();
    }

    /** Returns whether queries still belong to the parser. / 조회가 아직 파서 소유인지 반환한다.
     * @return parser ownership flag / 파서 소유권 표시 */
    boolean isParsing() {
        return parsing;
    }

    /** Installs the confirmed view callback without touching the DOM. / DOM 접근 없이 확정된 뷰 콜백을 설치한다.
     * @param callback scheduling action / 예약 동작 */
    void setInvalidation(Runnable callback) {
        invalidation = callback;
    }

    /** Returns the current resolver, including changes made during parsing. / 파싱 중 변경도 포함한 현재 리졸버를 반환한다.
     * @param width viewport width / 뷰포트 폭
     * @param height viewport height / 뷰포트 높이
     * @return resolver, or null when retired / 리졸버이며 폐기 후에는 null */
    synchronized StyleResolver styleResolver(int width, int height) {
        if (closed.get()) {
            return null;
        }
        updateStyles(width, height);
        return css.getResolver();
    }

    /** Returns the CSS engine with the pending changes of the document applied; the CSSOM of the page reads and edits the style sheets of this engine. / 문서의 대기 중인 변경을 적용한 CSS 엔진을 반환한다. 페이지의 CSSOM 이 이 엔진의 스타일 시트를 읽고 고친다.
     * @param width viewport width / 뷰포트 폭
     * @param height viewport height / 뷰포트 높이
     * @return the engine, or null when retired / 엔진이며 폐기 후에는 null */
    synchronized CssEngine cssEngine(int width, int height) {
        if (closed.get()) {
            return null;
        }
        updateStyles(width, height);
        return css;
    }

    /** Starts loading a parser-created sheet and remembers its initial blocking eligibility. / 파서 생성 시트 로딩을 시작하고 초기 차단 조건을 기억한다.
     * @param element parser-created style or link / 파서가 만든 style 또는 link
     * @param width viewport width / 뷰포트 폭
     * @param height viewport height / 뷰포트 높이 */
    synchronized void parserStyleSheet(Element element, int width, int height) {
        if (closed.get()) {
            return;
        }
        MediaEnvironment initial = new MediaEnvironment(width, height);
        initial.setScripting(document.isScriptingEnabled());
        String type = element.getAttribute("type");
        boolean cssType = type == null || type.trim().length() == 0 || "text/css".equalsIgnoreCase(type.trim());
        String rel = element.getAttribute("rel");
        boolean alternate = "link".equals(element.getLocalName()) && rel != null
                && (" " + rel.toLowerCase(java.util.Locale.ENGLISH).replaceAll("\\s+", " ") + " ").indexOf(" alternate ") >= 0;
        parserSheets.put(element, Boolean.valueOf(cssType && !alternate && !element.hasAttribute("disabled")
                && MediaQueryList.parse(element.getAttribute("media")).matches(initial)));
        sheetsChanged.set(true);
        updateStyles(width, height);
    }

    /** Waits for applicable preceding parser sheets before executing a classic parser/deferred script. / 일반 파서·defer 스크립트 실행 전에 적용 가능한 앞선 파서 시트를 기다린다.
     * @param waiter script cancellation / 스크립트 취소
     * @param width viewport width / 뷰포트 폭
     * @param height viewport height / 뷰포트 높이 */
    synchronized void prepareParserScript(CancelToken waiter, int width, int height) {
        if (closed.get() || !parsing || waiter.isCancelled()) {
            return;
        }
        waitingForStyles = true;
        styleWaiter = waiter;
        try {
            sheetsChanged.set(true);
            dirty.set(true);
            updateStyles(width, height);
        } finally {
            waitingForStyles = false;
            styleWaiter = null;
        }
    }

    /** Updates styles on the current DOM owner thread. / 현재 DOM 소유 스레드에서 스타일을 갱신한다.
     * @param width viewport width / 뷰포트 폭
     * @param height viewport height / 뷰포트 높이 */
    private void updateStyles(int width, int height) {
        if (css == null) {
            sheetsChanged.set(false);
            environment = new MediaEnvironment(width, height);
            environment.setScripting(document.isScriptingEnabled());
            css = new CssEngine(document, new SheetLoader(true), environment);
            css.getResolver().setElementState(elementState);
            // 스크립트(CSSOM)가 규칙, 미디어, 비활성 플래그를 바꾸면 레이아웃을 무효화하고 뷰에 다시 그리게 한다.
            css.setChangeListener(new Runnable() {
                /** Invalidates the layout after a style sheet changed. / 스타일 시트가 바뀐 뒤 레이아웃을 무효화한다. */
                public void run() {
                    invalidate();
                }
            });
            // CSS 전환과 애니메이션은 JavaScript 설정과 무관하게 동작한다(P10-07): 엔진은 스타일 리졸버에 설치된다.
            animations = new AnimationEngine(css, AnimationClock.SYSTEM);
            applyFontDefaults();
            css.loadStyleSheets();
        } else {
            applyFontDefaults();
            environment.setViewport(width, height);
            environment.setScripting(document.isScriptingEnabled());
            css.getResolver().setMediaEnvironment(environment);
            if (sheetsChanged.getAndSet(false)) {
                css.loadStyleSheets();
            } else {
                css.refreshIfNeeded();
            }
        }
    }

    /** Renders dirty state synchronously on the current owner, without publishing Swing changes. / 현재 소유자에서 변경 상태를 동기 배치하되 Swing 변경은 게시하지 않는다.
     * @param width viewport width / 뷰포트 폭
     * @param height viewport height / 뷰포트 높이
     * @return immutable rendering, or null after retirement / 불변 그림이며 폐기 후에는 null */
    synchronized RenderedPage render(int width, int height) {
        if (closed.get()) {
            return null;
        }
        boolean changed = dirty.getAndSet(false);
        boolean repaint = paintDirty.getAndSet(false);
        // 전환과 애니메이션의 시간을 진행한다: 값이 움직이면 다시 배치한다(문서 스레드에서 이벤트도 발생한다).
        boolean animated = animations != null && animations.tick();
        if (rendered == null || changed || animated || width != layoutWidth || height != layoutHeight) {
            updateStyles(width, height);
            // @font-face 규칙을 갱신한다(읽힌 글꼴은 유지된다). 글꼴이 도착하면 resourceChanged 가 다시 렌더링을 요청한다.
            fonts.setRules(fontFaceRules());
            rendered = PageRenderer.render(document, css, resources, width, height, fonts);
            // 웹 글꼴의 block 기간 때문에 보이지 않게 배치한 텍스트가 있으면 기간이 끝날 때 다시 렌더링한다(CSS Fonts 4 4.8).
            scheduleBlockEnd(fonts.takeBlockDeadline());
            layoutWidth = width;
            layoutHeight = height;
        } else if (repaint) {
            // 캔버스 그림만 바뀐 경우에는 배치를 그대로 두고 디스플레이 리스트만 다시 만든다.
            rendered = PageRenderer.fromLayout(rendered.getLayout(), resources);
        }
        scheduleFrame();
        return rendered;
    }

    /** True when only the picture of a canvas changed, so that the display list is rebuilt from the retained layout. / 캔버스 그림만 바뀌어 보관한 배치로 디스플레이 리스트만 다시 만들어야 하면 true. */
    private final AtomicBoolean paintDirty = new AtomicBoolean();

    /**
     * Installs the source of the canvas pictures (the script host of the page) that the renderer paints.
     * <p>
     * 렌더러가 그리는 캔버스 그림의 공급원(페이지의 스크립트 호스트)을 설치한다.
     *
     * @param source the source / 공급원
     */
    void setCanvasSource(org.duckdns.hjow.swingwebpanel.render.CanvasSource source) {
        resources.setCanvasSource(source);
    }

    /**
     * Installs the source of the audio control bar states (the script host of the page) that the renderer paints.
     * <p>
     * 렌더러가 그리는 오디오 컨트롤 막대 상태의 공급원(페이지의 스크립트 호스트)을 설치한다.
     *
     * @param source the source / 공급원
     */
    void setMediaControlSource(org.duckdns.hjow.swingwebpanel.render.MediaControlSource source) {
        resources.setMediaControlSource(source);
    }

    /**
     * Associates subresource loads with the active document's object URL environment.
     * <p>
     * 하위 리소스 로드를 활성 문서의 객체 URL 환경에 연결한다.
     *
     * @param scope document scope / 문서 범위
     */
    void setBlobUrlScope(org.duckdns.hjow.swingwebpanel.net.BlobUrlStore.Scope scope) {
        resources.setBlobUrlScope(scope);
    }

    /**
     * Records that the picture of a canvas changed and asks the view for a new rendering, without invalidating the layout. It only posts work
     * and may be called from any thread.
     * <p>
     * 캔버스의 그림이 바뀌었음을 기록하고 배치를 무효화하지 않고 뷰에 새로 그리도록 요청한다. 작업을 올리기만 하며 어느 스레드에서나 부를 수 있다.
     */
    void canvasChanged() {
        paintDirty.set(true);
        Runnable callback = invalidation;
        if (!closed.get() && callback != null) {
            callback.run();
        }
    }

    /** Makes controls and styles use the scripting-off state. / 컨트롤과 스타일에 스크립트 비활성 상태를 적용한다. */
    void scriptsDisabled() {
        document.setScriptingEnabled(false);
        invalidate();
    }

    /**
     * Returns an image that was already requested, without starting a request.
     * <p>
     * 이미 요청한 이미지를 요청을 시작하지 않고 반환한다.
     *
     * @param absoluteUrl the absolute URL / 절대 URL
     * @return the state or null / 상태 또는 null
     */
    org.duckdns.hjow.swingwebpanel.render.ImageResource peekImage(String absoluteUrl) {
        return resources.peekImage(absoluteUrl);
    }

    /**
     * Returns the state of an image in a CORS mode that was already requested, without starting a request.
     * <p>
     * 이미 요청한 CORS 모드 이미지의 상태를 요청을 시작하지 않고 반환한다.
     *
     * @param absoluteUrl the absolute URL / 절대 URL
     * @param cors        the CORS mode or null / CORS 모드. 없으면 null
     * @return the state or null / 상태 또는 null
     */
    org.duckdns.hjow.swingwebpanel.render.ImageResource peekImage(String absoluteUrl, String cors) {
        return resources.peekImage(absoluteUrl, cors);
    }

    /**
     * Schedules a new rendering at the end of the block period of a web font (CSS Fonts 4 section 4.8): the text that waited invisibly for the font is then laid
     * out again and painted in the fallback font (unless the font arrived before, which also renders again). Only the earliest end is waiting at a time; a later
     * end is found by the rendering of the earlier one.
     * <p>
     * 웹 글꼴의 block 기간이 끝날 때 새 렌더링을 예약한다(CSS Fonts 4 4.8 절): 그때 글꼴을 보이지 않게 기다리던 텍스트를 다시 배치해 대체 글꼴로 그린다(그 전에 글꼴이 오면 그때도 다시
     * 렌더링한다). 한 번에 가장 이른 끝 하나만 기다리며, 더 늦은 끝은 이른 끝의 렌더링이 찾는다.
     *
     * @param deadline the end in milliseconds of the system clock, 0 for none / 시스템 시계 ms 의 끝. 없으면 0
     */
    private synchronized void scheduleBlockEnd(final long deadline) {
        if (closed.get() || deadline <= 0 || (blockWake != 0 && blockWake <= deadline)) {
            return;
        }
        blockWake = deadline;
        if (frameTimer == null) {
            frameTimer = new Timer("swingwebpanel-animation", true);
        }
        frameTimer.schedule(new TimerTask() {
            /**
             * Forgets the waiting end and asks for a new rendering.
             * <p>
             * 기다리던 끝을 잊고 새 렌더링을 요청한다.
             */
            public void run() {
                synchronized (PagePresentation.this) {
                    // 더 이른 끝이 그 사이에 예약되지 않았을 때만 지운다.
                    if (blockWake == deadline) {
                        blockWake = 0;
                    }
                }
                invalidate();
            }
        }, Math.max(1, deadline - System.currentTimeMillis() + 1));
    }

    /**
     * Schedules the next frame while a transition or an animation runs: after about 16 milliseconds the page is marked as changed and
     * the invalidation callback asks the view to render again (which ticks the animations). Nothing is scheduled when nothing runs,
     * and only one frame is waiting at a time.
     * <p>
     * 전환이나 애니메이션이 실행되는 동안 다음 프레임을 예약한다: 약 16 밀리초 뒤 페이지를 변경됨으로 표시하고 무효화 콜백이 뷰에게 다시 렌더링하도록 요청한다(그러면
     * 애니메이션이 진행된다). 실행되는 것이 없으면 아무것도 예약하지 않고, 한 번에 프레임 하나만 기다린다.
     */
    private synchronized void scheduleFrame() {
        if (closed.get() || animations == null || !animations.isActive() || !frameQueued.compareAndSet(false, true)) {
            return;
        }
        if (frameTimer == null) {
            frameTimer = new Timer("swingwebpanel-animation", true);
        }
        frameTimer.schedule(new TimerTask() {
            /**
             * Marks the page as changed and asks for a new render.
             * <p>
             * 페이지를 변경됨으로 표시하고 새 렌더링을 요청한다.
             */
            public void run() {
                frameQueued.set(false);
                invalidate();
            }
        }, FRAME_MILLIS);
    }

    /** Returns the union of element border boxes in document coordinates (the origin is the top left of the page, scrolling is not subtracted) at the
     * size that the page was last laid out with. Used to scroll to a fragment target. / 마지막으로 배치한 크기에서 문서 좌표(원점이 페이지 왼쪽 위이며
     * 스크롤을 빼지 않음)로 요소 border box 합집합을 반환한다. 프래그먼트 대상으로 스크롤할 때 쓴다.
     * @param element queried element / 조회할 요소
     * @return x, y, width, height / x·y·폭·높이 */
    synchronized double[] documentBounds(Element element) {
        return bounds(element, layoutWidth > 0 ? layoutWidth : 800, layoutHeight > 0 ? layoutHeight : 600, new Point());
    }

    /** Returns the union of element border boxes in viewport coordinates after flushing changes. / 변경 반영 뒤 뷰포트 좌표로 요소 border box 합집합을 반환한다.
     * @param element queried element / 조회할 요소
     * @param width viewport width / 뷰포트 폭
     * @param height viewport height / 뷰포트 높이
     * @param position copied scroll position / 복사한 스크롤 위치
     * @return x, y, width, height / x·y·폭·높이 */
    synchronized double[] bounds(Element element, int width, int height, Point position) {
        if (element == null || element.getOwnerDocument() != document) {
            return new double[4];
        }
        RenderedPage page = render(width, height);
        if (page == null) {
            return new double[4];
        }
        List<LayoutBox> boxes = page.getLayout().getBoxes(element);
        if (boxes.isEmpty()) {
            return new double[4];
        }
        LayoutBox first = boxes.get(0);
        double left = first.getX();
        double top = first.getY();
        double right = left;
        double bottom = top;
        boolean nonempty = false;
        // 줄마다 분할된 인라인 요소도 모든 비어 있지 않은 border box의 합집합을 구한다.
        for (LayoutBox box : boxes) {
            if (box.getWidth() == 0 || box.getHeight() == 0) {
                continue;
            }
            left = nonempty ? Math.min(left, box.getX()) : box.getX();
            top = nonempty ? Math.min(top, box.getY()) : box.getY();
            right = nonempty ? Math.max(right, box.getX() + box.getWidth()) : box.getX() + box.getWidth();
            bottom = nonempty ? Math.max(bottom, box.getY() + box.getHeight()) : box.getY() + box.getHeight();
            nonempty = true;
        }
        return new double[] {left - position.x, top - position.y, nonempty ? right - left : 0, nonempty ? bottom - top : 0};
    }

    /** Tells whether a media query matches the page now (<code>window.matchMedia</code>), using the media environment of the style engine. / 미디어 질의가 지금 페이지에 일치하는지 알려 준다(<code>window.matchMedia</code>). 스타일 엔진의 미디어 환경을 쓴다.
     * @param query media query list text / 미디어 질의 목록 문자열
     * @param width viewport width / 뷰포트 폭
     * @param height viewport height / 뷰포트 높이
     * @return true if it matches / 일치하면 true */
    synchronized boolean matchesMedia(String query, int width, int height) {
        // 스타일 엔진이 아직 없으면 기본 환경으로 평가한다.
        MediaEnvironment env = environment == null ? new MediaEnvironment(width, height) : environment.copy();
        env.setViewport(width, height);
        env.setScripting(document.isScriptingEnabled());
        return MediaQueryList.parse(query).matches(env);
    }

    /** The selection geometry of the last rendering, made when first needed. / 마지막 렌더링의 선택 기하. 처음 필요할 때 만든다. */
    private org.duckdns.hjow.swingwebpanel.render.TextSelections selectionGeometry;

    /** Returns the selection geometry of a rendering (one per rendering). / 렌더링의 선택 기하를 반환한다(렌더링마다 하나).
     * @param page the rendering / 렌더링
     * @return the geometry / 기하 */
    private synchronized org.duckdns.hjow.swingwebpanel.render.TextSelections geometryOf(RenderedPage page) {
        if (selectionGeometry == null || selectionGeometry.getPage() != page) {
            selectionGeometry = new org.duckdns.hjow.swingwebpanel.render.TextSelections(page);
        }
        return selectionGeometry;
    }

    /** Returns the client rectangles of a range in viewport coordinates after flushing changes (<code>Range.getClientRects</code>). / 변경 반영 뒤 뷰포트 좌표로 범위의 클라이언트 사각형을 반환한다(<code>Range.getClientRects</code>).
     * @param range the range / 범위
     * @param width viewport width / 뷰포트 폭
     * @param height viewport height / 뷰포트 높이
     * @param position copied scroll position / 복사한 스크롤 위치
     * @return rectangles of x, y, width, height; empty if none / x·y·폭·높이의 사각형 목록. 없으면 빈 배열 */
    synchronized double[][] rangeRects(org.duckdns.hjow.swingwebpanel.dom.Range range, int width, int height, Point position) {
        if (range == null || range.getStartContainer().getOwnerDocument() != document) {
            return new double[0][];
        }
        RenderedPage page = render(width, height);
        if (page == null) {
            return new double[0][];
        }
        List<java.awt.geom.Rectangle2D> list = geometryOf(page).rects(range);
        double[][] out = new double[list.size()][];
        // 문서 좌표의 사각형에서 스크롤 위치를 빼 뷰포트 좌표로 옮긴다.
        for (int i = 0; i < out.length; i++) {
            java.awt.geom.Rectangle2D r = list.get(i);
            out[i] = new double[] {r.getX() - position.x, r.getY() - position.y, r.getWidth(), r.getHeight()};
        }
        return out;
    }

    /** Returns the text of a range as it is copied, after flushing changes (<code>Selection.toString</code>). / 변경 반영 뒤 범위의 복사될 때의 텍스트를 반환한다(<code>Selection.toString</code>).
     * @param range the range / 범위
     * @param width viewport width / 뷰포트 폭
     * @param height viewport height / 뷰포트 높이
     * @return the text, or null if the page has no rendering / 텍스트. 렌더링이 없으면 null */
    synchronized String rangeText(org.duckdns.hjow.swingwebpanel.dom.Range range, int width, int height) {
        if (range == null || range.getStartContainer().getOwnerDocument() != document) {
            return null;
        }
        RenderedPage page = render(width, height);
        return page == null ? null : geometryOf(page).text(range);
    }

    /**
     * Reads HTML rendered text from the current document layout. / 현재 문서 배치에서 HTML 렌더링 텍스트를 읽는다.
     * @param element the queried element / 조회할 요소
     * @param width viewport width / 뷰포트 폭
     * @param height viewport height / 뷰포트 높이
     * @return text, or null if there is no active rendering / 텍스트. 활성 렌더링이 없으면 null
     */
    synchronized String innerText(Element element, int width, int height) {
        if (element.getOwnerDocument() != document) {
            return null;
        }
        RenderedPage page = render(width, height);
        return page == null ? null : org.duckdns.hjow.swingwebpanel.render.RenderedText.get(element, page.getLayout(), css.getResolver());
    }

    /** Returns the border boxes of an element, one per box, in viewport coordinates (<code>getClientRects</code>). / 요소의 border box 를 상자마다 하나씩 뷰포트 좌표로 반환한다(<code>getClientRects</code>).
     * @param element queried element / 조회할 요소
     * @param width viewport width / 뷰포트 폭
     * @param height viewport height / 뷰포트 높이
     * @param position copied scroll position / 복사한 스크롤 위치
     * @return rectangles of x, y, width, height; empty if the element has no box / 사각형 목록. 상자가 없으면 빈 배열 */
    synchronized double[][] elementRects(Element element, int width, int height, Point position) {
        if (element == null || element.getOwnerDocument() != document) {
            return new double[0][];
        }
        RenderedPage page = render(width, height);
        if (page == null) {
            return new double[0][];
        }
        List<LayoutBox> boxes = page.getLayout().getBoxes(element);
        List<double[]> out = new java.util.ArrayList<double[]>();
        // 비어 있지 않은 상자마다 사각형 하나를 낸다(줄마다 갈라진 인라인 요소는 여러 개).
        for (LayoutBox box : boxes) {
            if (box.getWidth() == 0 && box.getHeight() == 0) {
                continue;
            }
            java.awt.geom.Rectangle2D r = new java.awt.geom.Rectangle2D.Double(box.getX(), box.getY(), box.getWidth(), box.getHeight());
            java.awt.geom.AffineTransform full = box.getFullTransform();
            if (full != null) {
                r = full.createTransformedShape(r).getBounds2D();
            }
            out.add(new double[] {r.getX() - position.x, r.getY() - position.y, r.getWidth(), r.getHeight()});
        }
        return out.toArray(new double[out.size()][]);
    }

    /** Returns the topmost element at a viewport point after flushing changes (<code>document.elementFromPoint</code>). / 변경 반영 뒤 뷰포트 점의 최상위 요소를 반환한다(<code>document.elementFromPoint</code>).
     * @param x viewport x / 뷰포트 x
     * @param y viewport y / 뷰포트 y
     * @param width viewport width / 뷰포트 폭
     * @param height viewport height / 뷰포트 높이
     * @param position copied scroll position / 복사한 스크롤 위치
     * @return the element, or null outside the viewport or the document / 요소. 뷰포트나 문서 밖이면 null */
    synchronized Element elementAt(double x, double y, int width, int height, Point position) {
        // 뷰포트 밖의 점은 요소가 없다.
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return null;
        }
        RenderedPage page = render(width, height);
        if (page == null) {
            return null;
        }
        return page.getHitTester().hit(x + position.x, y + position.y, position.x, position.y).getElement();
    }

    /** Invalidates only connected mutations without reading the DOM from a callback. / 콜백에서 DOM을 읽지 않고 연결된 변경만 무효화한다.
     * @param record mutation record / 변경 기록
     * @param connected whether connected / 연결 여부 */
    public void mutated(MutationRecord record, boolean connected) {
        if (connected) {
            invalidate();
        }
    }

    /** Marks a completed resource without touching CSS on the worker. / 작업자에서 CSS를 건드리지 않고 완료 리소스를 표시한다.
     * @param stylesheet whether stylesheet collection changed / 스타일시트 수집 변경 여부 */
    public void resourceChanged(boolean stylesheet) {
        if (stylesheet) {
            sheetsChanged.set(true);
        }
        invalidate();
        // 스크립트 호스트에도 알려, 이 리소스가 load 이벤트가 기다리던 마지막이었다면 load 가 발생하게 한다.
        Runnable observer = resourceObserver;
        if (observer != null && !closed.get()) {
            observer.run();
        }
        Runnable load = loadObserver;
        if (load != null && !closed.get()) {
            load.run();
        }
    }

    /**
     * Installs the receiver that is told after every resource that ended (it must only post work; it is called on a resource worker).
     * <p>
     * 리소스가 끝날 때마다 알려 받는 쪽을 설치한다(작업을 올리기만 해야 한다. 리소스 작업자에서 호출된다).
     *
     * @param observer the receiver / 받는 쪽
     */
    void setResourceObserver(Runnable observer) {
        resourceObserver = observer;
    }

    /** Receiver that is told whenever the loading state of the page may have changed (a resource or the load event), or null. / 페이지의 로딩 상태가 바뀌었을 수 있을 때마다(리소스, load 이벤트) 알려 받는 쪽. 없으면 null. */
    private volatile Runnable loadObserver;

    /**
     * Installs the receiver that is told whenever the loading state of the page may have changed: a resource ended or the window
     * <code>load</code> event fired. The frame that shows this page uses it to send the <code>load</code> event to its element. It must only
     * post work.
     * <p>
     * 페이지의 로딩 상태가 바뀌었을 수 있을 때마다(리소스가 끝났거나 window <code>load</code> 이벤트가 발생했을 때) 알려 받는 쪽을 설치한다. 이 페이지를 보여
     * 주는 프레임이 요소에 <code>load</code> 이벤트를 보내는 데 쓴다. 작업을 올리기만 해야 한다.
     *
     * @param observer the receiver / 받는 쪽
     */
    void setLoadObserver(Runnable observer) {
        loadObserver = observer;
    }

    /**
     * Tells whether a resource that this page started (a stylesheet or an image) is still loading.
     * <p>
     * 이 페이지가 시작한 리소스(스타일시트나 이미지)가 아직 로딩 중인지 알려 준다.
     *
     * @return true while one is loading / 로딩 중인 것이 있으면 true
     */
    boolean hasPendingResources() {
        FrameSet set = frameSet;
        return !closed.get() && (resources.hasPending() || set != null && set.hasPending());
    }

    /** The frames of the document (nested browsing contexts), or null for a page that has none. / 문서의 프레임(중첩된 브라우징 문맥). 없는 페이지는 null. */
    private volatile FrameSet frameSet;

    /**
     * Installs the frames of the document: the renderer gets their pictures from it and the loading state of the page waits for them.
     * <p>
     * 문서의 프레임을 설치한다: 렌더러가 여기서 프레임 그림을 얻고 페이지의 로딩 상태는 프레임을 기다린다.
     *
     * @param set the frame set / 프레임 집합
     */
    void setFrameSet(FrameSet set) {
        frameSet = set;
        resources.setFrameSource(set);
    }

    /**
     * Returns the frames of the document.
     * <p>
     * 문서의 프레임을 반환한다.
     *
     * @return the frame set or null / 프레임 집합 또는 null
     */
    FrameSet frameSet() {
        return frameSet;
    }

    /**
     * Returns the embedder policy a child frame's response must satisfy.
     * <p>
     * 자식 프레임의 응답이 충족해야 하는 삽입 정책을 반환한다.
     *
     * @return the document's policy / 문서의 정책
     */
    CrossOriginPolicy.Embedder embedderPolicy() {
        return embedderPolicy;
    }

    /**
     * Returns how many resources of this page ended and how many were started.
     * <p>
     * 이 페이지의 리소스가 몇 개 끝났고 몇 개를 시작했는지 반환한다.
     *
     * @return {finished, started} / {끝난 수, 시작한 수}
     */
    int[] resourceActivity() {
        int[] activity = resources.activity();
        FrameSet set = frameSet;
        // 프레임도 끝나야 하는 작업이므로 시작한 수와 끝난 수에 더한다.
        if (set != null) {
            int[] frames = set.activity();
            activity[0] += frames[0];
            activity[1] += frames[1];
        }
        return activity;
    }

    /**
     * Asks the view to report the loading state of the page again (after the window <code>load</code> event, or when scripts can no longer run)
     * without invalidating the layout. It only posts work and may be called from any thread.
     * <p>
     * 레이아웃을 무효화하지 않고 뷰가 페이지의 로딩 상태를 다시 보고하게 한다(window <code>load</code> 이벤트 뒤, 또는 스크립트가 더 실행될 수 없을
     * 때). 작업을 올리기만 하며 어느 스레드에서나 부를 수 있다.
     */
    void loadChanged() {
        Runnable callback = invalidation;
        if (!closed.get() && callback != null) {
            callback.run();
        }
        Runnable load = loadObserver;
        if (!closed.get() && load != null) {
            load.run();
        }
    }

    /** The default fonts of the component that shows this page (API contract 5.27). / 이 페이지를 보이는 컴포넌트의 기본 글꼴(API 계약 5.27). */
    private volatile FontDefaults fontDefaults = FontDefaults.NONE;

    /** The default fonts that the style engine and the font set were given last. / 스타일 엔진과 글꼴 집합에 마지막으로 준 기본 글꼴. */
    private FontDefaults appliedFontDefaults = FontDefaults.NONE;

    /**
     * Sets the default fonts of the component; the page is styled and laid out again if they changed.
     * <p>
     * 컴포넌트의 기본 글꼴을 정한다. 바뀌었으면 페이지의 스타일과 배치를 다시 계산한다.
     *
     * @param defaults the defaults, null for the built-in ones / 기본값. 내장 기본값이면 null
     */
    void setFontDefaults(FontDefaults defaults) {
        FontDefaults next = defaults == null ? FontDefaults.NONE : defaults;
        boolean changed = !next.equals(fontDefaults);
        fontDefaults = next;
        if (changed) {
            invalidate();
        }
    }

    /**
     * Gives the default fonts to the style engine (the default size and the user style sheet) and to the font set (the families of the
     * generic names), if they changed since the last time. Call it with the lock of this object, after the style engine was created.
     * <p>
     * 기본 글꼴이 지난번과 달라졌으면 스타일 엔진(기본 크기와 사용자 스타일 시트)과 글꼴 집합(일반 이름의 패밀리)에 준다. 스타일 엔진을 만든 뒤, 이
     * 객체의 잠금을 쥐고 부른다.
     */
    private void applyFontDefaults() {
        FontDefaults d = fontDefaults;
        if (d.equals(appliedFontDefaults)) {
            return;
        }
        appliedFontDefaults = d;
        StyleResolver resolver = css.getResolver();
        resolver.setDefaultFontSize(d.getSize());
        List<StyleSheet> user = new java.util.ArrayList<StyleSheet>();
        String text = d.userStyleSheet();
        if (text.length() > 0) {
            user.add(StyleSheet.parse(text, null, StyleSheet.ORIGIN_USER, null, new org.duckdns.hjow.swingwebpanel.css.CssDiagnostics()));
        }
        resolver.setUserStyleSheets(user);
        fonts.setGenericFamilies(d.getSerif(), d.getSansSerif(), d.getMonospace());
    }

    /** Coalesces layout invalidation and notifies only an installed view. / 배치 무효화를 합치고 설치된 뷰에만 알린다. */
    private void invalidate() {
        dirty.set(true);
        Runnable callback = invalidation;
        if (!closed.get() && callback != null) {
            callback.run();
        }
    }

    /**
     * Loads an image for the script side of the page (the <code>load</code> and <code>error</code> events of <code>img</code> elements)
     * through the same cached resource that the renderer paints from. Callable from any thread.
     * <p>
     * 페이지 스크립트 쪽(<code>img</code> 요소의 <code>load</code>, <code>error</code> 이벤트)을 위해 렌더러가 그리는 것과 같은 캐시 리소스로
     * 이미지를 로드한다. 어느 스레드에서나 부를 수 있다.
     *
     * @param absoluteUrl the absolute URL / 절대 URL
     * @param waiter      the receiver of the final state / 최종 상태를 받는 쪽
     */
    void requestImage(String absoluteUrl, PageResources.ImageWaiter waiter) {
        resources.requestImage(absoluteUrl, waiter);
    }

    /**
     * Loads an image for an element with a <code>crossorigin</code> attribute in CORS mode (see {@link PageResources#requestImage(String, String,
     * PageResources.ImageWaiter)}). Callable from any thread.
     * <p>
     * <code>crossorigin</code> 속성이 있는 요소의 이미지를 CORS 모드로 로드한다({@link PageResources#requestImage(String, String, PageResources.ImageWaiter)}
     * 참고). 어느 스레드에서나 부를 수 있다.
     *
     * @param absoluteUrl the absolute URL / 절대 URL
     * @param cors        <code>anonymous</code> or <code>use-credentials</code> / <code>anonymous</code> 또는 <code>use-credentials</code>
     * @param waiter      the receiver of the final state / 최종 상태를 받는 쪽
     */
    void requestImage(String absoluteUrl, String cors, PageResources.ImageWaiter waiter) {
        resources.requestImage(absoluteUrl, cors, waiter);
    }

    /**
     * Fetches the resource of an <code>audio</code> element for the script side of the page. Callable from any thread.
     * <p>
     * 페이지 스크립트 쪽을 위해 <code>audio</code> 요소의 리소스를 가져온다. 어느 스레드에서나 부를 수 있다.
     *
     * @param absoluteUrl the absolute URL / 절대 URL
     * @param callback    the receiver of the result / 결과를 받는 쪽
     * @return the handle for cancelling / 취소용 핸들
     */
    MediaRequestSource.Request requestMedia(String absoluteUrl, MediaRequestSource.Callback callback) {
        return resources.requestMedia(absoluteUrl, callback);
    }

    /**
     * Records the state of a media resource that a media element found out, for the renderer.
     * <p>
     * 미디어 요소가 알아낸 미디어 리소스의 상태를 렌더러를 위해 기록한다.
     *
     * @param absoluteUrl the absolute URL / 절대 URL
     * @param error       null if it can be played, else the reason / 재생할 수 있으면 null, 아니면 이유
     */
    void mediaState(String absoluteUrl, String error) {
        resources.mediaState(absoluteUrl, error);
    }

    /** Cancels immediately, then waits for parser/render ownership only on a cleanup worker. / 즉시 취소하고 파서·렌더링 소유권은 해제 작업자에서만 기다린다. */
    void retire() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        invalidation = null;
        if (parent != null) {
            parent.removeHook(this);
        }
        stopFrames();
        FrameSet set = frameSet;
        // 이 페이지의 프레임(자식 문서, 요청, 스크립트)도 함께 닫는다. 이미 그려진 그림은 불변이므로 화면에는 남는다.
        if (set != null) {
            set.close();
        }
        resources.close();
        PageCleanup.submit(new Runnable() {
            /** Finishes observer cleanup after parsing returns. / 파싱 반환 뒤 관찰자 해제를 끝낸다. */
            public void run() {
                boolean interrupted = false;
                // 버려진 파싱도 소유권을 반환한 뒤에만 CSS/DOM 관찰자를 해제한다.
                for (;;) {
                    try {
                        parsed.await();
                        break;
                    } catch (InterruptedException e) {
                        interrupted = true;
                    }
                }
                release();
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    /**
     * Stops the animation frames: the timer is canceled (its thread ends) and the animations end without an event.
     * <p>
     * 애니메이션 프레임을 멈춘다: 타이머를 취소하고(스레드가 끝난다) 애니메이션은 이벤트 없이 끝난다.
     */
    private void stopFrames() {
        Timer t;
        synchronized (this) {
            t = frameTimer;
            frameTimer = null;
            if (animations != null) {
                animations.dispose();
            }
        }
        if (t != null) {
            t.cancel();
        }
    }

    /** Drops observers under the same lock as rendering. / 렌더링과 같은 잠금 아래 관찰자를 제거한다. */
    private synchronized void release() {
        document.removeMutationListener(this);
        if (css != null) {
            css.dispose();
        }
        css = null;
        rendered = null;
        environment = null;
        document = null;
        parserSheets.clear();
    }

    /** Contextual loader preserving owner/import conditions through recursion. / 소유자·import 조건을 재귀적으로 보존하는 상황별 로더. */
    private final class SheetLoader implements ContextualStyleSheetLoader {
        /** Whether the owning chain permits blocking. / 소유 연결이 차단을 허용하는지 여부. */
        private final boolean eligible;
        /** Creates an inherited scope. / 상속한 범위를 만든다.
         * @param eligible blocking eligibility / 차단 허용 여부 */
        SheetLoader(boolean eligible) {
            this.eligible = eligible;
        }
        /** Selects an enabled parser owner and its matching media. / 활성 파서 소유자와 일치하는 미디어를 선택한다.
         * @param owner stylesheet owner / 시트 소유자
         * @return child loading policy / 자식 로딩 정책 */
        public StyleSheetLoader forOwner(Element owner) {
            return new SheetLoader(eligible && Boolean.TRUE.equals(parserSheets.get(owner)) && !owner.hasAttribute("disabled")
                    && MediaQueryList.parse(owner.getAttribute("media")).matches(environment));
        }
        /** Inherits the parent and import media conditions. / 부모와 import 미디어 조건을 상속한다.
         * @param media import media / import 미디어
         * @return child loading policy / 자식 로딩 정책 */
        public StyleSheetLoader forMedia(MediaQueryList media) {
            return new SheetLoader(eligible && media.matches(environment));
        }
        /** Waits only for a blocking parser scope; otherwise starts an asynchronous resource. / 파서 차단 범위만 기다리고 그 외에는 비동기 리소스를 시작한다.
         * @param url stylesheet address / 시트 주소
         * @return completed stylesheet / 완료된 시트
         * @throws IOException while pending or on failure/cancellation / 대기·실패·취소 시 */
        public LoadedStyleSheet load(String url) throws IOException {
            return eligible && waitingForStyles && parsing ? resources.awaitStyleSheet(url, styleWaiter) : resources.load(url);
        }
    }
}
