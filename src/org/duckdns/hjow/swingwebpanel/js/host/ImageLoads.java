package org.duckdns.hjow.swingwebpanel.js.host;

import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.MutationRecord;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;
import org.duckdns.hjow.swingwebpanel.render.ImageLookup;
import org.duckdns.hjow.swingwebpanel.render.ImageSelection;

/**
 * The image loading of the <code>img</code> elements of one page (plan items P18-05, P15-04-b-2): it chooses the source of an image
 * ({@link ImageSelection}: <code>src</code>, <code>srcset</code>, <code>sizes</code>, <code>picture</code>), asks the component to load it
 * through the {@link ImageRequestSource}, and when the load ended it tells the owner of the DOM, which fires the <code>load</code> or
 * <code>error</code> event at the element and updates <code>complete</code>, <code>naturalWidth</code>, <code>naturalHeight</code> and
 * <code>currentSrc</code>. While the document is still loading, every started image delays the window <code>load</code> event until its own
 * event fired, whether it succeeded or failed. The results are queued on the {@link ScriptLoads} of the page, so they reach the DOM owner
 * at the same safe points as the async scripts: between parser steps, while the owner waits, or as tasks of the document queue. Every method
 * except the callback of the request runs on the thread that owns the DOM of the page. The class never touches pixels; the renderer paints from
 * the same cached image.
 * <p>
 * 한 페이지의 <code>img</code> 요소의 이미지 로딩(계획 P18-05, P15-04-b-2): 이미지의 소스를 고르고({@link ImageSelection}: <code>src</code>,
 * <code>srcset</code>, <code>sizes</code>, <code>picture</code>), {@link ImageRequestSource} 로 컴포넌트에 로드를 요청하며, 로드가 끝나면 DOM
 * 소유자에게 알려 요소에 <code>load</code> 또는 <code>error</code> 이벤트를 보내고 <code>complete</code>, <code>naturalWidth</code>,
 * <code>naturalHeight</code>, <code>currentSrc</code> 를 갱신한다. 문서를 로딩하는 동안에는 시작한 이미지마다 성공이든 실패든 자신의 이벤트가
 * 발생할 때까지 window <code>load</code> 이벤트를 늦춘다. 결과는 페이지의 {@link ScriptLoads} 에 줄 세워지므로 async 스크립트와 같은 안전한 지점
 * (파서 단계 사이, 소유자가 기다리는 동안, 문서 큐의 작업)에서 DOM 소유자에게 전달된다. 요청의 콜백을 뺀 모든 메서드는 페이지의 DOM 을 소유한
 * 스레드에서 실행된다. 이 클래스는 픽셀을 다루지 않으며 렌더러가 같은 캐시된 이미지로 그린다.
 */
final class ImageLoads {

    /**
     * The result of one image request, handed to the owner of the DOM.
     * <p>
     * DOM 소유자에게 전달하는 이미지 요청 하나의 결과.
     */
    static final class Task {
        /** The element whose image was requested. / 이미지를 요청한 요소. */
        final Element image;
        /** The request number of the element; an older result is ignored. / 요소의 요청 번호. 오래된 결과는 무시한다. */
        final int version;
        /** The natural width in px, set before the result is queued. / 자연 너비(px). 결과를 줄 세우기 전에 정한다. */
        int width;
        /** The natural height in px, set before the result is queued. / 자연 높이(px). 결과를 줄 세우기 전에 정한다. */
        int height;
        /** The failure or null, set before the result is queued. / 실패 원인 또는 null. 결과를 줄 세우기 전에 정한다. */
        String error;

        /**
         * Creates a task.
         * <p>
         * 작업을 만든다.
         *
         * @param image   the element / 요소
         * @param version the request number / 요청 번호
         */
        Task(Element image, int version) {
            this.image = image;
            this.version = version;
        }
    }

    /**
     * The loading state of one image element.
     * <p>
     * 이미지 요소 하나의 로딩 상태.
     */
    private static final class State {
        /** The absolute URL that is being or was requested, or null. / 요청 중이거나 요청한 절대 URL. 없으면 null. */
        String requested;
        /** The CORS mode of the last request (anonymous or use-credentials), null without a crossorigin attribute. / 마지막 요청의 CORS 모드(anonymous 또는 use-credentials). crossorigin 속성이 없으면 null. */
        String cors;
        /** The absolute URL of the current source, empty if none. / 현재 소스의 절대 URL. 없으면 빈 문자열. */
        String currentSrc = "";
        /** The density of the chosen candidate. / 선택된 후보의 밀도. */
        double density = 1;
        /** The number of the last request. / 마지막 요청의 번호. */
        int version;
        /** True while a request is out and its result was not delivered. / 요청이 나가 있고 결과가 아직 전달되지 않았으면 true. */
        boolean pending;
        /** True when the current request ended (or no request is needed). / 현재 요청이 끝났거나 요청이 필요 없으면 true. */
        boolean complete = true;
        /** True if the last request failed. / 마지막 요청이 실패했으면 true. */
        boolean broken;
        /** The natural width in CSS px (pixels divided by the density). / 자연 너비(CSS px, 픽셀을 밀도로 나눈 값). */
        int width;
        /** The natural height in CSS px. / 자연 높이(CSS px). */
        int height;
        /** True while this image delays the window load event. / 이 이미지가 window load 이벤트를 늦추고 있으면 true. */
        boolean delaying;
    }

