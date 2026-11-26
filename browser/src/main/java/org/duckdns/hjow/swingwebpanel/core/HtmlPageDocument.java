package org.duckdns.hjow.swingwebpanel.core;

import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.EventTarget;
import org.duckdns.hjow.swingwebpanel.js.host.structured.ClonedValue;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;
import org.duckdns.hjow.swingwebpanel.render.MediaControls;
import org.duckdns.hjow.swingwebpanel.render.MediaControlState;

/**
 * The live document of a page: the DOM tree that the HTML parser built from the page source (plan items P07, P15-01). It is attached to a
 * {@link PageSnapshot} and released with it; the received source text in the snapshot is never replaced by a serialization of this tree.
 * Before confirmation the loader owns the tree; afterwards only the component's document queue may change it.
 * <p>
 * 페이지의 살아 있는 문서: HTML 파서가 페이지 원문으로 만든 DOM 트리(계획 P07, P15-01). {@link PageSnapshot} 에 붙으며 스냅샷과 함께 해제된다.
 * 스냅샷의 수신 원문은 이 트리의 직렬화로 바뀌지 않는다. 확정 전에는 로더가 트리를 소유하며 이후에는 컴포넌트의 문서 큐에서만 바꾼다.
 */
public final class HtmlPageDocument implements PageDocument {

    /**
     * The DOM document, null after {@link #dispose()}.
     * <p>
     * DOM 문서. {@link #dispose()} 뒤에는 null.
     */
    private volatile Document document;

    /** Settings captured without retaining previous pages. / 이전 페이지를 붙잡지 않고 복사한 설정. */
    final PageRequestContext requests;

    /** Optional script host, disposed with this document. / 이 문서와 함께 폐기하는 선택적 스크립트 호스트. */
    private final PageRuntime runtime;
    /** Shared parser/drawing state, optional for standalone loaders. / 파서·그리기 공유 상태이며 독립 로더에서는 선택 사항. */
    private final PagePresentation presentation;

    /** The timed redirect of a <code>meta http-equiv=refresh</code> element, or null. / <code>meta http-equiv=refresh</code> 요소의 예약된 이동. 없으면 null. */
    private volatile MetaRefresh metaRefresh;
    /** The session that carries out the timed redirect of a top-level document. / 최상위 문서의 예약된 이동을 수행하는 세션. */
    private volatile BrowserSession metaSession;
    /** The frame of a document that is the document of a frame, or null for a top-level document. / 프레임의 문서이면 그 프레임. 최상위 문서이면 null. */
    private volatile FrameContext metaFrame;
    /** The timer of the redirect (it polls until the document has loaded, then waits for the seconds). / 이동의 타이머(문서가 로드될 때까지 확인한 뒤 초를 기다린다). */
    private volatile javax.swing.Timer metaTimer;

    /**
     * Creates the holder.
     * <p>
     * 보관 객체를 만든다.
     *
     * @param document the DOM document, not null / DOM 문서(null 불가)
     */
    public HtmlPageDocument(Document document) {
        this(document, new PageRequestContext(null));
    }

    /**
     * Creates a document holder with navigation-specific resource settings.
     * <p>이동별 리소스 설정과 문서 보관 객체를 만든다.
     * @param document parsed DOM / 파싱한 DOM
     * @param requests copied resource settings / 복사한 리소스 설정
     */
    HtmlPageDocument(Document document, PageRequestContext requests) {
        this(document, requests, null, null);
    }

    /** Creates the parsed document with its already installed script host. / 이미 설치한 스크립트 호스트와 파싱한 문서를 만든다.
     * @param document parsed DOM / 파싱한 DOM
     * @param requests resource settings / 리소스 설정
     * @param runtime optional script host / 선택적 스크립트 호스트
     * @param presentation optional page-owned presentation / 선택적 페이지 소유 표현 상태 */
    HtmlPageDocument(Document document, PageRequestContext requests, PageRuntime runtime, PagePresentation presentation) {
        if (document == null) {
            throw new IllegalArgumentException("document must not be null");
        }
        this.document = document;
        this.requests = requests;
        this.runtime = runtime;
        this.presentation = presentation;
    }

