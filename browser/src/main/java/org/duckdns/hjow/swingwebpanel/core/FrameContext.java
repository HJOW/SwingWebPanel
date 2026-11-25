package org.duckdns.hjow.swingwebpanel.core;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.duckdns.hjow.swingwebpanel.content.error.ErrorKind;
import org.duckdns.hjow.swingwebpanel.content.error.ErrorPage;
import org.duckdns.hjow.swingwebpanel.content.error.ErrorPageBuilder;
import org.duckdns.hjow.swingwebpanel.core.frames.FrameLimits;
import org.duckdns.hjow.swingwebpanel.core.frames.FrameUrls;
import org.duckdns.hjow.swingwebpanel.core.frames.SandboxFlags;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.js.host.BrowsingContext;
import org.duckdns.hjow.swingwebpanel.js.host.PageScripts;
import org.duckdns.hjow.swingwebpanel.js.host.structured.ClonedValue;
import org.duckdns.hjow.swingwebpanel.net.CancelToken;
import org.duckdns.hjow.swingwebpanel.net.CancelledException;
import org.duckdns.hjow.swingwebpanel.net.CrossOriginPolicy;
import org.duckdns.hjow.swingwebpanel.render.FramePicture;
import org.duckdns.hjow.swingwebpanel.render.FrameScroll;
import org.duckdns.hjow.swingwebpanel.render.RenderedPage;

/**
 * One nested browsing context: the <code>iframe</code> or <code>frame</code> element, the document that it shows (a {@link HtmlPageDocument}
 * with its own DOM, script host, style and layout state), the name, the sandboxing flags, the scroll position and the viewport size, the
 * local history and the state of the load. The context of a frame is owned by the {@link FrameSet} of the document that contains the element
 * and is discarded with the element or with that page.
 * <p>
 * Everything runs on the document queue of the component (which owns all documents of a page), except that the network read of a document
 * runs on a loader thread and only reads bytes; the DOM of the frame is built, and its scripts run, on the document queue.
 * <p>
 * 중첩된 브라우징 문맥 하나: <code>iframe</code> 또는 <code>frame</code> 요소, 그것이 보여 주는 문서(자기 DOM, 스크립트 호스트, 스타일·레이아웃 상태를 가진
 * {@link HtmlPageDocument}), 이름, 샌드박스 플래그, 스크롤 위치와 뷰포트 크기, 지역 이력, 로드 상태. 프레임의 문맥은 요소를 포함한 문서의
 * {@link FrameSet} 이 소유하며 요소 또는 그 페이지와 함께 버려진다.
 * <p>
 * 모든 것은 컴포넌트의 문서 큐(한 페이지의 모든 문서를 소유함)에서 실행된다. 다만 문서의 네트워크 읽기는 읽기 스레드에서 바이트만 읽고, 프레임의 DOM 은
 * 문서 큐에서 만들며 스크립트도 문서 큐에서 실행한다.
 */
final class FrameContext implements BrowsingContext {

    /**
     * The document comes from the <code>src</code> address.
     * <p>
     * 문서가 <code>src</code> 주소에서 온다.
     */
    private static final int KIND_SRC = 0;

    /**
     * The document comes from the <code>srcdoc</code> attribute.
     * <p>
     * 문서가 <code>srcdoc</code> 속성에서 온다.
     */
    private static final int KIND_SRCDOC = 1;

    /**
     * The document is the empty <code>about:blank</code> document.
     * <p>
     * 문서가 빈 <code>about:blank</code> 문서이다.
     */
    private static final int KIND_BLANK = 2;

    /**
     * An entry of the local history of the frame: an address with the state object of <code>pushState</code> and the number of the document
     * that it belongs to (entries of one document differ only by <code>pushState</code> and fragments).
     * <p>
     * 프레임 지역 이력의 항목: 주소, <code>pushState</code> 의 상태 객체, 항목이 속한 문서의 번호(한 문서의 항목은 <code>pushState</code> 와
     * 프래그먼트로만 다르다).
     */
    private static final class Entry {
        /** The address. / 주소. */
        final String url;
        /** The state object or null. / 상태 객체 또는 null. */
        final ClonedValue state;
        /** The number of the document. / 문서의 번호. */
        final int documentId;

        /**
         * Creates an entry.
         * <p>
         * 항목을 만든다.
         *
         * @param url        the address / 주소
         * @param state      the state object or null / 상태 객체 또는 null
         * @param documentId the number of the document / 문서의 번호
         */
        Entry(String url, ClonedValue state, int documentId) {
            this.url = url;
            this.state = state;
            this.documentId = documentId;
        }
    }

    /** The set of the document that contains the frame element. / 프레임 요소를 포함한 문서의 집합. */
    final FrameSet owner;

    /** The <code>iframe</code> or <code>frame</code> element. / <code>iframe</code> 또는 <code>frame</code> 요소. */
    final Element element;

    /** The nesting level: 1 for a frame of the top-level document. / 중첩 단계: 최상위 문서의 프레임이면 1. */
    final int depth;

    /** True for a <code>frame</code> element (of a frameset), false for an <code>iframe</code>. / <code>frame</code> 요소(frameset 의 것)이면 true, <code>iframe</code> 이면 false. */
    private final boolean legacy;

    /** The number of the latest navigation; document queue only. / 최근 이동의 번호. 문서 큐 전용. */
    private int generation;

    /** The cancellation of the latest navigation; document queue only. / 최근 이동의 취소. 문서 큐 전용. */
    private CancelToken token;

    /** The attribute state that was processed last, to find changes. / 마지막으로 처리한 속성 상태. 변경을 찾는 데 쓴다. */
    private String key;

    /** The kind of the document that is being loaded. / 로드 중인 문서의 종류. */
    private int kind = KIND_BLANK;

    /** The address that was navigated to last. / 마지막으로 이동한 주소. */
    private String lastUrl = FrameUrls.ABOUT_BLANK;

    /** The srcdoc text that was navigated to last or null. / 마지막으로 이동한 srcdoc 텍스트. 없으면 null. */
    private String lastSrcdoc;

    /** The form submission that produced the document or null. / 문서를 만든 폼 제출. 없으면 null. */
    private PostData lastPost;

    /** The sandboxing flags of the document that is loaded or being loaded. / 로드했거나 로드 중인 문서의 샌드박스 플래그. */
    private volatile SandboxFlags flags = SandboxFlags.NONE;

    /** The name of the browsing context. / 브라우징 문맥의 이름. */
    private volatile String name = "";

    /** The committed document snapshot or null. / 확정된 문서 스냅샷. 없으면 null. */
    private volatile PageSnapshot content;

    /** The document of the snapshot or null. / 스냅샷의 문서. 없으면 null. */
    private volatile HtmlPageDocument page;

    /** The DOM of the document that is being built for the frame (it is parsed and scripted before it is installed), or null. / 프레임을 위해 만드는 중인 문서의 DOM(설치되기 전에 파싱되고 스크립트가 실행된다). 없으면 null. */
    private volatile Document parsingDocument;

    /** The script host of that document, or null. / 그 문서의 스크립트 호스트. 없으면 null. */
    private volatile PageScripts parsingScripts;

    /** The presentation state that renders the document of the frame, or null. / 프레임의 문서를 렌더링하는 표현 상태. 없으면 null. */
    private volatile PagePresentation shown;