    /**
     * The page that owns the images.
     * <p>
     * 이미지를 소유하는 페이지.
     */
    private final PageScripts page;

    /**
     * The result queue of the page (shared with the async scripts).
     * <p>
     * 페이지의 결과 줄(async 스크립트와 공유한다).
     */
    private final ScriptLoads loads;

    /**
     * The states by element; elements that are no longer referenced disappear.
     * <p>
     * 요소별 상태. 더 이상 참조되지 않는 요소는 사라진다.
     */
    private final Map<Element, State> states = new WeakHashMap<Element, State>();

    /**
     * The number of images that delay the load event.
     * <p>
     * load 이벤트를 늦추는 이미지의 수.
     */
    private int delaying;

    /**
     * Creates the image loads of a page.
     * <p>
     * 페이지의 이미지 로딩을 만든다.
     *
     * @param page  the page / 페이지
     * @param loads the result queue of the page / 페이지의 결과 줄
     */
    ImageLoads(PageScripts page, ScriptLoads loads) {
        this.page = page;
        this.loads = loads;
    }

    /**
     * Tells whether an image still delays the load event.
     * <p>
     * 이미지가 아직 load 이벤트를 늦추고 있는지 알려 준다.
     *
     * @return true if one does / 늦추는 것이 있으면 true
     */
    boolean isDelayingLoad() {
        return delaying > 0;
    }

    /**
     * Reacts to a change of the document: inserted <code>img</code> elements and <code>source</code> elements of a <code>picture</code> start
     * their load, and a change of <code>src</code>, <code>srcset</code>, <code>sizes</code>, <code>media</code> or <code>type</code> loads the
     * new source. Elements that are not connected load too, as an image made with <code>new Image()</code> does.
     * <p>
     * 문서의 변경에 반응한다: 삽입된 <code>img</code> 요소와 <code>picture</code> 의 <code>source</code> 요소는 로드를 시작하고,
     * <code>src</code>, <code>srcset</code>, <code>sizes</code>, <code>media</code>, <code>type</code> 의 변경은 새 소스를 로드한다.
     * <code>new Image()</code> 로 만든 이미지처럼 연결되지 않은 요소도 로드한다.
     *
     * @param record    the change / 변경
     * @param connected whether the target is in the document / 대상이 문서에 있는지 여부
     */
    void mutated(MutationRecord record, boolean connected) {
        if (record.getType() == MutationRecord.ATTRIBUTES && record.getTarget() instanceof Element) {
            String name = record.getAttributeName();
            if (name == null) {
                return;
            }
            String lower = name.toLowerCase();
            // 이미지 소스(또는 가져오는 방식인 crossorigin)를 정하는 속성이 바뀐 경우만 다시 고른다.
            if ("src".equals(lower) || "srcset".equals(lower) || "sizes".equals(lower) || "media".equals(lower) || "type".equals(lower) || "crossorigin".equals(lower)) {
                changed((Element) record.getTarget());
            }
        } else if (record.getType() == MutationRecord.CHILD_LIST && connected) {
            java.util.List<Node> added = record.getAddedNodes();
            // 삽입된 하위 트리마다 img 와 picture 의 source 를 찾아 소스를 고른다.
            for (int i = 0; i < added.size(); i++) {
                Node root = added.get(i);
                // 삽입된 하위 트리의 노드를 문서 순서로 훑으며 요소마다 소스를 고른다.
                for (Node n = root; n != null; n = Node.nextInTree(n, root)) {
                    if (n instanceof Element) {
                        changed((Element) n);
                    }
                }
            }
        }
    }

