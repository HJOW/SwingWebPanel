package org.duckdns.hjow.swingwebpanel.core;

import java.awt.Graphics2D;
import java.awt.Point;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.media.playback.AudioOutputFactory;
import org.duckdns.hjow.swingwebpanel.media.webaudio.output.OutputFactory;
import org.duckdns.hjow.swingwebpanel.net.CookieStore;
import org.duckdns.hjow.swingwebpanel.net.FetchLimits;
import org.duckdns.hjow.swingwebpanel.net.ResourceFetcher;
import org.duckdns.hjow.swingwebpanel.render.RenderedPage;

/**
 * Connects a session's DOM/CSS/layout to Swing. DOM reads and rebuilding run on the session's document thread; only immutable
 * drawing commands and dimensions cross to the EDT. Page identity is checked again before painting or delivering a callback.
 * Viewport and DOM changes are coalesced, and equal viewport sizes do not schedule another layout (P09-07).
 * <p>
 * 세션의 DOM·CSS·배치를 Swing에 연결한다. DOM 접근과 재배치는 문서 스레드에서, 불변 그림 명령과 크기의 게시 및 그리기는 EDT에서 처리한다.
 * 그리기와 통지 전에도 페이지 동일성을 확인한다. 뷰포트·DOM 변경은 합치며 같은 뷰포트 크기는 재배치를 예약하지 않는다.
 */
public final class PageView {
    /** Unique daemon worker names, without component references. / 컴포넌트 참조 없는 고유 데몬 작업자 이름. */
    private static final AtomicInteger WORKER_IDS = new AtomicInteger();
    /** EDT notification supplied by the Swing host. / Swing 호스트가 제공하는 EDT 통지. */
    public interface Host {
        /** Revalidates and repaints the host on the EDT. / EDT에서 호스트를 재배치하고 다시 그린다. */
        void viewChanged();
    }

    /** Optional script time policy supplied by the Swing host. / Swing 호스트가 선택적으로 제공하는 스크립트 시간 정책. */
    public interface HostWithScriptLimits extends Host {
        /**
         * Chooses the wall time limit for tasks of one top-level document.
         * <p>
         * 최상위 문서 하나의 작업에 적용할 경과 시간 한도를 고른다.
         *
         * @param url final document address / 최종 문서 주소
         * @return positive time limit in milliseconds, or a nonpositive value for the default / 양수 밀리초. 0 이하면 기본값
         */
        long scriptTaskTimeLimitMillis(String url);
    }

    /** Session owning the document queue. / 문서 큐를 소유하는 세션. */
    private final BrowserSession session;
    /** Scroll dimensions published on the EDT. / EDT에서 게시하는 스크롤 크기. */
    private final ViewMetrics metrics;
    /** Host callback, never called while waiting on the EDT. / EDT를 기다리는 동안 호출하지 않는 호스트 콜백. */
    private final Host host;
    /** Fetcher shared with document requests. / 문서 요청과 공유하는 가져오기 객체. */
    private final ResourceFetcher fetcher;
    /** Component cookie store. / 컴포넌트 쿠키 저장소. */
    private final CookieStore cookies;
    /** Bounded background fetching/decoding, with idle daemon expiration. / 유휴 데몬이 종료되는 제한 가져오기·디코딩 실행기. */
    private final ThreadPoolExecutor workers;
    /** Current page state, null for blank or disposal. / 현재 페이지 상태. 빈 페이지 또는 폐기 시 null. */
    private volatile State active;
    /** Last viewport width in document pixels. / 문서 픽셀 단위 마지막 뷰포트 폭. */
    private volatile int width = 640;
    /** Last viewport height in document pixels. / 문서 픽셀 단위 마지막 뷰포트 높이. */
    private volatile int height = 480;
    /** Immutable-by-convention viewport position copied on the EDT, in CSS pixels. / EDT에서 복사하고 이후 바꾸지 않는 뷰포트 위치(CSS 픽셀). */
    private volatile Point scroll = new Point();
    /** The page zoom factor (1 for 100 percent): component pixels per CSS pixel. / 페이지 확대 배율(1 이면 100 %): CSS 픽셀 하나당 컴포넌트 픽셀 수. */
    private volatile double zoom = 1.0;
    /** The viewport width in component pixels as the host reported it. / 호스트가 알린 컴포넌트 픽셀 단위 뷰포트 폭. */
    private volatile int extentWidth = 640;
    /** The viewport height in component pixels as the host reported it. / 호스트가 알린 컴포넌트 픽셀 단위 뷰포트 높이. */
    private volatile int extentHeight = 480;
    /** The viewport position in component pixels as the host reported it. / 호스트가 알린 컴포넌트 픽셀 단위 뷰포트 위치. */
    private volatile Point rawScroll = new Point();
    /** The selection controller of the component (highlight and Shift/drag selection), or null. / 컴포넌트의 선택 제어기(강조와 Shift/드래그 선택). 없으면 null. */
    private volatile PageSelection selection;
    /** The search in the page of the component (highlights of the matches), or null. / 컴포넌트의 페이지 안 찾기(일치의 강조). 없으면 null. */
    private volatile PageFind finder;
    /** Stops new work after disposal. / 폐기 후 새 작업을 막는 표시. */
    private volatile boolean disposed;