    /**
     * A small token that stands for this document in the load progress of the session, so that the session does not keep the document (or
     * its DOM) alive after the page was replaced.
     * <p>
     * 세션의 로딩 진행률에서 이 문서를 대신하는 작은 표식. 페이지가 교체된 뒤 세션이 문서(와 DOM)를 붙잡고 있지 않게 한다.
     */
    private final Object loadToken = new Object();

    /**
     * Returns the token that stands for this document in the load progress.
     * <p>
     * 로딩 진행률에서 이 문서를 대신하는 표식을 반환한다.
     *
     * @return the token / 표식
     */
    Object loadToken() {
        return loadToken;
    }

    /**
     * Tells whether the scripts of this document finished loading (the window <code>load</code> event fired) or can no longer run, so that
     * nothing script-related is left to wait for. A document without a script host has nothing to wait for.
     * <p>
     * 이 문서의 스크립트 로딩이 끝났는지(window <code>load</code> 이벤트가 발생했는지) 또는 더 실행될 수 없는지 알려 준다. 그러면 스크립트와
     * 관련해 더 기다릴 것이 없다. 스크립트 호스트가 없는 문서는 기다릴 것이 없다.
     *
     * @return true if nothing script-related is awaited / 스크립트와 관련해 기다리는 것이 없으면 true
     */
    boolean isLoadComplete() {
        return runtime == null || runtime.isLoadComplete();
    }

    /** Returns page presentation without transferring DOM ownership. / DOM 소유권 이전 없이 페이지 표현을 반환한다.
     * @return presentation or null for an unbound loader / 표현 상태이며 연결되지 않은 로더이면 null */
    PagePresentation getPresentation() {
        return presentation;
    }

    /**
     * Gives the document the timed redirect of its <code>meta http-equiv=refresh</code> element; it starts when the page is confirmed.
     * <p>
     * 문서에 <code>meta http-equiv=refresh</code> 요소의 예약된 이동을 준다. 페이지가 확정되면 시작한다.
     *
     * @param refresh the redirect / 이동
     * @param session the session that navigates a top-level document / 최상위 문서를 이동시키는 세션
     * @param frame   the frame of the document, or null for a top-level document / 문서의 프레임. 최상위 문서이면 null
     */
    void setMetaRefresh(MetaRefresh refresh, BrowserSession session, FrameContext frame) {
        this.metaRefresh = refresh;
        this.metaSession = session;
        this.metaFrame = frame;
    }

