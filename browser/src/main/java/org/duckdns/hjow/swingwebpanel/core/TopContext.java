package org.duckdns.hjow.swingwebpanel.core;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.js.host.BrowsingContext;
import org.duckdns.hjow.swingwebpanel.js.host.PageScripts;

/**
 * The browsing context of the top-level document of the component, as the scripts see it: it has no parent, is its own top and lists the
 * frames of the document. The name of the window (<code>window.name</code>) is kept here.
 * <p>
 * 스크립트가 보는 컴포넌트의 최상위 문서의 브라우징 문맥: 부모가 없고 자기가 최상위이며 문서의 프레임을 나열한다. window 이름(<code>window.name</code>)을
 * 여기에 보관한다.
 */
final class TopContext implements BrowsingContext {

    /** The script host of the document. / 문서의 스크립트 호스트. */
    private final PageRuntime runtime;

    /** The DOM document. / DOM 문서. */
    private final Document document;

    /** The presentation state that owns the frames of the document. / 문서의 프레임을 소유한 표현 상태. */
    private final PagePresentation presentation;

    /** The name of the window. / window 의 이름. */
    private volatile String name = "";

    /**
     * Creates the context.
     * <p>
     * 문맥을 만든다.
     *
     * @param runtime      the script host of the document / 문서의 스크립트 호스트
     * @param document     the DOM document / DOM 문서
     * @param presentation the presentation state of the document / 문서의 표현 상태
     */
    TopContext(PageRuntime runtime, Document document, PagePresentation presentation) {
        this.runtime = runtime;
        this.document = document;
        this.presentation = presentation;
    }

    /**
     * Returns the script host of the document.
     * <p>
     * 문서의 스크립트 호스트를 반환한다.
     *
     * @return the script host / 스크립트 호스트
     */
    public PageScripts getScripts() {
        return runtime.scripts();
    }

    /**
     * Returns the DOM document.
     * <p>
     * DOM 문서를 반환한다.
     *
     * @return the document / 문서
     */
    public Document getDocument() {
        return document;
    }

    /**
     * The top-level context has no parent.
     * <p>
     * 최상위 문맥에는 부모가 없다.
     *
     * @return null / null
     */
    public BrowsingContext getParent() {
        return null;
    }

    /**
     * The top-level context is its own top.
     * <p>
     * 최상위 문맥은 자기가 최상위이다.
     *
     * @return this context / 이 문맥
     */
    public BrowsingContext getTop() {
        return this;
    }

    /**
     * Returns the contexts of the frames of the document.
     * <p>
     * 문서의 프레임의 문맥을 반환한다.
     *
     * @return a new list / 새 목록
     */
    public List<BrowsingContext> getChildren() {
        FrameSet set = presentation.frameSet();
        List<BrowsingContext> out = new ArrayList<BrowsingContext>();
        // 프레임 집합의 문맥을 문서 순서대로 옮겨 담는다.
        if (set != null) {
            out.addAll(set.contexts());
        }
        return out;
    }

    /**
     * The top-level document is not inside a frame element.
     * <p>
     * 최상위 문서는 프레임 요소 안에 있지 않다.
     *
     * @return null / null
     */
    public Element getFrameElement() {
        return null;
    }

    /**
     * Returns the name of the window.
     * <p>
     * window 의 이름을 반환한다.
     *
     * @return the name / 이름
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the window.
     * <p>
     * window 의 이름을 정한다.
     *
     * @param value the name / 이름
     */
    public void setName(String value) {
        name = value == null ? "" : value;
    }

    /**
     * Returns the address that the origin of the document is computed from.
     * <p>
     * 문서의 오리진을 계산하는 원천 주소를 반환한다.
     *
     * @return the address / 주소
     */
    public String getOriginUrl() {
        return document.getOriginUrl();
    }

    /**
     * Navigates the top-level document through the navigation policy of the component.
     * <p>
     * 컴포넌트의 이동 정책을 거쳐 최상위 문서를 이동시킨다.
     *
     * @param url     the absolute address / 절대 주소
     * @param replace true to replace the history entry / 이력 항목을 바꾸려면 true
     */
    public void navigate(String url, boolean replace) {
        runtime.navigate(url, replace);
    }

    /**
     * Opens a window for a script of the top-level document (<code>window.open</code>).
     * <p>
     * 최상위 문서의 스크립트를 위해 창을 연다(<code>window.open</code>).
     *
     * @param url    the absolute address or an empty string / 절대 주소 또는 빈 문자열
     * @param target the target name / 대상 이름
     * @return the context that the target selects, or null / 대상이 고르는 문맥. 없으면 null
     */
    public BrowsingContext open(String url, String target) {
        return FrameNavigator.open(runtime.session(), presentation.frameSet(), null, this, target, url);
    }

    /**
     * The top-level context is not sandboxed: it may navigate any context.
     * <p>
     * 최상위 문맥은 샌드박스가 아니다: 어떤 문맥이든 이동시킬 수 있다.
     *
     * @param target the context / 문맥
     * @return true / true
     */
    public boolean mayNavigate(BrowsingContext target) {
        return true;
    }

    /**
     * Tells whether the page was closed.
     * <p>
     * 페이지가 닫혔는지 알려 준다.
     *
     * @return true if closed / 닫혔으면 true
     */
    public boolean isClosed() {
        return runtime.isClosed();
    }
}