    /** The presentation that the frame made itself for a document that came without one (released with it), or null. / 표현 상태 없이 온 문서를 위해 프레임이 직접 만든 표현 상태(문서와 함께 해제). 없으면 null. */
    private volatile PagePresentation created;

    /** The last rendering of the document, for painting and hit tests. / 그리기와 좌표 조회를 위한 문서의 마지막 렌더링. */
    private volatile RenderedPage lastRendered;

    /** True once the context was discarded. / 문맥이 버려지면 true. */
    private volatile boolean destroyed;

    /** True once nothing more is awaited for the load event of the element (it fired, failed or was discarded). / 요소의 load 이벤트에 대해 더 기다릴 것이 없으면(발생했거나 실패했거나 버려짐) true. */
    private volatile boolean settled;

    /** True once the load event was sent for the current navigation; document queue only. / 현재 이동의 load 이벤트를 보냈으면 true. 문서 큐 전용. */
    private boolean loadFired;

    /** Navigation generation of the installed document, or -1 before the first commit. / 설치된 문서의 이동 세대. 처음 확정 전이면 -1. */
    private int installedGeneration = -1;

    /** The width of the viewport of the frame in CSS pixels. / 프레임 뷰포트의 너비(CSS 픽셀). */
    private volatile int viewW = FrameLimits.DEFAULT_WIDTH;

    /** The height of the viewport of the frame in CSS pixels. / 프레임 뷰포트의 높이(CSS 픽셀). */
    private volatile int viewH = FrameLimits.DEFAULT_HEIGHT;

    /** True if the last picture drew the horizontal scrollbar. / 마지막 그림이 가로 스크롤바를 그렸으면 true. */
    private volatile boolean horizontalBar;

    /** True if the last picture drew the vertical scrollbar. / 마지막 그림이 세로 스크롤바를 그렸으면 true. */
    private volatile boolean verticalBar;

    /** The horizontal scroll offset. / 가로 스크롤 오프셋. */
    private volatile int scrollX;

    /** The vertical scroll offset. / 세로 스크롤 오프셋. */
    private volatile int scrollY;

    /** The local history; document queue only. / 지역 이력. 문서 큐 전용. */
    private final List<Entry> history = new ArrayList<Entry>();

    /** The index of the current entry in {@link #history}. / {@link #history} 에서 현재 항목의 위치. */
    private int historyIndex = -1;

    /** The counter of document numbers. / 문서 번호 계수기. */
    private int documentCounter;

    /** True while the history holds only the initial empty document. / 이력에 처음의 빈 문서만 있는 동안 true. */
    private boolean initialOnly = true;

    /** Coalesces the checks of the load state. / 로드 상태 확인을 합친다. */
    private final AtomicBoolean checkQueued = new AtomicBoolean();

    /** The task that checks the load state. / 로드 상태를 확인하는 작업. */
    private final Runnable checkTask = new Runnable() {
        /** Checks whether the frame finished loading. / 프레임이 로딩을 마쳤는지 확인한다. */
        public void run() {
            checkQueued.set(false);
            checkLoaded();
        }
    };

    /** Repaints the parent when the document of the frame changed. / 프레임의 문서가 바뀌면 부모를 다시 그린다. */
    private final Runnable repaint = new Runnable() {
        /** Asks the parent to rebuild its display list. / 부모에게 디스플레이 리스트를 다시 만들라고 요청한다. */
        public void run() {
            if (!destroyed) {
                owner.presentation.canvasChanged();
            }
        }
    };

    /**
     * Creates the context of a frame element. Call {@link #start(boolean)} afterwards.
     * <p>
     * 프레임 요소의 문맥을 만든다. 이어서 {@link #start(boolean)} 를 부른다.
     *
     * @param owner   the set that contains the element / 요소를 포함한 집합
     * @param element the frame element / 프레임 요소
     */
    FrameContext(FrameSet owner, Element element) {
        this.owner = owner;
        this.element = element;
        this.depth = owner.depth() + 1;
        this.legacy = "frame".equals(element.getLocalName());
        String n = element.getAttribute("name");
        this.name = n == null ? "" : n;
        this.nameAttribute = this.name;
    }

    /**
     * Processes the attributes of the element for the first time (HTML5 REC 2014-10-28 4.7.2).
     * <p>
     * 요소의 속성을 처음으로 처리한다(HTML5 REC 2014-10-28 4.7.2).
     *
     * @param defer true if the call happens inside a script's DOM change / 스크립트의 DOM 변경 안에서 부르면 true
     */
    void start(boolean defer) {
        deferred = defer;
        try {
            processAttributes(true);
        } finally {
            deferred = false;
        }
    }

    /** True while the first navigation runs inside a script's DOM change: a document that needs parsing is then built after the script returns. / 첫 이동이 스크립트의 DOM 변경 안에서 실행되는 동안 true. 파싱이 필요한 문서는 스크립트가 돌아온 뒤에 만든다. */
    private boolean deferred;

    /**
     * Processes the attributes again after a change of <code>src</code>, <code>srcdoc</code>, <code>name</code> or <code>sandbox</code>.
     * <p>
     * <code>src</code>, <code>srcdoc</code>, <code>name</code>, <code>sandbox</code> 가 바뀐 뒤 속성을 다시 처리한다.
     */
    void attributesChanged() {
        // 아직 시작하지 않은 문맥(같은 검사에서 만들어졌고 곧 시작될 문맥)은 건드리지 않는다.
        if (key == null) {
            return;
        }
        String n = element.getAttribute("name");
        // name 속성이 실제로 바뀐 때만 문맥의 이름을 속성에서 가져온다(스크립트가 window.name 으로 바꾼 이름을 되돌리지 않는다).
        String seen = n == null ? "" : n;
        if (!seen.equals(nameAttribute)) {
            nameAttribute = seen;
            name = seen;
        }
        processAttributes(false);
    }

    /** The last value of the <code>name</code> attribute that was applied. / 마지막으로 적용한 <code>name</code> 속성의 값. */
    private String nameAttribute = "";

    /**
     * Chooses what the frame shows from <code>srcdoc</code> (first), <code>src</code> or nothing, and navigates if that changed.
     * <p>
     * <code>srcdoc</code>(우선), <code>src</code>, 또는 아무것도 없음 중에서 프레임이 보여 줄 것을 고르고, 달라졌으면 이동한다.
     *
     * @param first true when the element was just inserted / 요소가 방금 삽입되었으면 true
     */
    private void processAttributes(boolean first) {
        if (destroyed) {
            return;
        }
        String srcdoc = legacy ? null : element.getAttribute("srcdoc");
        String src = element.getAttribute("src");
        String newKey;
        String url;
        // srcdoc 가 src 보다 우선하고, 처음부터 src 가 없으면 빈 문서로 시작한다.
        if (srcdoc != null) {
            newKey = "doc:" + srcdoc;
            url = FrameUrls.ABOUT_SRCDOC;
        } else if (first && src == null) {
            newKey = "initial";
            url = FrameUrls.ABOUT_BLANK;
        } else {
            url = FrameUrls.resolveSrc(owner.document.getBaseUri(), src);
            newKey = "src:" + url;
        }
        if (!first && newKey.equals(key)) {
            return;
        }
        key = newKey;
        navigate(url, srcdoc, null, false, false);
    }