    /** The factory of the sound outputs of the media elements, or null. / 미디어 요소의 소리 출력 팩터리. 없으면 null. */
    private volatile AudioOutputFactory audioOutputs;

    /** The factory of the outputs of the real-time Web Audio contexts, or null for the system output. / 실시간 Web Audio 컨텍스트의 출력 팩터리. 시스템 출력이면 null. */
    private volatile OutputFactory webAudioOutputs;

    /** The Swing overlays of the form controls (editors, bubbles, file chooser), or null. 폼 컨트롤의 Swing 오버레이(편집기, 말풍선, 파일 선택기). 없으면 null. */
    private volatile ControlOverlays controlOverlays;

    /** The receiver of form submissions, or null. 폼 제출을 받는 쪽. 없으면 null. */
    private volatile FormSubmitSink formSubmitSink;

    /**
     * Creates a view without starting threads of its own.
     * <p>자체 스레드를 시작하지 않고 뷰를 만든다.
     * @param session session and document queue / 세션과 문서 큐
     * @param metrics scroll dimensions / 스크롤 크기
     * @param host EDT callback / EDT 콜백
     */
    public PageView(BrowserSession session, ViewMetrics metrics, Host host) {
        this(session, metrics, host, new ResourceFetcher(null, FetchLimits.DEFAULT, null), new CookieStore());
    }

    /**
     * Creates a view using the same policies, SSL hook and cookies as main-document requests.
     * <p>주 문서 요청과 같은 정책·SSL 훅·쿠키를 사용하는 뷰를 만든다.
     * @param session owning session / 소유 세션
     * @param metrics scroll metrics / 스크롤 정보
     * @param host EDT callback / EDT 콜백
     * @param fetcher shared resource fetcher / 공용 리소스 가져오기 객체
     * @param cookies component cookies / 컴포넌트 쿠키
     */
    public PageView(BrowserSession session, ViewMetrics metrics, Host host, ResourceFetcher fetcher, CookieStore cookies) {
        if (session == null || metrics == null || host == null || fetcher == null || cookies == null) {
            throw new IllegalArgumentException("view arguments must not be null");
        }
        this.session = session;
        this.metrics = metrics;
        this.host = host;
        this.fetcher = fetcher;
        this.cookies = cookies;
        // 이 뷰가 확정된 페이지의 로딩 상태를 세션에 보고하므로, 세션은 확정 뒤에도 진행률을 이어 간다.
        session.attachView();
        workers = new ThreadPoolExecutor(4, 4, 5, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(256), new ResourceThreadFactory());
        workers.allowCoreThreadTimeOut(true);
    }

    /**
     * Reads the host's positive script task time or uses the engine default.
     * <p>
     * 호스트의 양수 스크립트 작업 시간을 읽거나 엔진 기본값을 쓴다.
     *
     * @param url final top-level document address / 최상위 문서의 최종 주소
     * @param defaultMillis engine default in milliseconds / 엔진 기본값(밀리초)
     * @return selected time in milliseconds / 선택한 밀리초
     */
    long scriptTaskTimeLimitMillis(String url, long defaultMillis) {
        long limit = host instanceof HostWithScriptLimits ? ((HostWithScriptLimits) host).scriptTaskTimeLimitMillis(url) : defaultMillis;
        return limit > 0L ? limit : defaultMillis;
    }

