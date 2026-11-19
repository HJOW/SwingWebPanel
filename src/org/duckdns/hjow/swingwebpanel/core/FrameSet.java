package org.duckdns.hjow.swingwebpanel.core;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.duckdns.hjow.swingwebpanel.core.frames.FrameLimits;
import org.duckdns.hjow.swingwebpanel.core.frames.SandboxFlags;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DomMutationListener;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.MutationRecord;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.js.host.BrowsingContext;
import org.duckdns.hjow.swingwebpanel.net.CancelToken;
import org.duckdns.hjow.swingwebpanel.render.FramePicture;
import org.duckdns.hjow.swingwebpanel.render.FrameSource;

/**
 * The nested browsing contexts of one document: it watches the DOM for <code>iframe</code> and <code>frame</code> elements and keeps one
 * {@link FrameContext} for each connected one (HTML5 REC 2014-10-28 4.7.2: a browsing context is created when the element is inserted and
 * discarded when it is removed; changes of <code>src</code>, <code>srcdoc</code> and <code>name</code> are processed again). The set is also
 * the source of the frame pictures for the renderer and tells the page whether frames are still loading.
 * <p>
 * All work on the contexts happens on the document queue of the component, which owns every document of a page. The listener only records that
 * a scan is needed and queues it; before the page is confirmed nothing is created (the loader thread owns the tree), but the page already
 * reports pending frames so that its <code>load</code> event waits for them.
 * <p>
 * 문서 하나의 중첩된 브라우징 문맥: DOM 에서 <code>iframe</code>, <code>frame</code> 요소를 관찰하며 연결된 요소마다 {@link FrameContext} 하나를 둔다
 * (HTML5 REC 2014-10-28 4.7.2: 브라우징 문맥은 요소가 삽입될 때 만들고 제거될 때 버리며 <code>src</code>, <code>srcdoc</code>, <code>name</code>
 * 변경은 다시 처리한다). 이 집합은 렌더러에 프레임 그림을 주는 공급원이기도 하고, 프레임이 아직 로딩 중인지 페이지에 알려 준다.
 * <p>
 * 문맥에 대한 모든 작업은 한 페이지의 모든 문서를 소유한 컴포넌트의 문서 큐에서 한다. 리스너는 검사가 필요하다는 것만 기록하고 큐에 올린다. 페이지가
 * 확정되기 전에는 아무것도 만들지 않지만(로더 스레드가 트리를 소유함) 페이지는 이미 프레임이 대기 중임을 알려, 페이지의 <code>load</code> 이벤트가
 * 프레임을 기다리게 한다.
 */
final class FrameSet implements FrameSource, DomMutationListener, Runnable {

    /**
     * What all frame sets of one top-level page share: the component, the loader that reads and parses frame documents, the navigation that
     * the page belongs to, the count of frames and the cancellation of the whole tree.
     * <p>
     * 최상위 페이지 하나의 모든 프레임 집합이 공유하는 것: 컴포넌트, 프레임 문서를 읽고 파싱하는 로더, 페이지가 속한 이동, 프레임 수, 트리 전체의 취소.
     */
    static final class Tree implements CancelToken.Hook {
        /** The component state. / 컴포넌트 상태. */
        final BrowserSession session;
        /** The view that creates presentation states for documents that come without one. / 표현 상태 없이 온 문서를 위해 표현 상태를 만드는 뷰. */
        final PageView view;
        /** The loader of frame documents. / 프레임 문서의 로더. */
        final ParsingPageLoader loader;
        /** The number of the navigation of the page, which the requests of frames carry. / 페이지의 이동 번호. 프레임의 요청이 가진다. */
        final int requestId;
        /** True if the navigation of the page bypasses the caches (<code>refresh(true)</code>); the frames of the page do the same. / 페이지의 이동이 캐시를 우회하면(<code>refresh(true)</code>) true. 페이지의 프레임도 같게 한다. */
        final boolean force;
        /** Whether the tree was closed. / 트리가 닫혔는지 여부. */
        private volatile boolean closed;
        /** The number of frames of the page. / 페이지의 프레임 수. */
        private final AtomicInteger count = new AtomicInteger();
        /** The frame set of the top-level document. / 최상위 문서의 프레임 집합. */
        private volatile FrameSet top;
        /** The browsing context of the top-level document as the scripts see it, or null if it has no script host. / 스크립트가 보는 최상위 문서의 브라우징 문맥. 스크립트 호스트가 없으면 null. */
        private volatile BrowsingContext topContext;