    /**
     * Navigates the frame: cancels the previous load, applies the limits and loads the document. A document without a network read (an empty
     * frame, <code>srcdoc</code>) is built at once, any other document is read on a loader thread and built on the document queue when it
     * arrives.
     * <p>
     * 프레임을 이동시킨다: 이전 로드를 취소하고, 한도를 적용하고, 문서를 로드한다. 네트워크 읽기가 없는 문서(빈 프레임, <code>srcdoc</code>)는 곧바로 만들고,
     * 그 밖의 문서는 읽기 스레드에서 읽어 도착하면 문서 큐에서 만든다.
     *
     * @param url     the absolute address (<code>about:blank</code> or <code>about:srcdoc</code> for the special documents) / 절대 주소
     * @param srcdoc  the srcdoc text, or null / srcdoc 텍스트. 없으면 null
     * @param post    the form submission or null / 폼 제출. 없으면 null
     * @param replace true to replace the current entry of the local history / 지역 이력의 현재 항목을 바꾸려면 true
     * @param force   true to bypass the caches / 캐시를 우회하려면 true
     */
    void navigate(String url, String srcdoc, PostData post, boolean replace, boolean force) {
        if (destroyed || owner.isClosed()) {
            return;
        }
        generation++;
        final int gen = generation;
        if (token != null) {
            token.cancel();
        }
        final CancelToken cancel = new CancelToken();
        token = cancel;
        loadFired = false;
        settled = false;
        lastUrl = url;
        lastSrcdoc = srcdoc;
        lastPost = post;
        kind = srcdoc != null ? KIND_SRCDOC : FrameUrls.ABOUT_BLANK.equals(url) ? KIND_BLANK : KIND_SRC;
        flags = SandboxFlags.parse(element.hasAttribute("sandbox") ? element.getAttribute("sandbox") : null).inherit(owner.effectiveFlags());
        recordEntry(url, replace);
        String problem = null;
        // 한도: 너무 깊은 중첩과 조상이 이미 보여 주는 주소(자기 자신을 임베드하는 페이지)는 로드하지 않고 오류 문서를 보여 준다.
        if (depth > FrameLimits.MAX_DEPTH) {
            problem = "Frames are nested too deeply (the limit is " + FrameLimits.MAX_DEPTH + " levels).";
        } else if (kind == KIND_SRC && FrameUrls.isLoop(url, owner.ancestorUrls())) {
            problem = "A page cannot be shown inside itself or inside one of its own frames.";
        }
        final FrameSet.Tree tree = owner.tree;
        // 페이지를 강제로 새로 고쳤으면(refresh(true)) 그 페이지의 프레임 문서도 캐시를 우회한다.
        final NavigationRequest request = tree.session.frameRequest(tree.requestId, url, force || tree.force, cancel, this, post, flags.allowsScripts());
        String base = owner.document.getBaseUri();
        if (problem != null) {
            commitLater(gen, errorSnapshot(url, problem), request);
        } else if (kind == KIND_SRCDOC) {
            commitLater(gen, PageSnapshot.loaded(FrameUrls.ABOUT_SRCDOC, srcdoc, "text/html", base, 200)
                    .withCrossOriginPolicy(CrossOriginPolicy.inheritedEmbedder(owner.presentation.embedderPolicy())), request);
        } else if (kind == KIND_BLANK) {
            commit(gen, PageSnapshot.loaded(FrameUrls.ABOUT_BLANK, "", "text/html", base, 200)
                    .withCrossOriginPolicy(CrossOriginPolicy.inheritedEmbedder(owner.presentation.embedderPolicy())), request);
        } else if (!tree.session.submitRead(new Runnable() {
            /** Reads the document on a loader thread. / 읽기 스레드에서 문서를 읽는다. */
            public void run() {
                read(gen, request);
            }
        })) {
            cancel.cancel();
        }
    }

    /**
     * Builds the document of a source that needs no network read: at once, or after the running script returns when the navigation began
     * inside a script's DOM change.
     * <p>
     * 네트워크 읽기가 필요 없는 원문의 문서를 만든다: 곧바로, 또는 이동이 스크립트의 DOM 변경 안에서 시작되었으면 실행 중인 스크립트가 돌아온 뒤에.
     *
     * @param gen      the number of the navigation / 이동 번호
     * @param snapshot the source / 원문
     * @param request  the request / 요청
     */
    private void commitLater(final int gen, final PageSnapshot snapshot, final NavigationRequest request) {
        if (!deferred) {
            commit(gen, snapshot, request);
        } else if (!owner.tree.session.submitDocumentTask(new Runnable() {
            /** Builds the document on the document queue. / 문서 큐에서 문서를 만든다. */
            public void run() {
                commit(gen, snapshot, request);
            }
        })) {
            snapshot.dispose();
        }
    }

    /**
     * Reads the source of a document on a loader thread and hands it to the document queue. A response that has to be saved goes to the
     * download handling of the component and leaves the frame as it is.
     * <p>
     * 읽기 스레드에서 문서의 원문을 읽어 문서 큐에 넘긴다. 저장해야 하는 응답은 컴포넌트의 다운로드 처리로 가며 프레임은 그대로 둔다.
     *
     * @param gen     the number of the navigation / 이동 번호
     * @param request the request / 요청
     */
    private void read(final int gen, final NavigationRequest request) {
        final FrameSet.Tree tree = owner.tree;
        PageSnapshot snapshot;
        try {
            snapshot = tree.loader.loadFrameSource(request);
            if (snapshot == null) {
                throw new IllegalStateException("page loader returned null");
            }
        } catch (CancelledException e) {
            return;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        } catch (final DownloadRequiredException e) {
            // 저장할 응답이면 프레임은 그대로 두고 다운로드 처리에 넘긴다.
            tree.session.submitDocumentTask(new Runnable() {
                /** Starts the download on the document queue. / 문서 큐에서 다운로드를 시작한다. */
                public void run() {
                    if (!destroyed && gen == generation) {
                        tree.session.frameDownload(request, e);
                        finishWithoutDocument();
                    }
                }
            });
            return;
        } catch (Throwable t) {
            if (request.getCancelToken().isCancelled()) {
                return;
            }
            snapshot = errorSnapshot(request.getUrl(), String.valueOf(t));
        }
        final PageSnapshot loaded = snapshot;
        // 문서의 파싱과 스크립트 실행은 문서 큐에서 한다.
        if (!tree.session.submitDocumentTask(new Runnable() {
            /** Builds the document on the document queue. / 문서 큐에서 문서를 만든다. */
            public void run() {
                commit(gen, loaded, request);
            }
        })) {
            loaded.dispose();
        }
    }

    /**
     * Tells the frame about the document that is about to be parsed: its scripts already run as the window of the frame, so the context
     * answers with that document (and its window) until the document is installed.
     * <p>
     * 곧 파싱할 문서를 프레임에 알린다: 그 스크립트는 이미 프레임의 window 로 실행되므로, 문서가 설치될 때까지 문맥은 그 문서(와 window)로 답한다.
     *
     * @param document the DOM of the new document / 새 문서의 DOM
     * @param scripts  its script host / 그 스크립트 호스트
     */
    void parsingStarted(Document document, PageScripts scripts) {
        parsingDocument = document;
        parsingScripts = scripts;
    }