    /**
     * Starts the load for an element that was inserted or changed: an <code>img</code> element itself, or the <code>img</code> children of
     * the <code>picture</code> that a <code>source</code> element belongs to.
     * <p>
     * 삽입되거나 바뀐 요소의 로드를 시작한다: <code>img</code> 요소 자신, 또는 <code>source</code> 요소가 속한 <code>picture</code> 의
     * <code>img</code> 자식들.
     *
     * @param e the element / 요소
     */
    private void changed(Element e) {
        if (!Namespaces.HTML.equals(e.getNamespaceURI()) || e.getLocalName() == null) {
            return;
        }
        String tag = e.getLocalName().toLowerCase();
        if ("img".equals(tag)) {
            track(e);
        } else if ("source".equals(tag) && e.getParentNode() instanceof Element
                && "picture".equals(((Element) e.getParentNode()).getLocalName())) {
            // picture 의 source 가 바뀌면 같은 picture 안의 img 의 소스를 다시 고른다.
            for (Node n = e.getParentNode().getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n instanceof Element && "img".equals(((Element) n).getLocalName())) {
                    track((Element) n);
                }
            }
        }
    }

    /**
     * Chooses the source of an image element and loads it unless the same URL is already loading or loaded.
     * <p>
     * 이미지 요소의 소스를 고르고, 같은 URL 을 이미 로딩 중이거나 로드했으면 아니면 로드한다.
     *
     * @param img the element / 요소
     */
    void track(Element img) {
        if (page.isDisposedPage()) {
            return;
        }
        State s = states.get(img);
        if (s == null) {
            s = new State();
            states.put(img, s);
        }
        MediaEnvironment env = new MediaEnvironment(page.getEnvironment().getViewportWidth(), page.getEnvironment().getViewportHeight());
        ImageSelection.Choice choice = ImageSelection.select(img, env);
        if (choice == null) {
            noSource(img, s);
            return;
        }
        String url = absolute(img, choice.getUrl().trim());
        // 같은 URL 이 이미 요청되었으면(로딩 중이거나 끝남) 다시 요청하지 않는다. 밀도만 갱신한다.
        String cors = ImageLookup.corsMode(img);
        if (url.equals(s.requested) && !s.broken && (cors == null ? s.cors == null : cors.equals(s.cors))) {
            s.density = choice.getDensity();
            return;
        }
        ImageRequestSource source = page.getEnvironment().getImageRequestSource();
        s.version++;
        s.requested = url;
        s.cors = cors;
        s.currentSrc = url;
        s.density = choice.getDensity();
        s.pending = source != null;
        s.complete = false;
        s.broken = false;
        s.width = 0;
        s.height = 0;
        if (source == null) {
            // 이미지 서비스가 없는 호스트는 로드하지 않는다(완료되지 않은 채로 남는다).
            return;
        }
        // 문서를 로딩하는 동안 시작한 이미지는 자신의 이벤트가 발생할 때까지 load 이벤트를 늦춘다.
        if (!page.isLoadFired() && !s.delaying) {
            s.delaying = true;
            delaying++;
        }
        final Task task = new Task(img, s.version);
        final ScriptLoads.Load load = loads.beginExternal(task);
        ImageRequestSource.Callback callback = new ImageRequestSource.Callback() {
                /**
                 * Records the result and queues it for the owner of the DOM (may run on any thread).
                 * <p>
                 * 결과를 기록하고 DOM 소유자에게 줄 세운다(어느 스레드에서나 실행될 수 있다).
                 *
                 * @param width  the natural width / 자연 너비
                 * @param height the natural height / 자연 높이
                 * @param error  the failure or null / 실패 원인 또는 null
                 */
                public void done(int width, int height, String error) {
                    task.width = width;
                    task.height = height;
                    task.error = error;
                    loads.finishExternal(load, null);
                }
        };
        try {
            // crossorigin 속성이 있고 서비스가 CORS 모드를 알면 그 모드로, 아니면 일반 이미지로 요청한다.
            if (cors != null && source instanceof CorsImageRequestSource) {
                ((CorsImageRequestSource) source).request(url, cors, callback);
            } else {
                source.request(url, callback);
            }
        } catch (RuntimeException e) {
            // 서비스가 요청을 받지 못하면 실패로 끝낸다.
            task.error = String.valueOf(e);
            loads.finishExternal(load, new IOException(task.error));
        }
    }

    /**
     * Handles an element without a usable source: the image is complete and empty; an <code>src</code> attribute that is present but empty
     * is an error.
     * <p>
     * 쓸 수 있는 소스가 없는 요소를 처리한다: 이미지는 완료되고 비어 있다. 속성이 있으나 비어 있는 <code>src</code> 는 오류이다.
     *
     * @param img the element / 요소
     * @param s   its state / 상태
     */
    private void noSource(Element img, State s) {
        s.version++;
        s.requested = null;
        s.currentSrc = "";
        s.pending = false;
        s.complete = true;
        s.broken = false;
        s.width = 0;
        s.height = 0;
        if (s.delaying) {
            s.delaying = false;
            delaying--;
        }
        String src = img.getAttribute("src");
        // src 속성이 있는데 비어 있으면 error 이벤트를 올린다(소스를 비운 경우도 포함).
        if (src != null && src.trim().length() == 0 && !img.hasAttribute("srcset")) {
            Task task = new Task(img, s.version);
            task.error = "empty src";
            s.pending = true;
            s.complete = false;
            ScriptLoads.Load load = loads.beginExternal(task);
            loads.finishExternal(load, null);
        }
    }

    /**
     * Resolves a source URL against the base URL of the document.
     * <p>
     * 소스 URL 을 문서의 기준 URL 로 해석한다.
     *
     * @param img the element / 요소
     * @param raw the URL as written / 적힌 URL
     * @return the absolute URL, or the text itself when it cannot be resolved / 절대 URL. 해석할 수 없으면 텍스트 그대로
     */
    private static String absolute(Element img, String raw) {
        String base = img.getOwnerDocument() == null ? null : img.getOwnerDocument().getBaseUri();
        if (base == null || base.length() == 0 || "about:blank".equals(base) || "about:text".equals(base)) {
            return raw;
        }
        try {
            return UrlUtil.resolve(base, raw);
        } catch (IllegalArgumentException e) {
            return raw;
        }
    }

    /**
     * Applies a result that reached the owner of the DOM: updates the state and fires <code>load</code> or <code>error</code>. A result of an
     * older request is dropped. The window load event is released after the element event.
     * <p>
     * DOM 소유자에게 도착한 결과를 적용한다: 상태를 갱신하고 <code>load</code> 또는 <code>error</code> 를 발생시킨다. 오래된 요청의 결과는 버린다.
     * window load 이벤트는 요소의 이벤트 뒤에 풀어 준다.
     *
     * @param task the result / 결과
     */
    void deliver(Task task) {
        State s = states.get(task.image);
        if (s == null || s.version != task.version || page.isDisposedPage()) {
            return;
        }
        s.pending = false;
        s.complete = true;
        boolean ok = task.error == null;
        s.broken = !ok;
        if (ok) {
            double density = s.density <= 0 ? 1 : s.density;
            s.width = (int) Math.round(task.width / density);
            s.height = (int) Math.round(task.height / density);
        } else {
            s.width = 0;
            s.height = 0;
        }
        page.fireImageEvent(task.image, ok ? "load" : "error");
        if (s.delaying) {
            s.delaying = false;
            delaying--;
        }
    }

    /**
     * Tells whether an image is complete (<code>HTMLImageElement.complete</code>): it has no source, or its request ended.
     * <p>
     * 이미지가 완료되었는지 알려 준다(<code>HTMLImageElement.complete</code>): 소스가 없거나 요청이 끝났다.
     *
     * @param img the element / 요소
     * @return the flag / 플래그
     */
    boolean isComplete(Element img) {
        State s = states.get(img);
        if (s != null) {
            return s.complete;
        }
        String src = img.getAttribute("src");
        String srcset = img.getAttribute("srcset");
        return (src == null || src.trim().length() == 0) && (srcset == null || srcset.trim().length() == 0);
    }

    /**
     * Returns the natural width of a loaded image in CSS px, 0 for an image that is not available.
     * <p>
     * 로드한 이미지의 자연 너비를 CSS px 로 반환한다. 이용할 수 없는 이미지는 0.
     *
     * @param img the element / 요소
     * @return the width / 너비
     */
    int naturalWidth(Element img) {
        State s = states.get(img);
        return s == null || !s.complete || s.broken ? 0 : s.width;
    }

    /**
     * Returns the natural height of a loaded image in CSS px, 0 for an image that is not available.
     * <p>
     * 로드한 이미지의 자연 높이를 CSS px 로 반환한다. 이용할 수 없는 이미지는 0.
     *
     * @param img the element / 요소
     * @return the height / 높이
     */
    int naturalHeight(Element img) {
        State s = states.get(img);
        return s == null || !s.complete || s.broken ? 0 : s.height;
    }

    /**
     * Tells whether the last load of an image failed (the image is "broken": <code>complete</code> but without a picture).
     * <p>
     * 이미지의 마지막 로드가 실패했는지(이미지가 "깨짐": <code>complete</code> 이지만 그림이 없음) 알려 준다.
     *
     * @param img the element / 요소
     * @return true if broken / 깨졌으면 true
     */
    boolean isBroken(Element img) {
        State s = states.get(img);
        return s != null && s.complete && s.broken;
    }

    /**
     * Returns the absolute URL of the current source of an image (<code>currentSrc</code>), empty if there is none.
     * <p>
     * 이미지의 현재 소스의 절대 URL(<code>currentSrc</code>)을 반환한다. 없으면 빈 문자열.
     *
     * @param img the element / 요소
     * @return the URL / URL
     */
    String currentSrc(Element img) {
        State s = states.get(img);
        return s == null ? "" : s.currentSrc;
    }

    /**
     * Drops all states; results that arrive later are ignored.
     * <p>
     * 모든 상태를 버린다. 나중에 도착하는 결과는 무시한다.
     */
    void dispose() {
        states.clear();
        delaying = 0;
    }
}