        /**
         * Returns the browsing context of the top-level document.
         * <p>
         * 최상위 문서의 브라우징 문맥을 반환한다.
         *
         * @return the context or null / 문맥 또는 null
         */
        BrowsingContext topContext() {
            return topContext;
        }

        /**
         * Creates the tree of a top-level page and ties it to the cancellation of its navigation.
         * <p>
         * 최상위 페이지의 트리를 만들고 그 이동의 취소에 묶는다.
         *
         * @param session   the component / 컴포넌트
         * @param loader    the frame document loader / 프레임 문서 로더
         * @param view      the view of the component / 컴포넌트의 뷰
         * @param requestId the navigation number / 이동 번호
         * @param token     the cancellation of the navigation / 이동의 취소
         * @param force     true if the navigation bypasses the caches / 이동이 캐시를 우회하면 true
         */
        Tree(BrowserSession session, ParsingPageLoader loader, PageView view, int requestId, CancelToken token, boolean force) {
            this.force = force;
            this.view = view;
            this.session = session;
            this.loader = loader;
            this.requestId = requestId;
            token.addHook(this);
        }

        /**
         * Reserves a frame for the page.
         * <p>
         * 페이지의 프레임 하나를 예약한다.
         *
         * @return false if the page has too many frames / 페이지에 프레임이 너무 많으면 false
         */
        boolean reserve() {
            if (count.incrementAndGet() > FrameLimits.MAX_FRAMES_PER_PAGE) {
                count.decrementAndGet();
                return false;
            }
            return true;
        }

        /**
         * Gives a reserved frame back.
         * <p>
         * 예약한 프레임을 돌려준다.
         */
        void release() {
            count.decrementAndGet();
        }

        /**
         * Returns the number of frames of the page.
         * <p>
         * 페이지의 프레임 수를 반환한다.
         *
         * @return the count / 개수
         */
        int size() {
            return count.get();
        }

        /**
         * Tells whether the tree was closed.
         * <p>
         * 트리가 닫혔는지 알려 준다.
         *
         * @return true if closed / 닫혔으면 true
         */
        boolean isClosed() {
            return closed;
        }

        /**
         * Closes the whole tree when the navigation of the page is cancelled or the page is replaced.
         * <p>
         * 페이지의 이동이 취소되거나 페이지가 교체되면 트리 전체를 닫는다.
         */
        public void onCancel() {
            closed = true;
            FrameSet set = top;
            if (set != null) {
                set.close();
            }
        }
    }

    /** The data that all frames of the page share. / 페이지의 모든 프레임이 공유하는 데이터. */
    final Tree tree;

    /** The frame whose document this set belongs to, or null for the top-level document. / 이 집합이 속한 문서의 프레임. 최상위 문서이면 null. */
    final FrameContext owner;

    /** The document that is watched. / 관찰하는 문서. */
    final Document document;

    /** The presentation of the document, told when a frame changes. / 문서의 표현 상태. 프레임이 바뀌면 알린다. */
    final PagePresentation presentation;

    /** The script host of the document or null if scripts cannot run there. / 문서의 스크립트 호스트. 스크립트를 실행할 수 없으면 null. */
    final PageRuntime runtime;

    /** The contexts in tree order; guarded by this object. / 트리 순서의 문맥. 이 객체로 보호한다. */
    private final List<FrameContext> contexts = new ArrayList<FrameContext>();

    /** The contexts by element; guarded by this object. / 요소별 문맥. 이 객체로 보호한다. */
    private final Map<Element, FrameContext> byElement = new IdentityHashMap<Element, FrameContext>();

    /** True once the page was confirmed and contexts may be created. / 페이지가 확정되어 문맥을 만들 수 있게 되면 true. */
    private volatile boolean activated;

    /** True once the set was closed. / 집합이 닫히면 true. */
    private volatile boolean closed;