    /**
     * Builds the document of a source that arrived and installs it, unless the frame navigated again or was discarded meanwhile.
     * <p>
     * 도착한 원문의 문서를 만들어 설치한다. 그 사이 프레임이 다시 이동했거나 버려졌으면 설치하지 않는다.
     *
     * @param gen      the number of the navigation / 이동 번호
     * @param snapshot the source / 원문
     * @param request  the request / 요청
     */
    private void commit(int gen, PageSnapshot snapshot, NavigationRequest request) {
        if (destroyed || gen != generation || owner.isClosed()) {
            snapshot.dispose();
            return;
        }
        PageSnapshot result;
        try {
            result = owner.tree.loader.attachFrame(snapshot, request);
        } catch (RuntimeException e) {
            snapshot.dispose();
            result = null;
        } finally {
            parsingDocument = null;
            parsingScripts = null;
        }
        if (result == null) {
            // 문서를 만들지 못하면 그 이유를 보여 주는 오류 문서로 대신한다.
            PageSnapshot error = errorSnapshot(request.getUrl(), "The document of the frame could not be built.");
            try {
                result = owner.tree.loader.attachFrame(error, request);
            } catch (RuntimeException e) {
                error.dispose();
                finishWithoutDocument();
                return;
            } finally {
                parsingDocument = null;
                parsingScripts = null;
            }
        }
        if (destroyed || gen != generation || owner.isClosed()) {
            result.dispose();
            return;
        }
        install(gen, result);
    }

    /**
     * Makes a built document the content of the frame, disposes the previous one and activates the new one.
     * <p>
     * 만든 문서를 프레임의 내용으로 삼고, 이전 문서를 해제하고, 새 문서를 활성화한다.
     *
     * @param gen the completed navigation generation / 완료된 이동 세대
     * @param result the snapshot with the document / 문서가 붙은 스냅샷
     */
    private void install(int gen, PageSnapshot result) {
        PageSnapshot old = content;
        PagePresentation oldCreated = created;
        content = result;
        HtmlPageDocument p = result.getDocument() instanceof HtmlPageDocument ? (HtmlPageDocument) result.getDocument() : null;
        page = p;
        installedGeneration = gen;
        lastRendered = null;
        scrollX = 0;
        scrollY = 0;
        created = null;
        shown = null;
        if (p != null) {
            PagePresentation presentation = p.getPresentation();
            // 일반 텍스트 같은 문서는 표현 상태 없이 만들어지므로 프레임이 직접 만들어 그린다(최상위의 뷰가 하는 것과 같다).
            if (presentation == null && p.getDocument() != null) {
                presentation = owner.tree.view.createPresentation(p.getDocument(), p.requests);
                presentation.parsingFinished();
                created = presentation;
            }
            shown = presentation;
            if (presentation != null) {
                presentation.setInvalidation(repaint);
                presentation.setLoadObserver(new Runnable() {
                    /** Queues a check of the load state. / 로드 상태 확인을 예약한다. */
                    public void run() {
                        postCheck();
                    }
                });
            }
            // 문서의 스크립트 타이머와 이 문서의 프레임이 이제 동작할 수 있다.
            p.activate(owner.topSnapshot());
        }
        if (old != null) {
            disposeLater(old);
        }
        if (oldCreated != null) {
            oldCreated.retire();
        }
        owner.presentation.canvasChanged();
        checkLoaded();
    }

    /**
     * Ends a navigation that ends without a new document (a download): the frame keeps its document and nothing is awaited any more.
     * <p>
     * 새 문서 없이 끝나는 이동(다운로드)을 끝낸다: 프레임은 문서를 그대로 두고 더는 기다리는 것이 없다.
     */
    private void finishWithoutDocument() {
        loadFired = true;
        settled = true;
        owner.presentation.resourceChanged(false);
    }

    /**
     * Builds the snapshot of a document that tells why a frame cannot show its address.
     * <p>
     * 프레임이 주소를 보여 줄 수 없는 이유를 알려 주는 문서의 스냅샷을 만든다.
     *
     * @param url     the address / 주소
     * @param message the explanation / 설명
     * @return the snapshot / 스냅샷
     */
    private static PageSnapshot errorSnapshot(String url, String message) {
        ErrorPage page = ErrorPageBuilder.build(ErrorKind.LOAD_FAILED, url, 0, "Frame not loaded", message, "");
        return PageSnapshot.error(url, page.getHtml(), page.getMimeType(), 0);
    }

    /**
     * Disposes a snapshot on the cleanup worker, so that nothing is waited for on the document queue.
     * <p>
     * 문서 큐에서 기다리지 않도록 정리 작업자에서 스냅샷을 해제한다.
     *
     * @param snapshot the snapshot / 스냅샷
     */
    private static void disposeLater(final PageSnapshot snapshot) {
        PageCleanup.submit(new Runnable() {
            /** Disposes the snapshot. / 스냅샷을 해제한다. */
            public void run() {
                snapshot.dispose();
            }
        });
    }

    /**
     * Prepares the DOM of a document of this frame before it is parsed: the special documents take their base address and origin from the
     * creator, and a sandbox without <code>allow-same-origin</code> gives the document an opaque origin. Called on the document queue.
     * <p>
     * 이 프레임의 문서를 파싱하기 전에 DOM 을 준비한다: 특수 문서는 기준 주소와 오리진을 생성자에게서 가져오고, <code>allow-same-origin</code> 이
     * 없는 샌드박스는 문서에 불투명 오리진을 준다. 문서 큐에서 부른다.
     *
     * @param doc the new DOM document / 새 DOM 문서
     */
    void prepareDocument(Document doc) {
        // 문서의 본문 여백 힌트가 이 프레임 요소의 marginwidth/marginheight 를 볼 수 있게 한다.
        doc.setContainerFrame(element);
        // about:blank 와 srcdoc 문서의 오리진은 생성자 문서의 별칭이고 기준 주소도 생성자의 것이다.
        if (kind == KIND_SRCDOC || kind == KIND_BLANK) {
            doc.setUrl(kind == KIND_SRCDOC ? FrameUrls.ABOUT_SRCDOC : FrameUrls.ABOUT_BLANK);
            doc.setBaseUri(owner.document.getBaseUri());
            doc.setOriginUrl(owner.document.getOriginUrl());
        }
        // 샌드박스에 allow-same-origin 이 없으면 문서의 오리진은 다른 모든 오리진과 다르다.
        if (!flags.allowsSameOrigin()) {
            doc.setOriginUrl(FrameUrls.ABOUT_BLANK);
        }
    }

    /**
     * Navigates for a form of the document of this frame. Call on the document queue.
     * <p>
     * 이 프레임의 문서의 폼을 위해 이동한다. 문서 큐에서 부른다.
     *
     * @param submission the submission / 제출 정보
     */
    void submitForm(org.duckdns.hjow.swingwebpanel.dom.html.FormSubmission submission) {
        if (destroyed || submission == null || submission.getUrl() == null) {
            return;
        }
        String url = submission.getUrl().trim();
        boolean post = org.duckdns.hjow.swingwebpanel.dom.html.FormSubmission.POST.equals(submission.getMethod());
        PostData data = null;
        // 폼 본문은 POST 로 보내며 응답한 문서가 제출을 기억한다.
        if (post) {
            byte[] body = submission.getBody();
            data = new PostData(submission.getContentType(), body == null ? new byte[0] : body, sourceUrl());
        }
        if (NavigationPolicy.allowed(sourceUrl(), url)) {
            navigate(url, null, data, false, false);
        }
    }