    /** Static factory without an implicit PageView/component reference. / PageView·컴포넌트의 암묵적 참조가 없는 정적 팩토리. */
    private static final class ResourceThreadFactory implements ThreadFactory {
        /** Creates the thread factory. / 스레드 팩토리를 만든다. */
        ResourceThreadFactory() {
        }
        /** Creates an expiring daemon worker. / 유휴 후 종료되는 데몬 작업자를 만든다.
         * @param work executor work / 실행기 작업
         * @return daemon thread / 데몬 스레드 */
        public Thread newThread(Runnable work) {
            Thread thread = new Thread(work, "swingwebpanel-resource-" + WORKER_IDS.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }

    /**
     * Sets the factory that opens the sound outputs of the media elements of the pages that this view shows (null for none: the elements then
     * fail to play). The component sets it once, when it is created.
     * <p>
     * 이 뷰가 보여 주는 페이지의 미디어 요소를 위한 소리 출력을 여는 팩터리를 정한다(null 이면 없음. 요소는 재생에 실패한다). 컴포넌트가 만들어질 때
     * 한 번 정한다.
     *
     * @param factory the factory or null / 팩터리 또는 null
     */
    public void setAudioOutputFactory(AudioOutputFactory factory) {
        this.audioOutputs = factory;
    }

    /**
     * Sets the factory that opens the outputs of the real-time <code>AudioContext</code> objects of the pages that this view shows (null for the system output, Java Sound).
     * The component sets it once, when it is created.
     * <p>
     * 이 뷰가 보여 주는 페이지의 실시간 <code>AudioContext</code> 의 출력을 여는 팩터리를 정한다(null 이면 시스템 출력, Java Sound). 컴포넌트가 만들어질 때 한 번 정한다.
     *
     * @param factory the factory or null / 팩터리 또는 null
     */
    public void setWebAudioOutputFactory(OutputFactory factory) {
        this.webAudioOutputs = factory;
    }

    /**
     * Sets the Swing overlays (editors over text controls, validation bubbles, file chooser) that the form controls of the pages use. The
     * component sets them once, when it is created; without them the form controls have no editors and no dialogs.
     * <p>
     * 페이지의 폼 컨트롤이 쓰는 Swing 오버레이(텍스트 컨트롤 위의 편집기, 검증 말풍선, 파일 선택기)를 정한다. 컴포넌트가 만들어질 때 한 번 정한다. 없으면 폼
     * 컨트롤에 편집기와 대화상자가 없다.
     *
     * @param overlays the overlays or null / 오버레이 또는 null
     */
    public void setControlOverlays(ControlOverlays overlays) {
        this.controlOverlays = overlays;
    }

    /**
     * Sets the receiver of the form submissions of the pages that this view shows (the navigation of the component).
     * <p>
     * 이 뷰가 보여 주는 페이지의 폼 제출을 받는 쪽(컴포넌트의 이동)을 정한다.
     *
     * @param sink the receiver or null / 받는 쪽 또는 null
     */
    public void setFormSubmitSink(FormSubmitSink sink) {
        this.formSubmitSink = sink;
    }

    /**
     * Creates the host of the form controls of a page before it is parsed.
     * <p>
     * 파싱 전에 페이지의 폼 컨트롤 호스트를 만든다.
     *
     * @param document     the DOM of the page / 페이지의 DOM
     * @param presentation the presentation state of the page / 페이지의 표현 상태
     * @return the host / 호스트
     */
    PageFormHost createFormHost(Document document, PagePresentation presentation) {
        return createFormHost(document, presentation, null);
    }

    /**
     * Creates the host of the form controls of a page. Its forms go to the browsing context that their <code>target</code> selects: the
     * top-level document reaches the receiver of the component, a frame is navigated by its own rules (sandbox, target, history).
     * <p>
     * 페이지의 폼 컨트롤 호스트를 만든다. 폼은 <code>target</code> 이 고르는 브라우징 문맥으로 간다: 최상위 문서는 컴포넌트의 수신자에 닿고, 프레임은 자기
     * 규칙(샌드박스, target, 이력)으로 이동한다.
     *
     * @param document     the DOM of the page / 페이지의 DOM
     * @param presentation the presentation state of the page / 페이지의 표현 상태
     * @param frame        the frame that the page is the document of, or null for the top-level document / 페이지가 문서인 프레임. 최상위 문서이면 null
     * @return the host / 호스트
     */
    PageFormHost createFormHost(Document document, PagePresentation presentation, FrameContext frame) {
        FormSubmitSink sink = FrameNavigator.formSink(session, presentation, frame, formSubmitSink);
        return new PageFormHost(document, presentation, this, session, sink, controlOverlays);
    }

    /**
     * Tells whether a presentation belongs to the page that this view shows now (not a page that was replaced or is still loading).
     * <p>
     * 표현 상태가 이 뷰가 지금 보여 주는 페이지의 것인지(교체된 페이지나 아직 읽는 중인 페이지가 아닌지) 알려 준다.
     *
     * @param presentation the presentation state / 표현 상태
     * @return true if it is the page that is shown / 보이는 페이지의 것이면 true
     */
    boolean isCurrent(PagePresentation presentation) {
        State s = active;
        return s != null && s.presentation == presentation && s.valid();
    }

    /**
     * Returns the factory of the sound outputs of the media elements, or null.
     * <p>
     * 미디어 요소의 소리 출력 팩터리를 반환한다. 없으면 null.
     *
     * @return the factory or null / 팩터리 또는 null
     */
    public AudioOutputFactory getAudioOutputFactory() {
        return audioOutputs;
    }

    /**
     * Returns the factory of the outputs of the real-time Web Audio contexts, or null for the system output.
     * <p>
     * 실시간 Web Audio 컨텍스트의 출력 팩터리를 반환한다. 시스템 출력이면 null.
     *
     * @return the factory or null / 팩터리 또는 null
     */
    public OutputFactory getWebAudioOutputFactory() {
        return webAudioOutputs;
    }

    /**
     * Installs the confirmed snapshot. Delayed notifications for replaced pages are ignored. Call on the EDT.
     * <p>확정된 스냅샷을 설치한다. 이미 교체된 페이지의 늦은 통지는 무시한다. EDT에서 호출한다.
     * @param snapshot confirmed page / 확정된 페이지
     */
    public synchronized void pageConfirmed(PageSnapshot snapshot) {
        if (disposed || session.getSnapshot() != snapshot) {
            return;
        }
        if (active != null && active.snapshot == snapshot && active.valid()) {
            return;
        }
        closeActive();
        metrics.setContentSize(0, 0);
        host.viewChanged();
        if (snapshot.getDocument() instanceof HtmlPageDocument) {
            State state = new State(snapshot);
            active = state;
            state.request();
        }
    }

    /**
     * Installs a scripted page on ownership handoff, before any zero-delay timer can query its presentation. No Swing call occurs here.
     * <p>지연 0 타이머의 표현 조회 전에 소유권 인계 시 스크립트 페이지를 설치하며 여기서는 Swing을 호출하지 않는다.
     * @param snapshot confirmed page / 확정된 페이지
     */
    synchronized void preparePage(PageSnapshot snapshot) {
        if (disposed || session.getSnapshot() != snapshot || active != null && active.snapshot == snapshot && active.valid()) {
            return;
        }
        closeActive();
        if (snapshot.getDocument() instanceof HtmlPageDocument) {
            State state = new State(snapshot);
            active = state;
            state.request();
        }
    }

    /** Returns the copied viewport width without calling Swing. / Swing 호출 없이 복사한 뷰포트 폭을 반환한다.
     * @return width in CSS pixels / CSS 픽셀 단위 폭 */
    int viewportWidth() {
        return width;
    }

    /** Returns the copied viewport height without calling Swing. / Swing 호출 없이 복사한 뷰포트 높이를 반환한다.
     * @return height in CSS pixels / CSS 픽셀 단위 높이 */
    int viewportHeight() {
        return height;
    }

    /** Copies the viewport position on the EDT; the document queue never reads Swing widgets. / EDT에서 뷰포트 위치를 복사하며 문서 큐는 Swing 위젯을 읽지 않는다.
     * @param x horizontal scroll / 가로 스크롤
     * @param y vertical scroll / 세로 스크롤 */
    public void setViewportPosition(int x, int y) {
        rawScroll = new Point(x, y);
        // 컴포넌트 픽셀의 스크롤 위치를 CSS 픽셀로 바꿔 보관한다(배율 1 이면 그대로이다).
        double z = zoom;
        scroll = new Point((int) Math.round(x / z), (int) Math.round(y / z));
    }

    /** Creates page-owned presentation state before parsing begins. / 파싱 전에 페이지 소유 표현 상태를 만든다.
     * @param document unshared DOM / 독립 DOM
     * @param context copied resource settings / 복사한 리소스 설정
     * @return new presentation / 새 표현 상태 */
    PagePresentation createPresentation(Document document, PageRequestContext context) {
        PagePresentation created = new PagePresentation(document, context, fetcher, cookies, workers);
        created.setFontDefaults(fontDefaults);
        presentations.put(created, Boolean.TRUE);
        return created;
    }

    /** The default fonts of the component (API contract 5.27). / 컴포넌트의 기본 글꼴(API 계약 5.27). */
    private volatile FontDefaults fontDefaults = FontDefaults.NONE;

    /** The presentations that this view created and that are still in use (top-level pages and frames), held weakly. / 이 뷰가 만들었고 아직 쓰이는 표현(최상위 페이지와 프레임). 약하게 붙든다. */
    private final java.util.Map<PagePresentation, Boolean> presentations = java.util.Collections
            .synchronizedMap(new java.util.WeakHashMap<PagePresentation, Boolean>());

    /**
     * Returns the default fonts of the component.
     * <p>
     * 컴포넌트의 기본 글꼴을 반환한다.
     *
     * @return the defaults; never null / 기본값. null 이 아니다
     */
    public FontDefaults getFontDefaults() {
        return fontDefaults;
    }

    /**
     * Sets the default fonts of the component: the pages that are shown now (frames included) are styled and laid out again, and pages
     * that are loaded later start with the new defaults.
     * <p>
     * 컴포넌트의 기본 글꼴을 정한다: 지금 보이는 페이지(프레임 포함)의 스타일과 배치를 다시 계산하고, 나중에 읽는 페이지는 새 기본값으로 시작한다.
     *
     * @param defaults the defaults, null for the built-in ones / 기본값. 내장 기본값이면 null
     */
    public void setFontDefaults(FontDefaults defaults) {
        FontDefaults next = defaults == null ? FontDefaults.NONE : defaults;
        fontDefaults = next;
        PagePresentation[] live;
        synchronized (presentations) {
            live = presentations.keySet().toArray(new PagePresentation[0]);
        }
        // 살아 있는 표현마다 새 기본값을 알린다. 닫힌 표현은 스스로 무시한다.
        for (int i = 0; i < live.length; i++) {
            if (live[i] != null) {
                live[i].setFontDefaults(next);
            }
        }
    }

    /** Returns a copy of the latest EDT scroll position. / 최신 EDT 스크롤 위치의 복사본을 반환한다.
     * @return scroll position / 스크롤 위치 */
    Point viewportPosition() {
        return new Point(scroll);
    }

    /**
     * Updates the viewport extent, not the possibly overflowing component size. Equal sizes do no work.
     * <p>넘칠 수 있는 컴포넌트 크기가 아닌 뷰포트 범위를 갱신한다. 같은 크기면 작업하지 않는다.
     * @param w viewport width; nonpositive values keep the previous width / 뷰포트 폭. 0 이하는 이전 값 유지
     * @param h viewport height; nonpositive values keep the previous height / 뷰포트 높이. 0 이하는 이전 값 유지
     */
    public void setViewport(int w, int h) {
        if (w > 0) {
            extentWidth = w;
        }
        if (h > 0) {
            extentHeight = h;
        }
        applyViewport();
    }

    /**
     * Turns the viewport extent of the host (component pixels) into the CSS viewport that the layout uses (extent divided by the zoom factor)
     * and schedules a layout if it changed.
     * <p>
     * 호스트의 뷰포트 크기(컴포넌트 픽셀)를 레이아웃이 쓰는 CSS 뷰포트(크기를 배율로 나눈 값)로 바꾸고, 바뀌었으면 재배치를 예약한다.
     */
    private void applyViewport() {
        double z = zoom;
        int nextW = Math.max(1, (int) Math.round(extentWidth / z));
        int nextH = Math.max(1, (int) Math.round(extentHeight / z));
        if (nextW == width && nextH == height) {
            return;
        }
        width = nextW;
        height = nextH;
        State state = active;
        if (state != null) {
            state.request();
        }
    }

    /**
     * Returns the page zoom factor: component pixels per CSS pixel (1 for 100 percent).
     * <p>
     * 페이지 확대 배율을 반환한다: CSS 픽셀 하나당 컴포넌트 픽셀 수(1 이면 100 %).
     *
     * @return the factor / 배율
     */
    public double getZoom() {
        return zoom;
    }

    /**
     * Sets the page zoom factor. The layout works in CSS pixels, so the CSS viewport (extent divided by the factor) and the scroll position
     * in CSS pixels are computed again, and a layout is scheduled if the viewport changed; painting, hit testing and the scroll size use the
     * factor from now on. Call it on the event dispatch thread.
     * <p>
     * 페이지 확대 배율을 정한다. 레이아웃은 CSS 픽셀로 일하므로 CSS 뷰포트(크기를 배율로 나눈 값)와 CSS 픽셀 스크롤 위치를 다시 계산하고, 뷰포트가 바뀌었으면
     * 재배치를 예약한다. 그리기, 좌표 조회, 스크롤 크기는 이제부터 배율을 쓴다. 이벤트 디스패치 스레드에서 호출한다.
     *
     * @param factor the factor, a finite positive number / 배율. 유한한 양수
     * @throws IllegalArgumentException for NaN, infinite or not positive numbers / NaN, 무한, 양수가 아닌 수일 때
     */
    public void setZoom(double factor) {
        if (Double.isNaN(factor) || Double.isInfinite(factor) || factor <= 0) {
            throw new IllegalArgumentException("zoom must be a finite positive number: " + factor);
        }
        zoom = factor;
        Point raw = rawScroll;
        scroll = new Point((int) Math.round(raw.x / factor), (int) Math.round(raw.y / factor));
        applyViewport();
    }

    /**
     * Sets the selection controller of the component (null for none): it paints the highlight over the page and is told about every new layout.
     * <p>
     * 컴포넌트의 선택 제어기를 정한다(없으면 null). 페이지 위에 강조를 그리고 새 레이아웃을 알게 된다.
     *
     * @param controller the controller or null / 제어기. 없으면 null
     */
    public void setSelection(PageSelection controller) {
        this.selection = controller;
    }

    /**
     * Sets the search in the page of the component (null for none): its highlights are painted over the page and it is told about every
     * new layout.
     * <p>
     * 컴포넌트의 페이지 안 찾기를 정한다(없으면 null). 그 강조를 페이지 위에 그리고, 새 레이아웃을 알게 된다.
     *
     * @param find the search or null / 찾기. 없으면 null
     */
    public void setFind(PageFind find) {
        this.finder = find;
    }

    /**
     * Returns the selection controller of the component, or null.
     * <p>
     * 컴포넌트의 선택 제어기를 반환한다. 없으면 null.
     *
     * @return the controller or null / 제어기. 없으면 null
     */
    public PageSelection getSelection() {
        return selection;
    }

    /**
     * Creates the listener that a document of this view's pages gets for its selection: every change of the selection of the document is
     * passed to the selection controller (see {@link PageSelection}).
     * <p>
     * 이 뷰의 페이지의 문서가 선택을 위해 받는 리스너를 만든다: 문서의 선택이 바뀔 때마다 선택 제어기에 전달한다({@link PageSelection} 참조).
     *
     * @return the listener / 리스너
     */
    org.duckdns.hjow.swingwebpanel.dom.SelectionListener createSelectionListener() {
        return new org.duckdns.hjow.swingwebpanel.dom.SelectionListener() {
            /** Passes the change on to the controller. / 변경을 제어기에 넘긴다.
             * @param changed the selection that changed / 바뀐 선택 */
            public void selectionChanged(org.duckdns.hjow.swingwebpanel.dom.Selection changed) {
                PageSelection controller = selection;
                if (controller != null) {
                    controller.documentSelectionChanged(changed.getDocument());
                }
            }
        };
    }

    /**
     * Returns the current immutable drawing/layout snapshot for internal hosting code. DOM-linked hit testing must still
     * run on the document thread. A superseded or unfinished page returns null.
     * <p>내부 호스트용 현재 불변 그림·배치 스냅샷을 반환한다. DOM과 연결된 좌표 조회는 문서 스레드에서 수행한다. 교체되거나 미완성이면 null.
     * @return rendered snapshot or null / 렌더링 스냅샷 또는 null
     */
    public RenderedPage getRenderedPage() {
        State state = active;
        return state != null && state.valid() ? state.page : null;
    }

    /**
     * Paints immutable commands on the EDT in component/document coordinates. JScrollPane supplies scrolling by translating
     * the component, so no second scroll translation is applied here. The caller owns the graphics context.
     * <p>EDT에서 컴포넌트·문서 좌표의 불변 명령을 그린다. JScrollPane이 컴포넌트를 옮기므로 추가 스크롤 변환은 하지 않는다. 그래픽스는 호출자가 소유한다.
     * @param graphics graphics context / 그래픽스 컨텍스트
     */
    public void paint(Graphics2D graphics) {
        RenderedPage page = getRenderedPage();
        if (page != null) {
            double factor = zoom;
            // JScrollPane 이 컴포넌트를 옮겨 그래픽스가 이미 스크롤되어 있으므로 옮김은 0 이고, fixed 와 sticky 상자는 뷰포트 위치를 알아야 한다.
            java.awt.Point view = scroll;
            page.paint(graphics, graphics.getClipBounds(), 0, 0, factor, 0L, view.x, view.y);
            PageFind find = finder;
            // 찾기의 강조는 선택 강조 아래에 그린다: 다른 일치들을 먼저, 현재 일치를 그 위에.
            if (find != null) {
                org.duckdns.hjow.swingwebpanel.render.SelectionHighlight matches = find.getMatches();
                if (matches != null && matches.getPage() == page) {
                    matches.paint(graphics, factor);
                }
                org.duckdns.hjow.swingwebpanel.render.SelectionHighlight current = find.getCurrent();
                if (current != null && current.getPage() == page) {
                    current.paint(graphics, factor);
                }
            }
            PageSelection controller = selection;
            org.duckdns.hjow.swingwebpanel.render.SelectionHighlight highlight = controller == null ? null : controller.getHighlight();
            // 선택 강조는 지금 그리는 렌더링에 대해 계산된 것일 때만 그 위에 덧칠한다.
            if (highlight != null && highlight.getPage() == page) {
                highlight.paint(graphics, factor);
            }
            // 편집 캐럿은 깜박임 주기의 보이는 절반에서만 그린다.
            org.duckdns.hjow.swingwebpanel.render.SelectionHighlight caret = controller == null ? null : controller.getCaret();
            if (caret != null && caret.getPage() == page && controller.isCaretOn()) {
                caret.paint(graphics, factor);
            }
        }
    }

    /**
     * Invalidates the current drawing immediately and drops page observers on the document queue. It is safe from any thread.
     * <p>현재 그림을 즉시 무효화하고 문서 큐에서 페이지 관찰자를 해제한다. 어느 스레드에서나 안전하다.
     */
    public synchronized void clear() {
        closeActive();
        SwingUtilities.invokeLater(new Runnable() {
            /** Resets scroll dimensions on the EDT. / EDT에서 스크롤 크기를 초기화한다. */
            public void run() {
                if (active == null) {
                    metrics.setContentSize(0, 0);
                    host.viewChanged();
                }
            }
        });
    }

    /** Stops this view permanently; repeated calls are safe. / 이 뷰를 영구 중지하며 반복 호출은 안전하다. */
    public synchronized void dispose() {
        disposed = true;
        clear();
        workers.shutdownNow();
    }

    /** Invalidates and releases the last page under the view lock. / 뷰 잠금 아래 이전 페이지를 무효화하고 해제한다. */
    private void closeActive() {
        final State previous = active;
        active = null;
        ControlOverlays overlays = controlOverlays;
        if (overlays != null) {
            // 페이지가 바뀌거나 비워지면 그 페이지의 편집기와 말풍선을 없앤다.
            overlays.clearLater();
        }
        PageSelection controller = selection;
        // 선택 강조와 드래그 상태도 옛 페이지를 붙잡지 않도록 버린다.
        if (controller != null) {
            controller.pageClosed();
        }
        PageFind find = finder;
        // 찾기도 옛 페이지와 함께 끝난다.
        if (find != null) {
            find.pageClosed();
        }
        if (previous != null) {
            previous.closed = true;
            previous.page = null;
            previous.presentation.retire();
            session.submitDocumentTask(new Runnable() {
                /** Removes DOM/style observers on their owning thread. / 소유 스레드에서 DOM·스타일 관찰자를 제거한다. */
                public void run() {
                    previous.release();
                }
            });
        }
    }

    /** One page's state; mutable engine fields are document-thread confined. / 한 페이지의 상태이며 엔진 가변 필드는 문서 스레드 전용이다. */
    private final class State implements Runnable {
        /** Snapshot whose identity protects callbacks. / 콜백을 보호하는 동일성 기준 스냅샷. */
        private final PageSnapshot snapshot;
        /** The document holder of the snapshot, kept so that reporting does not depend on the snapshot after its disposal. / 스냅샷의 문서 보관 객체. 폐기 뒤에도 보고가 스냅샷에 의존하지 않게 한다. */
        private final HtmlPageDocument holder;
        /** Coalesces rebuild requests. / 재배치 요청을 합치는 표시. */
        private final AtomicBoolean queued = new AtomicBoolean();
        /** Page state transferred from parsing or created for a custom loader. / 파싱에서 인계받거나 사용자 로더용으로 만든 페이지 상태. */
        private final PagePresentation presentation;
        /** Current immutable drawing commands. / 현재 불변 그림 명령. */
        private volatile RenderedPage page;
        /** Page retirement flag. / 페이지 교체 표시. */
        private volatile boolean closed;

        /**
         * Creates a pending state.
         * <p>대기 중인 상태를 만든다.
         * @param snapshot owning page / 소유 페이지
         */
        State(PageSnapshot snapshot) {
            this.snapshot = snapshot;
            holder = (HtmlPageDocument) snapshot.getDocument();
            PagePresentation existing = holder.getPresentation();
            if (existing == null) {
                existing = createPresentation(holder.getDocument(), holder.requests);
                existing.parsingFinished();
            }
            presentation = existing;
            presentation.setInvalidation(new Runnable() {
                /** Queues drawing only after the page is installed. / 페이지 설치 뒤에만 그리기를 예약한다. */
                public void run() {
                    request();
                }
            });
        }

        /** Tests whether the page still owns this view. / 페이지가 이 뷰를 여전히 소유하는지 확인한다.
         * @return true while current / 현재이면 true */
        boolean valid() {
            return !closed && !disposed && active == this && session.getSnapshot() == snapshot;
        }

        /** Queues at most one pending rebuild. / 대기 중인 재배치를 최대 하나 예약한다. */
        void request() {
            if (valid() && queued.compareAndSet(false, true) && !session.submitDocumentTask(this)) {
                queued.set(false);
            }
        }

        /**
         * Builds on the document thread and posts dimensions/repaint without waiting on Swing.
         * <p>문서 스레드에서 구성하고 Swing을 기다리지 않고 크기·그리기를 게시한다.
         */
        public void run() {
            queued.set(false);
            if (!valid()) {
                release();
                return;
            }
            boolean retry = false;
            try {
                final int w = width;
                final int h = height;
                final RenderedPage rendered = presentation.render(w, h);
                if (rendered == null) {
                    return;
                }
                // 도중에 변경된 뷰포트나 교체된 페이지의 결과는 게시하지 않고 최신 크기로 다시 배치한다.
                if (!valid() || width != w || height != h) {
                    // 곧 최신 크기로 다시 배치하므로 이 결과로는 로딩 상태를 보고하지 않는다.
                    retry = true;
                    request();
                    return;
                }
                page = rendered;
                PageSelection controller = selection;
                // 새 레이아웃이 게시되면 선택 강조를 새 좌표로 다시 계산한다.
                if (controller != null) {
                    controller.layoutChanged(snapshot);
                }
                PageFind find = finder;
                // 찾기가 활성이면 새 레이아웃의 글에서 다시 찾는다.
                if (find != null) {
                    find.layoutChanged(snapshot);
                }
                // 페이지의 기하 관찰자(IntersectionObserver, ResizeObserver)가 새 배치를 보게 한다.
                holder.layoutPublished();
                SwingUtilities.invokeLater(new Runnable() {
                    /** Publishes only the latest dimensions on the EDT. / EDT에서 최신 크기만 게시한다. */
                    public void run() {
                        if (valid() && page == rendered) {
                            metrics.setContentSize(rendered.getPreferredWidth() > w ? rendered.getPreferredWidth() : 0,
                                    rendered.getPreferredHeight());
                            host.viewChanged();
                        }
                    }
                });
            } finally {
                if (!valid()) {
                    release();
                } else if (!retry) {
                    reportLoad();
                }
            }
        }

        /**
         * Reports the loading state of the page to the session after a layout attempt: how many resources ended of the started ones, and
         * whether the scripts finished. A layout that failed is reported as well, so the progress of the page cannot stay unfinished.
         * <p>
         * 배치를 시도한 뒤 페이지의 로딩 상태를 세션에 보고한다: 시작한 리소스 중 몇 개가 끝났는지와 스크립트가 끝났는지. 배치가 실패한 경우도
         * 보고하므로 페이지의 진행률이 끝나지 않은 채 남을 수 없다.
         */
        private void reportLoad() {
            int[] activity = presentation.resourceActivity();
            session.pageLoadProgress(snapshot, activity[0], activity[1], holder.isLoadComplete());
        }

        /** Releases observers on the document thread; repeated calls are harmless. / 문서 스레드에서 관찰자를 해제하며 반복 호출은 안전하다. */
        void release() {
            presentation.retire();
        }
    }
}