    /** True once the document got a frame element (before the contexts exist). / 문서에 프레임 요소가 생기면(문맥이 생기기 전에) true. */
    private volatile boolean sawFrames;

    /** The confirmed top-level snapshot that the scripts of the frames are validated against. / 프레임의 스크립트를 검증하는 기준인 확정된 최상위 스냅샷. */
    private volatile PageSnapshot topSnapshot;

    /** True while a scan waits in the document queue. / 검사가 문서 큐에서 대기하는 동안 true. */
    private final AtomicBoolean scanQueued = new AtomicBoolean();

    /**
     * Creates the set of a document and starts observing it; call before the document is parsed.
     * <p>
     * 문서의 집합을 만들고 관찰을 시작한다. 문서를 파싱하기 전에 부른다.
     *
     * @param tree         the shared data / 공유 데이터
     * @param owner        the frame of the document or null / 문서의 프레임. 없으면 null
     * @param document     the document / 문서
     * @param presentation the presentation of the document / 문서의 표현 상태
     * @param runtime      the script host or null / 스크립트 호스트 또는 null
     */
    private FrameSet(Tree tree, FrameContext owner, Document document, PagePresentation presentation, PageRuntime runtime) {
        this.tree = tree;
        this.owner = owner;
        this.document = document;
        this.presentation = presentation;
        this.runtime = runtime;
    }

    /**
     * Creates the frame set of a page that is about to be parsed. The top-level document starts a new tree; the document of a frame joins the
     * tree of its parent.
     * <p>
     * 곧 파싱할 페이지의 프레임 집합을 만든다. 최상위 문서는 새 트리를 시작하고, 프레임의 문서는 부모의 트리에 들어간다.
     *
     * @param session      the component / 컴포넌트
     * @param loader       the frame document loader / 프레임 문서 로더
     * @param view         the view of the component / 컴포넌트의 뷰
     * @param request      the navigation request of the page / 페이지의 이동 요청
     * @param document     the new document / 새 문서
     * @param presentation the presentation of the document / 문서의 표현 상태
     * @param runtime      the script host or null / 스크립트 호스트 또는 null
     * @param frame        the frame of the document, or null for the top-level document / 문서의 프레임. 최상위 문서이면 null
     * @return the set / 집합
     */
    static FrameSet create(BrowserSession session, ParsingPageLoader loader, PageView view, NavigationRequest request, Document document,
            PagePresentation presentation, PageRuntime runtime, FrameContext frame) {
        Tree tree = frame != null ? frame.owner.tree : new Tree(session, loader, view, request.getId(), request.getCancelToken(), request.isForce());
        FrameSet set = new FrameSet(tree, frame, document, presentation, runtime);
        if (frame == null) {
            tree.top = set;
            tree.topContext = runtime == null ? null : runtime.getBrowsingContext();
        }
        document.addMutationListener(set);
        return set;
    }

    /**
     * Records that frame elements were inserted, removed or changed, and queues a scan.
     * <p>
     * 프레임 요소가 삽입, 제거, 변경되었음을 기록하고 검사를 예약한다.
     *
     * @param record    the change / 변경
     * @param connected true if the target is part of the document / 대상이 문서의 일부이면 true
     */
    public void mutated(MutationRecord record, boolean connected) {
        if (closed || !connected) {
            return;
        }
        boolean relevant = false;
        if (record.getType() == MutationRecord.CHILD_LIST) {
            relevant = containsFrame(record.getAddedNodes());
            if (relevant) {
                sawFrames = true;
            }
            relevant = relevant || containsFrame(record.getRemovedNodes());
        } else if (record.getType() == MutationRecord.ATTRIBUTES && isFrameElement(record.getTarget())) {
            String name = record.getAttributeName();
            relevant = "src".equals(name) || "srcdoc".equals(name) || "name".equals(name) || "sandbox".equals(name);
        }
        if (relevant) {
            // 스크립트가 프레임을 넣거나 빼면(문서 스레드) 그 자리에서 문맥을 맞춘다. 그래야 appendChild 직후에 contentWindow 가 있고 removeChild 직후에
            // 없어진다(문서의 프레임 요소가 삽입되면 브라우징 문맥이 곧바로 만들어지는 규격의 순서). 그 밖의 경우는 문서 큐에서 맞춘다.
            if (activated && record.getType() == MutationRecord.CHILD_LIST && tree.session.isDocumentThread()) {
                scan(true);
            } else {
                requestScan();
            }
        }
    }