    /**
     * Returns the address of the network document that this frame belongs to: its own address, or that of the nearest ancestor when its
     * document is <code>about:blank</code> or <code>about:srcdoc</code>. The navigation policy uses it.
     * <p>
     * 이 프레임이 속한 네트워크 문서의 주소를 반환한다: 자기 주소, 문서가 <code>about:blank</code> 나 <code>about:srcdoc</code> 이면 가장 가까운 조상의
     * 주소. 이동 정책이 쓴다.
     *
     * @return the address / 주소
     */
    String sourceUrl() {
        HtmlPageDocument p = page;
        String u = p == null || p.getDocument() == null ? null : p.getDocument().getUrl();
        if (u != null && !u.startsWith("about:")) {
            return u;
        }
        return owner.sourceUrl();
    }

    /**
     * Returns the picture that the renderer paints for this frame in a content box of the given size: it lays the document out for that size,
     * keeps the scroll offset inside the document and decides about the scrollbars.
     * <p>
     * 렌더러가 주어진 크기의 콘텐츠 상자에 이 프레임을 위해 그리는 그림을 반환한다: 문서를 그 크기로 배치하고, 스크롤 오프셋을 문서 안으로 제한하고,
     * 스크롤바를 정한다.
     *
     * @param w the width of the content box / 콘텐츠 상자의 너비
     * @param h the height of the content box / 콘텐츠 상자의 높이
     * @return the picture / 그림
     */
    FramePicture picture(double w, double h) {
        int vw = Math.max(1, (int) Math.round(w));
        int vh = Math.max(1, (int) Math.round(h));
        viewW = vw;
        viewH = vh;
        RenderedPage rendered = null;
        PagePresentation presentation = shown;
        if (presentation != null) {
            rendered = presentation.render(vw, vh);
        }
        // 표현 상태가 이미 닫혔으면 마지막으로 렌더링한 그림을 그대로 쓴다.
        if (rendered == null) {
            rendered = lastRendered;
        } else {
            lastRendered = rendered;
        }
        if (rendered == null) {
            return new FramePicture(null, 0, 0, false, false);
        }
        int cw = rendered.getPreferredWidth();
        int ch = rendered.getPreferredHeight();
        scrollX = FrameScroll.clamp(scrollX, cw, vw);
        scrollY = FrameScroll.clamp(scrollY, ch, vh);
        String mode = element.getAttribute("scrolling");
        boolean none = mode != null && ("no".equalsIgnoreCase(mode) || "0".equals(mode) || "false".equalsIgnoreCase(mode));
        boolean always = mode != null && ("yes".equalsIgnoreCase(mode) || "1".equals(mode) || "scroll".equalsIgnoreCase(mode));
        boolean horizontal = !none && (always || cw > vw);
        boolean vertical = !none && (always || ch > vh);
        horizontalBar = horizontal;
        verticalBar = vertical;
        return new FramePicture(rendered, scrollX, scrollY, horizontal, vertical);
    }

    /**
     * Returns the document of the frame.
     * <p>
     * 프레임의 문서를 반환한다.
     *
     * @return the document or null / 문서 또는 null
     */
    HtmlPageDocument page() {
        return page;
    }

    /**
     * Returns the last rendering of the document of the frame (the one that was painted).
     * <p>
     * 프레임의 문서의 마지막 렌더링(그려진 것)을 반환한다.
     *
     * @return the rendering or null / 렌더링. 없으면 null
     */
    RenderedPage lastRendered() {
        return lastRendered;
    }

    /**
     * Returns the sandboxing flags that apply to the document of the frame.
     * <p>
     * 프레임의 문서에 적용되는 샌드박스 플래그를 반환한다.
     *
     * @return the flags / 플래그
     */
    SandboxFlags flags() {
        return flags;
    }

    /**
     * Returns the name of the browsing context (<code>window.name</code>).
     * <p>
     * 브라우징 문맥의 이름(<code>window.name</code>)을 반환한다.
     *
     * @return the name, empty if none / 이름. 없으면 빈 문자열
     */
    String name() {
        return name;
    }

    /**
     * Tells whether the load event of the element is still awaited.
     * <p>
     * 요소의 load 이벤트를 아직 기다리는지 알려 준다.
     *
     * @return true while loading / 로딩 중이면 true
     */
    boolean isPending() {
        return !settled && !destroyed;
    }

    /**
     * Returns the width of the viewport of the frame.
     * <p>
     * 프레임 뷰포트의 너비를 반환한다.
     *
     * @return the width in CSS pixels / 너비(CSS 픽셀)
     */
    int viewportWidth() {
        return viewW;
    }

    /**
     * Returns the height of the viewport of the frame.
     * <p>
     * 프레임 뷰포트의 높이를 반환한다.
     *
     * @return the height in CSS pixels / 높이(CSS 픽셀)
     */
    int viewportHeight() {
        return viewH;
    }

    /**
     * Returns the scroll position of the frame.
     * <p>
     * 프레임의 스크롤 위치를 반환한다.
     *
     * @return a new point / 새 점
     */
    Point scrollPosition() {
        return new Point(scrollX, scrollY);
    }

    /**
     * Scrolls the frame by a distance, as far as its document allows, and repaints it when the position changed.
     * <p>
     * 프레임을 문서가 허용하는 만큼 주어진 거리만큼 스크롤하고, 위치가 바뀌면 다시 그린다.
     *
     * @param dx the horizontal distance / 가로 거리
     * @param dy the vertical distance / 세로 거리
     * @return true if the position changed / 위치가 바뀌었으면 true
     */
    boolean scrollBy(int dx, int dy) {
        return scrollTo(scrollX + dx, scrollY + dy);
    }

    /**
     * Scrolls the frame to a position, limited to what its document allows, and repaints it when the position changed.
     * <p>
     * 프레임을 문서가 허용하는 범위로 제한한 위치로 스크롤하고, 위치가 바뀌면 다시 그린다.
     *
     * @param x the wanted horizontal position / 원하는 가로 위치
     * @param y the wanted vertical position / 원하는 세로 위치
     * @return true if the position changed / 위치가 바뀌었으면 true
     */
    boolean scrollTo(int x, int y) {
        RenderedPage r = lastRendered;
        int nx = r == null ? Math.max(0, x) : FrameScroll.clamp(x, r.getPreferredWidth(), viewW);
        int ny = r == null ? Math.max(0, y) : FrameScroll.clamp(y, r.getPreferredHeight(), viewH);
        if (nx == scrollX && ny == scrollY) {
            return false;
        }
        scrollX = nx;
        scrollY = ny;
        repaint.run();
        return true;
    }