    /**
     * Starts the timed redirect of a <code>meta http-equiv=refresh</code> element (HTML5 2014 section 4.2.5.3): when the document has
     * loaded completely and the seconds of the element have passed, the browsing context navigates to the address of the element, with
     * replacement of the history entry, or to the page itself if the element has no address. The redirect is not done while the
     * document is sandboxed without <code>allow-scripts</code> (the sandboxed automatic features flag), for an address that the scheme
     * policy blocks, nor after the document was disposed; it does not depend on the JavaScript setting.
     * <p>
     * <code>meta http-equiv=refresh</code> 요소의 예약된 이동을 시작한다(HTML5 2014 §4.2.5.3): 문서가 완전히 로드되고 요소의 초가 지나면 브라우징 문맥이 이력 항목을 바꾸며
     * 요소의 주소로, 주소가 없으면 페이지 자신으로 이동한다. <code>allow-scripts</code> 없이 샌드박스된 문서(샌드박스된 자동 기능 플래그), 스킴 정책이 막는 주소, 문서를 폐기한 뒤에는
     * 이동하지 않는다. JavaScript 설정과 무관하다.
     */
    private void startMetaRefresh() {
        final MetaRefresh refresh = metaRefresh;
        Document doc = document;
        if (refresh == null || doc == null || metaTimer != null) {
            return;
        }
        FrameContext frame = metaFrame;
        if (frame != null && frame.flags().isSandboxed() && !frame.flags().allowsScripts()) {
            return;
        }
        final javax.swing.Timer timer = new javax.swing.Timer(100, null);
        timer.setRepeats(false);
        // 이 콜백에서 0ms로 다시 시작할 때 앞 이벤트의 정리가 새 알림까지 지우지 않게 한다.
        timer.setCoalesce(false);
        timer.addActionListener(new java.awt.event.ActionListener() {
            /** Whether the document has loaded completely and the wait of the element has started. / 문서가 완전히 로드되어 요소의 기다림이 시작되었는지 여부. */
            private boolean waiting;

            /**
             * Polls until the document has loaded, then waits for the seconds of the element and redirects.
             * <p>
             * 문서가 로드될 때까지 확인한 뒤 요소의 초를 기다리고 이동한다.
             *
             * @param e the timer event / 타이머 이벤트
             */
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (document == null || metaTimer != timer) {
                    return;
                }
                if (!waiting) {
                    if (!isLoadComplete()) {
                        timer.restart();
                        return;
                    }
                    waiting = true;
                    timer.setInitialDelay(Math.min(refresh.getSeconds(), Integer.MAX_VALUE / 1000) * 1000);
                    timer.restart();
                    return;
                }
                metaTimer = null;
                redirect(refresh);
            }
        });
        metaTimer = timer;
        timer.start();
    }

    /**
     * Carries out the timed redirect on the event dispatch thread: resolves the address against the base URL of the document and
     * navigates, replacing the history entry of the page.
     * <p>
     * 예약된 이동을 이벤트 디스패치 스레드에서 수행한다: 주소를 문서의 기준 URL 로 해석해 페이지의 이력 항목을 바꾸며 이동한다.
     *
     * @param refresh the redirect / 이동
     */
    private void redirect(MetaRefresh refresh) {
        Document doc = document;
        if (doc == null) {
            return;
        }
        String current = doc.getUrl();
        String target;
        try {
            target = refresh.getUrl() == null ? current : UrlUtil.resolve(doc.getBaseUri() != null && doc.getBaseUri().length() > 0 ? doc.getBaseUri() : current,
                    refresh.getUrl());
        } catch (RuntimeException e) {
            // 해석할 수 없는 주소는 이동하지 않는다.
            return;
        }
        String source = UrlUtil.SCHEME_FILE.equals(UrlUtil.getScheme(target)) ? doc.getOriginUrl() : current;
        if (target == null || !NavigationPolicy.allowed(source, target)) {
            return;
        }
        FrameContext frame = metaFrame;
        if (frame != null) {
            frame.scriptNavigate(target, true);
        } else if (metaSession != null) {
            metaSession.navigateFromPage(target, true);
        }
    }

    /** Activates timers after the session confirms this exact snapshot. / 세션이 바로 이 스냅샷을 확정한 뒤 타이머를 활성화한다.
     * @param snapshot confirmed snapshot / 확정된 스냅샷 */
    void activate(PageSnapshot snapshot) {
        if (document == null || (runtime != null && !runtime.activate(snapshot))) {
            return;
        }
        startMetaRefresh();
        FrameSet set = frames();
        // 이 문서의 iframe/frame 요소도 이제 중첩 브라우징 문맥을 만들 수 있다.
        if (set != null) {
            set.activate(snapshot);
        }
    }

    /**
     * Returns the script host of this document.
     * <p>
     * 이 문서의 스크립트 호스트를 반환한다.
     *
     * @return the script host, or null if scripts cannot run in this document / 스크립트 호스트. 이 문서에서 스크립트를 실행할 수 없으면 null
     */
    org.duckdns.hjow.swingwebpanel.js.host.PageScripts scripts() {
        return runtime == null ? null : runtime.scripts();
    }

    /**
     * Returns the runtime of this document while it is confirmed in the top-level context.
     * <p>
     * 최상위 문맥에서 확정된 이 문서의 실행 환경을 반환한다.
     *
     * @return the runtime, or null for a document without scripts / 실행 환경. 스크립트 없는 문서이면 null
     */
    PageRuntime runtime() {
        return runtime;
    }

    /**
     * Returns the frames (nested browsing contexts) of this document.
     * <p>
     * 이 문서의 프레임(중첩된 브라우징 문맥)을 반환한다.
     *
     * @return the frame set, or null if the document has no presentation state / 프레임 집합. 문서에 표현 상태가 없으면 null
     */
    FrameSet frames() {
        return presentation == null ? null : presentation.frameSet();
    }

    /**
     * Finds the document that owns a DOM node: this document, or the document of one of its frames (at any depth). Input events of the
     * user go to the document that was hit, so the caller needs the owner of the target element to choose the right script host.
     * <p>
     * DOM 노드를 소유한 문서를 찾는다: 이 문서, 또는 이 문서의 프레임(어느 깊이든)의 문서. 사용자 입력 이벤트는 맞은 문서로 가므로, 호출자는 올바른
     * 스크립트 호스트를 고르기 위해 대상 요소의 소유자가 필요하다.
     *
     * @param node the node or null / 노드 또는 null
     * @return the owner document, this one if it is not found / 소유 문서. 찾지 못하면 이 문서
     */
    HtmlPageDocument ownerOf(org.duckdns.hjow.swingwebpanel.dom.Node node) {
        Document doc = document;
        if (node == null || doc == null) {
            return this;
        }
        Document nodeDoc = node instanceof Document ? (Document) node : node.getOwnerDocument();
        if (nodeDoc == doc) {
            return this;
        }
        FrameSet set = frames();
        HtmlPageDocument found = set == null ? null : set.documentFor(nodeDoc);
        return found != null ? found : this;
    }

    /** Delivers actual input on the document queue, including the script task checkpoint. / 스크립트 작업 체크포인트까지 포함해 문서 큐에서 실제 입력을 전달한다.
     * @param target input target / 입력 대상
     * @param event trusted input / 신뢰 입력
     * @return false if cancelled / 취소되었으면 false */
    boolean dispatchInput(EventTarget target, Event event) {
        return dispatchInput(target, event, false);
    }

    /**
     * Dispatches trusted input on this document, recording an explicit platform paste action.
     * <p>
     * 이 문서에서 신뢰 입력을 전달하고 플랫폼의 명시적인 붙여넣기를 기록한다.
     *
     * @param target input target / 입력 대상
     * @param event host input event / 호스트 입력 이벤트
     * @param pasteAction whether the host recognized its paste shortcut / 호스트가 붙여넣기 단축키로 인식했는지 여부
     * @return true if allowed / 입력이 취소되지 않았으면 true
     */
    boolean dispatchInput(EventTarget target, Event event, boolean pasteAction) {
        return runtime == null ? target.dispatchTrustedEvent(event) : runtime.scripts().dispatchTrustedEvent(target, event, pasteAction);
    }

    /**
     * Applies an action of the user on the control bar of an <code>audio</code> element of this document (see
     * {@link org.duckdns.hjow.swingwebpanel.js.host.PageScripts#controlMedia(Element, int, double)}); a document without scripts has no media elements that
     * play, so nothing happens there.
     * <p>
     * 이 문서의 <code>audio</code> 요소의 컨트롤 막대에서 사용자가 한 동작을 적용한다({@link org.duckdns.hjow.swingwebpanel.js.host.PageScripts#controlMedia(Element, int, double)}
     * 참고). 스크립트가 없는 문서에는 재생하는 미디어 요소가 없으므로 아무 일도 일어나지 않는다.
     *
     * @param target   the <code>audio</code> element / <code>audio</code> 요소
     * @param part     the part of the bar / 막대의 부분
     * @param fraction the position or volume from 0 to 1 / 0 에서 1 까지의 위치 또는 음량
     */
    void controlMedia(Element target, int part, double fraction) {
        if (runtime != null) {
            runtime.scripts().controlMedia(target, part, fraction);
        }
    }

    /**
     * Handles control keys for a focused audio element on the document thread.
     * <p>
     * 문서 스레드에서 포커스된 오디오의 컨트롤 키를 처리한다.
     *
     * @param target the focused audio element / 포커스된 오디오 요소
     * @param key the DOM key value / DOM 키 값
     * @param repeat whether the key is held down / 키를 누른 채로 반복된 입력인지 여부
     * @return true if the control accepts the key / 컨트롤에서 처리하는 키이면 true
     */
    boolean controlAudioKey(Element target, String key, boolean repeat) {
        if (runtime == null || target.getOwnerDocument() != document || !target.isConnected() || !target.hasAttribute("controls")) {
            return false;
        }
        // 누른 채로 있더라도 재생이나 음소거를 한 번만 전환한다.
        boolean play = " ".equals(key) || "Enter".equals(key);
        if (play || "m".equalsIgnoreCase(key)) {
            if (!repeat) {
                controlMedia(target, play ? MediaControls.PART_PLAY : MediaControls.PART_MUTE, 0);
            }
            return true;
        }
        boolean seek = "ArrowLeft".equals(key) || "ArrowRight".equals(key);
        boolean volume = "ArrowUp".equals(key) || "ArrowDown".equals(key);
        if (!seek && !volume) {
            return false;
        }
        MediaControlState state = runtime.scripts().mediaControlSource().getMediaControls(target);
        if (state != null) {
            // 좌우는 5초씩 탐색하고 위아래는 음량을 10%씩 바꾼다. 범위는 기존 컨트롤이 제한한다.
            if (volume) {
                controlMedia(target, MediaControls.PART_VOLUME, state.getVolume() + ("ArrowUp".equals(key) ? 0.1 : -0.1));
            } else if (state.getDuration() > 0 && !Double.isInfinite(state.getDuration())) {
                double seconds = state.getCurrentTime() + ("ArrowRight".equals(key) ? 5 : -5);
                controlMedia(target, MediaControls.PART_SEEK, seconds / state.getDuration());
            }
        }
        return true;
    }

    /**
     * Applies a move to another entry of this document (a navigation that only changes the fragment, or a traversal between the entries that
     * <code>history.pushState</code> made) on the document thread: the document URL becomes the new URL, the target of the fragment is
     * located, the state object of the entry becomes <code>history.state</code>, <code>popstate</code> is fired at the window, and
     * <code>hashchange</code> follows when the fragment changed.
     * <p>
     * 이 문서의 다른 항목으로의 이동(프래그먼트만 바꾸는 이동, 또는 <code>history.pushState</code> 가 만든 항목 사이의 이동)을 문서 스레드에서
     * 적용한다: 문서 URL이 새 URL이 되고, 프래그먼트의 대상을 찾고, 항목의 상태 객체가 <code>history.state</code> 가 되며, window 에
     * <code>popstate</code> 를 보내고, 프래그먼트가 바뀌었으면 이어서 <code>hashchange</code> 를 보낸다.
     *
     * @param oldUrl   the URL before the navigation / 이동 전의 URL
     * @param newUrl   the URL after the navigation / 이동 뒤의 URL
     * @param state    the state object of the entry that the document moved to (null for an entry without one); it becomes
     *                 <code>history.state</code> / 문서가 옮겨 간 항목의 상태 객체(없으면 null). <code>history.state</code> 가 된다
     * @param popState true to fire <code>popstate</code> (the document moved to another entry) / <code>popstate</code> 를 보내려면 true(문서가 다른
     *                 항목으로 옮겨 갔다)
     * @return the border box of the fragment target in document coordinates as x, y, width, height (all zero for the top of the page), or
     *         null if nothing should be scrolled to / 프래그먼트 대상의 border box(문서 좌표, x·y·폭·높이. 페이지 맨 위이면 모두 0). 스크롤할 곳이 없으면
     *         null
     */
    double[] locationChanged(String oldUrl, String newUrl, ClonedValue state, boolean popState) {
        Document doc = document;
        if (doc == null) {
            return null;
        }
        doc.setUrl(newUrl);
        String fragment = UrlUtil.getFragment(newUrl);
        double[] where = null;
        if (fragment != null) {
            String id = UrlUtil.percentDecode(fragment);
            // 빈 프래그먼트와 top 은 페이지 맨 위이고, 그 밖에는 id 가 같은 요소, 없으면 name 이 같은 a 요소가 대상이다.
            if (id.length() == 0 || "top".equalsIgnoreCase(id) && findFragmentTarget(doc, id) == null) {
                where = new double[4];
            } else {
                Element target = findFragmentTarget(doc, id);
                if (target != null && presentation != null) {
                    where = presentation.documentBounds(target);
                    presentation.elementState().setTarget(target);
                    presentation.elementStateChanged();
                }
            }
        }
        String oldFragment = UrlUtil.getFragment(oldUrl);
        boolean changed = oldFragment == null ? fragment != null && fragment.length() > 0 : !oldFragment.equals(fragment);
        if (runtime != null) {
            // 문서가 옮겨 간 항목의 상태가 history.state 가 되고, popstate 와 (프래그먼트가 달라졌으면) hashchange 를 이 순서로 보낸다.
            runtime.scripts().historyTraversed(state, popState, oldUrl, newUrl, changed);
        }
        return where;
    }

    /**
     * Finds the element that a fragment names: the element with that id, otherwise the first <code>a</code> element with that name.
     * <p>
     * 프래그먼트가 가리키는 요소를 찾는다: id 가 같은 요소, 없으면 name 이 같은 첫 <code>a</code> 요소.
     *
     * @param doc      the document / 문서
     * @param fragment the decoded fragment / 디코딩한 프래그먼트
     * @return the element or null / 요소 또는 null
     */
    private static Element findFragmentTarget(Document doc, String fragment) {
        Element byId = doc.getElementById(fragment);
        if (byId != null) {
            return byId;
        }
        java.util.List<Element> anchors = doc.getElementsByTagName("a").toList();
        // name 속성이 같은 첫 앵커를 찾는다.
        for (int i = 0; i < anchors.size(); i++) {
            if (fragment.equals(anchors.get(i).getAttribute("name"))) {
                return anchors.get(i);
            }
        }
        return null;
    }

    /**
     * Returns the DOM document.
     * <p>
     * DOM 문서를 반환한다.
     *
     * @return the document, or null after disposal / 문서. 해제된 뒤에는 null
     */
    public Document getDocument() {
        return document;
    }

    /**
     * Runs the unloading events of the page now (<code>beforeunload</code>, <code>pagehide</code>, <code>unload</code>) and waits for them for at most the given time; a
     * page that has no script host, was never shown or listens to none of them does nothing.
     * <p>
     * 페이지의 언로드 이벤트(<code>beforeunload</code>, <code>pagehide</code>, <code>unload</code>)를 지금 실행하고 주어진 시간까지 기다린다. 스크립트 호스트가 없거나
     * 한 번도 보이지 않았거나 그 어느 것도 듣지 않는 페이지는 아무것도 하지 않는다.
     *
     * @param waitMs the longest wait in milliseconds / 가장 긴 대기 시간(밀리초)
     */
    void unloadNow(long waitMs) {
        if (runtime != null) {
            runtime.unloadNow(waitMs);
        }
    }

    /**
     * Tells the scripts of this document that the component published a new layout, so that the geometry observers (<code>IntersectionObserver</code>,
     * <code>ResizeObserver</code>) look at the page again. It only posts a task and may be called from any thread.
     * <p>
     * 컴포넌트가 새 레이아웃을 게시했음을 이 문서의 스크립트에 알려 기하 관찰자(<code>IntersectionObserver</code>, <code>ResizeObserver</code>)가 페이지를 다시 보게 한다.
     * 작업을 올리기만 하며 어느 스레드에서나 부를 수 있다.
     */
    void layoutPublished() {
        if (runtime != null) {
            runtime.layoutPublished();
        }
    }

    /**
     * Fires <code>focus</code> or <code>blur</code> at the window of this document (the window focus of the component changed).
     * <p>
     * 이 문서의 window 에 <code>focus</code> 또는 <code>blur</code> 를 발생시킨다(컴포넌트의 창 포커스가 바뀌었다).
     *
     * @param gained true if the window gained the focus / 포커스를 얻었으면 true
     */
    void windowFocus(boolean gained) {
        if (runtime != null) {
            runtime.windowFocus(gained);
        }
    }

    /**
     * Releases the reference to the DOM tree so that it can be collected with the page.
     * <p>
     * 페이지와 함께 수거될 수 있도록 DOM 트리에 대한 참조를 놓는다.
     */
    public void dispose() {
        Document doc = document;
        // 예약된 meta refresh 이동을 취소한다.
        javax.swing.Timer timer = metaTimer;
        metaTimer = null;
        if (timer != null) {
            timer.stop();
        }
        if (doc != null && doc.getFormHost() instanceof PageFormHost) {
            // 폼 호스트는 문서가 버려진 뒤에는 편집기와 대화상자를 더 만들지 않는다.
            ((PageFormHost) doc.getFormHost()).close();
        }
        if (runtime != null) {
            runtime.close();
        }
        if (presentation != null) {
            presentation.retire();
        }
        document = null;
    }
}