    /**
     * Tells whether a node is an <code>iframe</code> or <code>frame</code> element.
     * <p>
     * 노드가 <code>iframe</code> 또는 <code>frame</code> 요소인지 알려 준다.
     *
     * @param node the node / 노드
     * @return true for a frame element / 프레임 요소이면 true
     */
    static boolean isFrameElement(Node node) {
        if (!(node instanceof Element)) {
            return false;
        }
        String name = ((Element) node).getLocalName();
        return "iframe".equals(name) || "frame".equals(name);
    }

    /**
     * Tells whether one of the nodes is a frame element or contains one.
     * <p>
     * 노드 중 하나가 프레임 요소이거나 프레임 요소를 포함하는지 알려 준다.
     *
     * @param nodes the nodes / 노드
     * @return true if a frame element is involved / 프레임 요소가 관련되면 true
     */
    private static boolean containsFrame(List<Node> nodes) {
        // 추가되거나 제거된 노드와 그 자손에서 프레임 요소를 찾는다.
        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            if (isFrameElement(n)) {
                return true;
            }
            if (n instanceof Element && (((Element) n).getElementsByTagName("iframe").toList().size() > 0
                    || ((Element) n).getElementsByTagName("frame").toList().size() > 0)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Queues a scan on the document queue (after the page was confirmed).
     * <p>
     * 검사를 문서 큐에 올린다(페이지가 확정된 뒤).
     */
    private void requestScan() {
        if (!activated || closed) {
            return;
        }
        if (scanQueued.compareAndSet(false, true) && !tree.session.submitDocumentTask(this)) {
            scanQueued.set(false);
        }
    }

    /**
     * Runs the queued scan.
     * <p>
     * 예약된 검사를 실행한다.
     */
    public void run() {
        scanQueued.set(false);
        if (!closed) {
            scan(false);
        }
    }

    /**
     * Allows contexts once the page was confirmed and creates those of the frame elements that are already in the document.
     * <p>
     * 페이지가 확정되면 문맥을 만들 수 있게 하고 이미 문서에 있는 프레임 요소의 문맥을 만든다.
     *
     * @param top the confirmed snapshot of the top-level document / 확정된 최상위 문서의 스냅샷
     */
    void activate(PageSnapshot top) {
        topSnapshot = top;
        activated = true;
        // 확정 전에 생긴 프레임 요소가 있으면 문서 큐에서 문맥을 만든다.
        if (sawFrames) {
            requestScan();
        }
    }

    /**
     * Makes the contexts match the frame elements that are connected to the document: a context is created for a new element, discarded
     * for a removed one, and an existing one processes changed attributes. Call it on the document queue.
     * <p>
     * 문서에 연결된 프레임 요소와 문맥을 맞춘다: 새 요소에는 문맥을 만들고, 제거된 요소의 문맥은 버리고, 기존 문맥은 바뀐 속성을 처리한다. 문서 큐에서 부른다.
     *
     * @param inline true when called from inside the DOM change that caused it (a script is running): documents that would be parsed now
     *               (<code>srcdoc</code>) are built after the script returns / 그것을 일으킨 DOM 변경 안에서 부르면(스크립트 실행 중) true. 지금 파싱해야 할
     *               문서(<code>srcdoc</code>)는 스크립트가 돌아온 뒤에 만든다
     */
    private void scan(boolean inline) {
        List<Element> elements = frameElements();
        List<FrameContext> gone = new ArrayList<FrameContext>();
        List<FrameContext> created = new ArrayList<FrameContext>();
        List<FrameContext> kept = new ArrayList<FrameContext>();
        synchronized (this) {
            // 문서에서 사라진 요소의 문맥을 찾는다.
            for (int i = 0; i < contexts.size(); i++) {
                FrameContext c = contexts.get(i);
                if (!containsIdentity(elements, c.element)) {
                    gone.add(c);
                } else {
                    kept.add(c);
                }
            }
            // 사라진 요소의 문맥은 목록에서 뺀다.
            for (int i = 0; i < gone.size(); i++) {
                contexts.remove(gone.get(i));
                byElement.remove(gone.get(i).element);
            }
            // 새 요소에는 문맥을 만든다(페이지의 프레임 수 한도 안에서).
            for (int i = 0; i < elements.size(); i++) {
                Element e = elements.get(i);
                if (!byElement.containsKey(e) && tree.reserve()) {
                    FrameContext c = new FrameContext(this, e);
                    contexts.add(c);
                    byElement.put(e, c);
                    created.add(c);
                }
            }
        }
        // 잠금 밖에서 문맥의 로드를 시작하거나 정리한다(스크립트와 이벤트가 실행될 수 있다).
        for (int i = 0; i < gone.size(); i++) {
            gone.get(i).destroy();
        }
        // 기존 문맥은 바뀐 속성을 다시 처리한다.
        for (int i = 0; i < kept.size(); i++) {
            kept.get(i).attributesChanged();
        }
        // 새 문맥은 로드를 시작한다.
        for (int i = 0; i < created.size(); i++) {
            created.get(i).start(inline);
        }
        framesChanged();
        presentation.canvasChanged();
    }

    /**
     * Tells the scripts of the document that its frames changed (created, discarded, renamed), so that <code>window[0]</code>,
     * <code>window[name]</code> and <code>length</code> follow.
     * <p>
     * 문서의 스크립트에 프레임이 바뀌었음(생성, 폐기, 이름 변경)을 알려 <code>window[0]</code>, <code>window[name]</code>, <code>length</code> 가
     * 따라가게 한다.
     */
    void framesChanged() {
        PageRuntime r = runtime;
        if (r != null && r.scripts() != null && !closed) {
            r.scripts().framesChanged();
        }
    }

    /**
     * Collects the frame elements that are part of the document.
     * <p>
     * 문서의 일부인 프레임 요소를 모은다.
     *
     * @return the elements, <code>iframe</code> first / 요소들. <code>iframe</code> 이 먼저
     */
    private List<Element> frameElements() {
        List<Element> out = new ArrayList<Element>();
        out.addAll(document.getElementsByTagName("iframe").toList());
        out.addAll(document.getElementsByTagName("frame").toList());
        return out;
    }

    /**
     * Tells whether a list contains an element (by identity).
     * <p>
     * 목록에 요소가 들어 있는지(동일성으로) 알려 준다.
     *
     * @param list    the list / 목록
     * @param element the element / 요소
     * @return true if contained / 들어 있으면 true
     */
    private static boolean containsIdentity(List<Element> list, Element element) {
        // 목록의 요소를 동일성으로 비교한다.
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == element) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the picture of the frame of an element for the renderer.
     * <p>
     * 렌더러를 위해 요소의 프레임 그림을 반환한다.
     *
     * @param frameElement the iframe or frame element / iframe 또는 frame 요소
     * @param width        the width of the content box / 콘텐츠 상자의 너비
     * @param height       the height of the content box / 콘텐츠 상자의 높이
     * @return the picture or null if the element has no context / 그림. 요소에 문맥이 없으면 null
     */
    public FramePicture getFrame(Element frameElement, double width, double height) {
        FrameContext context = contextOf(frameElement);
        return context == null ? null : context.picture(width, height);
    }

    /**
     * Returns the context of a frame element.
     * <p>
     * 프레임 요소의 문맥을 반환한다.
     *
     * @param element the element / 요소
     * @return the context or null / 문맥 또는 null
     */
    synchronized FrameContext contextOf(Element element) {
        return element == null ? null : byElement.get(element);
    }

    /**
     * Returns a copy of the contexts in tree order.
     * <p>
     * 트리 순서의 문맥 복사본을 반환한다.
     *
     * @return the contexts / 문맥
     */
    synchronized List<FrameContext> contexts() {
        return new ArrayList<FrameContext>(contexts);
    }

    /**
     * Finds the page document that belongs to a DOM document among the frames of this set, at any depth.
     * <p>
     * 이 집합의 프레임(어느 깊이든)에서 DOM 문서에 속한 페이지 문서를 찾는다.
     *
     * @param dom the DOM document / DOM 문서
     * @return the page document or null / 페이지 문서 또는 null
     */
    HtmlPageDocument documentFor(Document dom) {
        List<FrameContext> list = contexts();
        // 프레임의 문서와 그 안의 프레임을 깊이 우선으로 찾는다.
        for (int i = 0; i < list.size(); i++) {
            HtmlPageDocument page = list.get(i).page();
            if (page == null) {
                continue;
            }
            if (page.getDocument() == dom) {
                return page;
            }
            FrameSet inner = page.frames();
            HtmlPageDocument found = inner == null ? null : inner.documentFor(dom);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * Finds the frame that a DOM document is the document of, at any depth below this set.
     * <p>
     * 이 집합 아래(어느 깊이든)에서 DOM 문서가 문서인 프레임을 찾는다.
     *
     * @param dom the DOM document / DOM 문서
     * @return the frame or null / 프레임 또는 null
     */
    FrameContext frameOf(Document dom) {
        List<FrameContext> list = contexts();
        // 프레임과 그 안의 프레임을 깊이 우선으로 찾는다.
        for (int i = 0; i < list.size(); i++) {
            FrameContext c = list.get(i);
            HtmlPageDocument page = c.page();
            if (page == null) {
                continue;
            }
            if (page.getDocument() == dom) {
                return c;
            }
            FrameSet inner = page.frames();
            FrameContext found = inner == null ? null : inner.frameOf(dom);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * Tells whether frames of this document are still loading or about to be created, so that the page must not report that it finished.
     * <p>
     * 이 문서의 프레임이 아직 로딩 중이거나 곧 만들어질 것인지 알려 준다. 그러면 페이지는 끝났다고 알리면 안 된다.
     *
     * @return true while a frame is pending / 프레임이 대기 중이면 true
     */
    boolean hasPending() {
        if (closed) {
            return false;
        }
        // 확정 전에는 프레임 요소가 보이기만 해도 대기 중이다(문맥은 확정 뒤에 만든다).
        if (!activated) {
            return sawFrames;
        }
        if (scanQueued.get()) {
            return true;
        }
        List<FrameContext> list = contexts();
        // 문맥 중 로딩이 끝나지 않은 것이 있는지 확인한다.
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).isPending()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns how many frames finished and how many exist, for the progress of the page.
     * <p>
     * 페이지의 진행률을 위해 프레임이 몇 개 끝났고 몇 개 있는지 반환한다.
     *
     * @return {finished, started} / {끝난 수, 시작한 수}
     */
    int[] activity() {
        List<FrameContext> list = contexts();
        int finished = 0;
        // 로딩이 끝난 문맥을 센다.
        for (int i = 0; i < list.size(); i++) {
            if (!list.get(i).isPending()) {
                finished++;
            }
        }
        int started = list.size();
        // 아직 문맥을 만들지 못한 프레임 요소가 있으면 하나가 끝나지 않은 것으로 센다.
        if (list.isEmpty() && hasPending()) {
            started = 1;
        }
        return new int[] { finished, started };
    }

    /**
     * Returns the sandboxing flags that frames of this document inherit.
     * <p>
     * 이 문서의 프레임이 물려받는 샌드박스 플래그를 반환한다.
     *
     * @return the flags / 플래그
     */
    SandboxFlags effectiveFlags() {
        return owner == null ? SandboxFlags.NONE : owner.flags();
    }

    /** The frame whose document has the keyboard focus (kept in the frame set of the top-level document), or null for the top-level document. / 문서가 키보드 포커스를 가진 프레임(최상위 문서의 프레임 집합에 둔다). 최상위 문서이면 null. */
    private volatile FrameContext focusedFrame;

    /**
     * Records which frame has the keyboard focus after a click: key events go to its document until a click lands elsewhere.
     * <p>
     * 클릭 뒤에 어느 프레임이 키보드 포커스를 갖는지 기록한다: 다른 곳을 클릭하기 전까지 키 이벤트는 그 프레임의 문서로 간다.
     *
     * @param frame the frame, or null for the top-level document / 프레임. 최상위 문서이면 null
     */
    void setFocusedFrame(FrameContext frame) {
        rootSet().focusedFrame = frame;
    }

    /**
     * Returns the frame whose document has the keyboard focus.
     * <p>
     * 문서가 키보드 포커스를 가진 프레임을 반환한다.
     *
     * @return the frame or null for the top-level document / 프레임. 최상위 문서이면 null
     */
    FrameContext focusedFrame() {
        FrameContext f = rootSet().focusedFrame;
        return f != null && !f.isDestroyed() ? f : null;
    }

    /**
     * Returns the frame set of the top-level document of the page, where names of browsing contexts are looked up.
     * <p>
     * 페이지의 최상위 문서의 프레임 집합을 반환한다. 브라우징 문맥의 이름은 여기서부터 찾는다.
     *
     * @return the root set / 루트 집합
     */
    FrameSet rootSet() {
        FrameSet top = tree.top;
        return top == null ? this : top;
    }

    /**
     * Returns the nesting level of the document of this set: 0 for the top-level document.
     * <p>
     * 이 집합의 문서의 중첩 단계를 반환한다. 최상위 문서는 0.
     *
     * @return the level / 단계
     */
    int depth() {
        return owner == null ? 0 : owner.depth;
    }

    /**
     * Returns the addresses of the documents of this set and of all its ancestors, nearest first (the documents that a frame of this set must
     * not load again).
     * <p>
     * 이 집합의 문서와 모든 조상 문서의 주소를 가까운 것부터 반환한다(이 집합의 프레임이 다시 로드하면 안 되는 문서).
     *
     * @return the addresses / 주소
     */
    List<String> ancestorUrls() {
        List<String> out = new ArrayList<String>();
        FrameSet s = this;
        // 이 문서에서 최상위 문서까지 올라가며 주소를 모은다.
        while (s != null) {
            out.add(s.document.getUrl());
            s = s.owner == null ? null : s.owner.owner;
        }
        return out;
    }

    /**
     * Returns the address of the network document that this set belongs to: the address of its document, or the one of the nearest ancestor
     * when the document is <code>about:blank</code> or <code>about:srcdoc</code>. The navigation policy of frames uses it.
     * <p>
     * 이 집합이 속한 네트워크 문서의 주소를 반환한다: 문서의 주소, 문서가 <code>about:blank</code> 나 <code>about:srcdoc</code> 이면 가장 가까운 조상의
     * 주소. 프레임의 이동 정책이 쓴다.
     *
     * @return the address / 주소
     */
    String sourceUrl() {
        String u = document.getUrl();
        if (u != null && !u.startsWith("about:") || owner == null) {
            return u;
        }
        return owner.owner.sourceUrl();
    }

    /**
     * Returns the snapshot of the top-level page that the scripts of frames are validated against.
     * <p>
     * 프레임의 스크립트를 검증하는 기준인 최상위 페이지의 스냅샷을 반환한다.
     *
     * @return the snapshot or null before confirmation / 스냅샷. 확정 전이면 null
     */
    PageSnapshot topSnapshot() {
        return topSnapshot;
    }

    /**
     * Discards all contexts: requests are cancelled, scripts and presentations are released, nested frames are closed. Safe from any thread
     * and repeatable.
     * <p>
     * 모든 문맥을 버린다: 요청을 취소하고 스크립트와 표현 상태를 해제하며 중첩된 프레임을 닫는다. 어느 스레드에서나 안전하며 반복해도 된다.
     */
    void close() {
        if (closed) {
            return;
        }
        closed = true;
        List<FrameContext> copy;
        synchronized (this) {
            copy = new ArrayList<FrameContext>(contexts);
            contexts.clear();
            byElement.clear();
        }
        // 문맥을 하나씩 버린다.
        for (int i = 0; i < copy.size(); i++) {
            copy.get(i).destroy();
        }
    }

    /**
     * Tells whether the set was closed.
     * <p>
     * 집합이 닫혔는지 알려 준다.
     *
     * @return true if closed / 닫혔으면 true
     */
    boolean isClosed() {
        return closed;
    }
}