    /**
     * Tells whether the frame can scroll in the direction of a wheel movement (so that the frame, not the enclosing scroll pane, takes it).
     * <p>
     * 휠 움직임의 방향으로 프레임이 스크롤할 수 있는지 알려 준다(그러면 둘러싼 스크롤 창이 아니라 프레임이 받는다).
     *
     * @param dx the horizontal distance / 가로 거리
     * @param dy the vertical distance / 세로 거리
     * @return true if the position would change / 위치가 바뀔 수 있으면 true
     */
    boolean canScrollBy(int dx, int dy) {
        RenderedPage r = lastRendered;
        if (r == null) {
            return false;
        }
        int nx = FrameScroll.clamp(scrollX + dx, r.getPreferredWidth(), viewW);
        int ny = FrameScroll.clamp(scrollY + dy, r.getPreferredHeight(), viewH);
        return nx != scrollX || ny != scrollY;
    }

    /** No scrollbar. / 스크롤바 없음. */
    static final int BAR_NONE = 0;

    /** The thumb of the vertical scrollbar. / 세로 스크롤바의 썸. */
    static final int BAR_VERTICAL_THUMB = 1;

    /** The track of the vertical scrollbar outside its thumb. / 세로 스크롤바 트랙 중 썸 밖. */
    static final int BAR_VERTICAL_TRACK = 2;

    /** The thumb of the horizontal scrollbar. / 가로 스크롤바의 썸. */
    static final int BAR_HORIZONTAL_THUMB = 3;

    /** The track of the horizontal scrollbar outside its thumb. / 가로 스크롤바 트랙 중 썸 밖. */
    static final int BAR_HORIZONTAL_TRACK = 4;

    /**
     * Tells which part of the overlay scrollbars is at a point of the content box (the scrollbars that the last picture drew).
     * <p>
     * 콘텐츠 상자의 한 점에 오버레이 스크롤바의 어느 부분이 있는지 알려 준다(마지막 그림이 그린 스크롤바).
     *
     * @param cx x in the content box / 콘텐츠 상자 안의 x
     * @param cy y in the content box / 콘텐츠 상자 안의 y
     * @return one of the BAR constants / BAR 상수 중 하나
     */
    int scrollbarAt(double cx, double cy) {
        RenderedPage r = lastRendered;
        if (r == null) {
            return BAR_NONE;
        }
        int bar = FrameScroll.BAR;
        // 세로 스크롤바는 오른쪽 가장자리의 띠이고 가로 스크롤바는 아래 가장자리의 띠이다.
        if (verticalBar && cx >= viewW - bar && cy < viewH - (horizontalBar ? bar : 0)) {
            double[] t = FrameScroll.thumb(viewH - (horizontalBar ? bar : 0), r.getPreferredHeight(), scrollY);
            return cy >= t[0] && cy < t[0] + t[1] ? BAR_VERTICAL_THUMB : BAR_VERTICAL_TRACK;
        }
        if (horizontalBar && cy >= viewH - bar && cx < viewW - (verticalBar ? bar : 0)) {
            double[] t = FrameScroll.thumb(viewW - (verticalBar ? bar : 0), r.getPreferredWidth(), scrollX);
            return cx >= t[0] && cx < t[0] + t[1] ? BAR_HORIZONTAL_THUMB : BAR_HORIZONTAL_TRACK;
        }
        return BAR_NONE;
    }

    /**
     * Returns where the thumb of a scrollbar starts along its track.
     * <p>
     * 스크롤바의 썸이 트랙을 따라 어디서 시작하는지 반환한다.
     *
     * @param vertical true for the vertical scrollbar / 세로 스크롤바이면 true
     * @return the start in CSS pixels / 시작 위치(CSS 픽셀)
     */
    double thumbStart(boolean vertical) {
        RenderedPage r = lastRendered;
        if (r == null) {
            return 0;
        }
        int bar = FrameScroll.BAR;
        return vertical ? FrameScroll.thumb(viewH - (horizontalBar ? bar : 0), r.getPreferredHeight(), scrollY)[0]
                : FrameScroll.thumb(viewW - (verticalBar ? bar : 0), r.getPreferredWidth(), scrollX)[0];
    }

    /**
     * Scrolls so that the thumb of a scrollbar is at a start position (dragging the thumb).
     * <p>
     * 스크롤바의 썸이 시작 위치에 오도록 스크롤한다(썸 끌기).
     *
     * @param vertical   true for the vertical scrollbar / 세로 스크롤바이면 true
     * @param thumbStart the wanted start of the thumb along the track / 트랙을 따른 썸의 원하는 시작 위치
     */
    void scrollToThumb(boolean vertical, double thumbStart) {
        RenderedPage r = lastRendered;
        if (r == null) {
            return;
        }
        int bar = FrameScroll.BAR;
        if (vertical) {
            scrollTo(scrollX, FrameScroll.offsetForThumb(viewH - (horizontalBar ? bar : 0), r.getPreferredHeight(), thumbStart));
        } else {
            scrollTo(FrameScroll.offsetForThumb(viewW - (verticalBar ? bar : 0), r.getPreferredWidth(), thumbStart), scrollY);
        }
    }

    /**
     * Queues a check of the load state on the document queue (callable from any thread).
     * <p>
     * 로드 상태 확인을 문서 큐에 예약한다(어느 스레드에서나 부를 수 있다).
     */
    private void postCheck() {
        if (!destroyed && checkQueued.compareAndSet(false, true) && !owner.tree.session.submitDocumentTask(checkTask)) {
            checkQueued.set(false);
        }
    }

    /**
     * Called when the window <code>load</code> event of the document of the frame fired.
     * <p>
     * 프레임의 문서의 window <code>load</code> 이벤트가 발생했을 때 불린다.
     */
    void scriptsLoaded() {
        postCheck();
    }

    /**
     * Sends the <code>load</code> event to the frame element once the document of the frame and everything that it started (resources and
     * frames) finished, and lets the parent page re-evaluate its own load state.
     * <p>
     * 프레임의 문서와 그것이 시작한 모든 것(리소스와 프레임)이 끝나면 프레임 요소에 <code>load</code> 이벤트를 한 번 보내고, 부모 페이지가 자기 로드
     * 상태를 다시 판단하게 한다.
     */
    void checkLoaded() {
        // 새 문서를 기다리는 동안 이전 문서의 완료 상태로 load를 보내지 않는다.
        if (destroyed || loadFired || installedGeneration != generation) {
            return;
        }
        HtmlPageDocument p = page;
        // 문서가 아직 없으면(첫 이동 도중) 기다린다. 문서 없이 끝난 응답은 이 검사 전에 settled 로 표시한다.
        if (p == null) {
            return;
        }
        PagePresentation presentation = shown;
        boolean resources = presentation == null || !presentation.hasPendingResources();
        if (!p.isLoadComplete() || !resources) {
            return;
        }
        loadFired = true;
        settled = true;
        Event load = new Event("load", false, false);
        // 부모 문서의 스크립트 호스트가 있으면 그것으로 보내 마이크로태스크와 관찰자가 이어서 실행되게 한다.
        if (owner.runtime != null) {
            owner.runtime.scripts().dispatchTrustedEvent(element, load);
        } else {
            element.dispatchTrustedEvent(load);
        }
        owner.presentation.resourceChanged(false);
    }

    /**
     * Records a new entry of the local history for a navigation (replacing the initial empty document, or the current entry on request).
     * <p>
     * 이동을 위해 지역 이력에 새 항목을 기록한다(처음의 빈 문서나, 요청하면 현재 항목을 바꾼다).
     *
     * @param url     the address / 주소
     * @param replace true to replace the current entry / 현재 항목을 바꾸려면 true
     */
    private void recordEntry(String url, boolean replace) {
        Entry entry = new Entry(url, null, ++documentCounter);
        if ((replace || initialOnly) && historyIndex >= 0) {
            history.set(historyIndex, entry);
        } else {
            // 현재 위치 뒤의 항목은 버리고 새 항목을 더한다.
            while (history.size() > historyIndex + 1) {
                history.remove(history.size() - 1);
            }
            history.add(entry);
            historyIndex = history.size() - 1;
            // 한도를 넘으면 가장 오래된 항목을 버린다.
            if (history.size() > FrameLimits.MAX_HISTORY) {
                history.remove(0);
                historyIndex--;
            }
        }
        initialOnly = FrameUrls.ABOUT_BLANK.equals(url) && history.size() == 1;
    }

    /**
     * Returns the number of entries of the local history (<code>history.length</code> of the frame).
     * <p>
     * 지역 이력의 항목 수를 반환한다(프레임의 <code>history.length</code>).
     *
     * @return the number of entries, at least 1 / 항목 수(1 이상)
     */
    int historyLength() {
        return Math.max(1, history.size());
    }

    /**
     * Navigates for a script of the document of the frame (<code>location.href</code> and friends). The scheme policy was already checked.
     * A change of the fragment only is a navigation inside the document: it adds an entry, scrolls and sends <code>hashchange</code>.
     * <p>
     * 프레임의 문서의 스크립트를 위해 이동한다(<code>location.href</code> 등). 스킴 정책은 이미 확인했다. 프래그먼트만 바꾸는 것은 문서 안의 이동으로
     * 항목을 더하고, 스크롤하고, <code>hashchange</code> 를 보낸다.
     *
     * @param url     the absolute address / 절대 주소
     * @param replace true to replace the entry / 항목을 바꾸려면 true
     */
    void scriptNavigate(String url, boolean replace) {
        if (destroyed) {
            return;
        }
        HtmlPageDocument p = page;
        String current = p == null || p.getDocument() == null ? null : p.getDocument().getUrl();
        // 같은 문서 안의 이동(프래그먼트만 다름)은 다시 요청하지 않는다.
        if (current != null && NavigationPolicy.hasFragment(url) && NavigationPolicy.sameDocument(current, url) && !current.startsWith("about:")) {
            Entry from = historyIndex >= 0 ? history.get(historyIndex) : null;
            Entry entry = new Entry(url, null, from == null ? ++documentCounter : from.documentId);
            if (replace && historyIndex >= 0) {
                history.set(historyIndex, entry);
            } else {
                // 현재 위치 뒤의 항목은 버리고 새 항목을 더한다.
                while (history.size() > historyIndex + 1) {
                    history.remove(history.size() - 1);
                }
                history.add(entry);
                historyIndex = history.size() - 1;
            }
            moveInsideDocument(current, url, null, false);
            return;
        }
        navigate(url, null, null, replace, false);
    }

    /**
     * Applies a move to another address of the same document: the document URL changes, the target of the fragment is scrolled to and the
     * events are sent.
     * <p>
     * 같은 문서의 다른 주소로의 이동을 적용한다: 문서 URL 이 바뀌고, 프래그먼트의 대상으로 스크롤하고, 이벤트를 보낸다.
     *
     * @param oldUrl   the address before / 이전 주소
     * @param newUrl   the address after / 이후 주소
     * @param state    the state object or null / 상태 객체 또는 null
     * @param popState true to send popstate / popstate 를 보내려면 true
     */
    private void moveInsideDocument(String oldUrl, String newUrl, ClonedValue state, boolean popState) {
        HtmlPageDocument p = page;
        if (p == null) {
            return;
        }
        double[] where = p.locationChanged(oldUrl, newUrl, state, popState);
        if (where != null) {
            scrollTo((int) Math.round(where[0]), (int) Math.round(where[1]));
        }
    }

    /**
     * Reloads the document of the frame for a script (<code>location.reload()</code>).
     * <p>
     * 스크립트를 위해 프레임의 문서를 다시 읽는다(<code>location.reload()</code>).
     */
    void scriptReload() {
        navigate(lastUrl, lastSrcdoc, lastPost, true, false);
    }

    /**
     * Records <code>history.pushState</code> or <code>replaceState</code> of the document of the frame in the local history.
     * <p>
     * 프레임의 문서의 <code>history.pushState</code>, <code>replaceState</code> 를 지역 이력에 기록한다.
     *
     * @param state   the serialized state or null / 직렬화된 상태 또는 null
     * @param url     the new absolute address / 새 절대 주소
     * @param replace true for <code>replaceState</code> / <code>replaceState</code> 이면 true
     */
    void scriptUpdateHistory(ClonedValue state, String url, boolean replace) {
        Entry from = historyIndex >= 0 ? history.get(historyIndex) : null;
        Entry entry = new Entry(url, state, from == null ? documentCounter : from.documentId);
        if (replace && historyIndex >= 0) {
            history.set(historyIndex, entry);
            return;
        }
        // 현재 위치 뒤의 항목은 버리고 새 항목을 더한다.
        while (history.size() > historyIndex + 1) {
            history.remove(history.size() - 1);
        }
        history.add(entry);
        historyIndex = history.size() - 1;
        if (history.size() > FrameLimits.MAX_HISTORY) {
            history.remove(0);
            historyIndex--;
        }
    }

    /**
     * Moves in the local history of the frame (<code>history.go</code>, <code>back</code> and <code>forward</code> of the document of the
     * frame). An entry of the same document restores its state and sends <code>popstate</code>; an entry of another document loads it
     * again. An impossible distance does nothing.
     * <p>
     * 프레임의 지역 이력에서 이동한다(프레임의 문서의 <code>history.go</code>, <code>back</code>, <code>forward</code>). 같은 문서의 항목은 상태를
     * 복원하고 <code>popstate</code> 를 보내며, 다른 문서의 항목은 다시 로드한다. 불가능한 거리는 아무것도 하지 않는다.
     *
     * @param delta the number of entries, negative for back / 칸 수(뒤로는 음수)
     */
    void scriptHistoryGo(int delta) {
        if (destroyed) {
            return;
        }
        if (delta == 0) {
            scriptReload();
            return;
        }
        int target = historyIndex + delta;
        if (target < 0 || target >= history.size()) {
            return;
        }
        Entry from = history.get(historyIndex);
        Entry to = history.get(target);
        historyIndex = target;
        initialOnly = false;
        if (to.documentId == from.documentId) {
            moveInsideDocument(from.url, to.url, to.state, true);
            return;
        }
        // 다른 문서의 항목은 그 주소를 다시 로드하되 새 항목을 더하지 않는다.
        navigateWithoutEntry(to.url);
    }

    /**
     * Loads an address for a history traversal: like {@link #navigate} but the local history keeps its entries.
     * <p>
     * 이력 이동을 위해 주소를 로드한다: {@link #navigate} 와 같지만 지역 이력의 항목은 그대로 둔다.
     *
     * @param url the address of the entry / 항목의 주소
     */
    private void navigateWithoutEntry(String url) {
        int index = historyIndex;
        List<Entry> saved = new ArrayList<Entry>(history);
        boolean wasInitial = initialOnly;
        navigate(url, null, null, true, false);
        // navigate 가 바꾼 항목을 이동 전의 이력으로 되돌리되 현재 위치의 주소만 갱신한다.
        history.clear();
        history.addAll(saved);
        historyIndex = index;
        initialOnly = wasInitial;
    }

    /**
     * Returns the script host of the document that the frame shows now.
     * <p>
     * 프레임이 지금 보여 주는 문서의 스크립트 호스트를 반환한다.
     *
     * @return the script host or null / 스크립트 호스트 또는 null
     */
    public PageScripts getScripts() {
        PageScripts building = parsingScripts;
        if (building != null) {
            return building;
        }
        HtmlPageDocument p = page;
        return p == null ? null : p.scripts();
    }

    /**
     * Returns the DOM document that the frame shows now.
     * <p>
     * 프레임이 지금 보여 주는 DOM 문서를 반환한다.
     *
     * @return the document or null / 문서 또는 null
     */
    public Document getDocument() {
        Document building = parsingDocument;
        if (building != null) {
            return building;
        }
        HtmlPageDocument p = page;
        return p == null ? null : p.getDocument();
    }

    /**
     * Returns the parent context: the context of the frame that contains the frame element, or the top-level context.
     * <p>
     * 부모 문맥을 반환한다: 프레임 요소를 포함한 프레임의 문맥, 또는 최상위 문맥.
     *
     * @return the parent / 부모
     */
    public BrowsingContext getParent() {
        return owner.owner != null ? owner.owner : owner.tree.topContext();
    }

    /**
     * Returns the top-level context.
     * <p>
     * 최상위 문맥을 반환한다.
     *
     * @return the top-level context / 최상위 문맥
     */
    public BrowsingContext getTop() {
        BrowsingContext top = owner.tree.topContext();
        return top != null ? top : this;
    }

    /**
     * Returns the contexts of the frames inside the document of this frame.
     * <p>
     * 이 프레임의 문서 안의 프레임의 문맥을 반환한다.
     *
     * @return a new list / 새 목록
     */
    public List<BrowsingContext> getChildren() {
        HtmlPageDocument p = page;
        FrameSet set = p == null ? null : p.frames();
        List<BrowsingContext> out = new ArrayList<BrowsingContext>();
        // 문서의 프레임 집합이 있으면 그 문맥을 옮겨 담는다.
        if (set != null) {
            out.addAll(set.contexts());
        }
        return out;
    }

    /**
     * Returns the frame element of this context.
     * <p>
     * 이 문맥의 프레임 요소를 반환한다.
     *
     * @return the element / 요소
     */
    public Element getFrameElement() {
        return element;
    }

    /**
     * Returns the name of the context.
     * <p>
     * 문맥의 이름을 반환한다.
     *
     * @return the name / 이름
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the context (<code>window.name = ...</code> in the frame) and lets the parent follow, so that
     * <code>parent[name]</code> works.
     * <p>
     * 문맥의 이름을 정하고(프레임 안의 <code>window.name = ...</code>) 부모가 따라가게 하여 <code>parent[name]</code> 이 동작하게 한다.
     *
     * @param value the name / 이름
     */
    public void setName(String value) {
        name = value == null ? "" : value;
        owner.framesChanged();
    }

    /**
     * Returns the address that the origin of the document of the frame is computed from.
     * <p>
     * 프레임의 문서의 오리진을 계산하는 원천 주소를 반환한다.
     *
     * @return the address, <code>about:blank</code> if the frame has no document yet / 주소. 프레임에 아직 문서가 없으면 <code>about:blank</code>
     */
    public String getOriginUrl() {
        Document d = getDocument();
        return d == null ? FrameUrls.ABOUT_BLANK : d.getOriginUrl();
    }

    /**
     * Navigates the frame for a script of another document (an assignment to <code>location</code> of a window that the script reached);
     * the navigation policy applies.
     * <p>
     * 다른 문서의 스크립트를 위해 프레임을 이동시킨다(스크립트가 닿은 window 의 <code>location</code> 대입). 이동 정책이 적용된다.
     *
     * @param url     the absolute address / 절대 주소
     * @param replace true to replace the entry / 항목을 바꾸려면 true
     */
    public void navigate(String url, boolean replace) {
        if (NavigationPolicy.allowed(sourceUrl(), url) && FrameNavigator.permitted(this)) {
            scriptNavigate(url, replace);
        }
    }

    /**
     * Opens a window for a script of the document of this frame (<code>window.open</code>).
     * <p>
     * 이 프레임의 문서의 스크립트를 위해 창을 연다(<code>window.open</code>).
     *
     * @param url    the absolute address or an empty string / 절대 주소 또는 빈 문자열
     * @param target the target name / 대상 이름
     * @return the context that the target selects, or null / 대상이 고르는 문맥. 없으면 null
     */
    public BrowsingContext open(String url, String target) {
        return FrameNavigator.open(owner.tree.session, owner.rootSet(), this, owner.tree.topContext(), target, url);
    }

    /**
     * Tells whether a script of this frame may navigate a context: any context unless this frame is sandboxed; a sandboxed frame may
     * navigate itself, the frames nested in it, and the top-level context only with <code>allow-top-navigation</code>.
     * <p>
     * 이 프레임의 스크립트가 문맥을 이동시킬 수 있는지 알려 준다: 이 프레임이 샌드박스가 아니면 어떤 문맥이든 가능하다. 샌드박스된 프레임은 자기 자신,
     * 그 안에 중첩된 프레임을 이동시킬 수 있고 최상위 문맥은 <code>allow-top-navigation</code> 이 있을 때만 이동시킬 수 있다.
     *
     * @param target the context / 문맥
     * @return true if allowed / 허용이면 true
     */
    public boolean mayNavigate(BrowsingContext target) {
        if (!flags.isSandboxed() || target == this) {
            return true;
        }
        if (target instanceof FrameContext) {
            return FrameNavigator.isDescendant((FrameContext) target, this);
        }
        return target != null && target.getParent() == null && flags.allowsTopNavigation();
    }

    /**
     * Tells whether the context was discarded.
     * <p>
     * 문맥이 버려졌는지 알려 준다.
     *
     * @return true if discarded / 버려졌으면 true
     */
    public boolean isClosed() {
        return destroyed;
    }

    /**
     * Discards the context: the load is cancelled, the document with its scripts, timers and presentation is released and the nested frames
     * are closed. Safe from any thread and repeatable.
     * <p>
     * 문맥을 버린다: 로드를 취소하고, 문서와 그 스크립트, 타이머, 표현 상태를 해제하고, 중첩된 프레임을 닫는다. 어느 스레드에서나 안전하며 반복해도 된다.
     */
    void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        settled = true;
        CancelToken t = token;
        if (t != null) {
            t.cancel();
        }
        PageSnapshot c = content;
        PagePresentation own = created;
        content = null;
        page = null;
        shown = null;
        created = null;
        lastRendered = null;
        if (c != null) {
            disposeLater(c);
        }
        if (own != null) {
            own.retire();
        }
        owner.tree.release();
    }

    /**
     * Tells whether the context was discarded.
     * <p>
     * 문맥이 버려졌는지 알려 준다.
     *
     * @return true if discarded / 버려졌으면 true
     */
    boolean isDestroyed() {
        return destroyed;
    }
}
