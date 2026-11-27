package org.duckdns.hjow.swingwebpanel.js.host;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;

/**
 * A browsing context as the script host sees it: the top-level document of the component or a nested context of an <code>iframe</code> or
 * <code>frame</code> element. The scripts of one page use it for <code>window.parent</code>, <code>top</code>, <code>frames</code>,
 * <code>length</code>, <code>frameElement</code>, <code>name</code> and <code>iframe.contentWindow</code>; the component implements it. All
 * methods are called on the document queue of the component, which owns every document of the page, so a script of one document may read the
 * state of the others.
 * <p>
 * 스크립트 호스트가 보는 브라우징 문맥: 컴포넌트의 최상위 문서 또는 <code>iframe</code>, <code>frame</code> 요소의 중첩된 문맥. 한 페이지의 스크립트는
 * 이것으로 <code>window.parent</code>, <code>top</code>, <code>frames</code>, <code>length</code>, <code>frameElement</code>, <code>name</code>,
 * <code>iframe.contentWindow</code> 를 구현하며 컴포넌트가 이를 구현한다. 모든 메서드는 한 페이지의 모든 문서를 소유한 컴포넌트의 문서 큐에서 불리므로
 * 한 문서의 스크립트가 다른 문서의 상태를 읽을 수 있다.
 */
public interface BrowsingContext {

    /**
     * Returns the script host of the document that the context shows now.
     * <p>
     * 문맥이 지금 보여 주는 문서의 스크립트 호스트를 반환한다.
     *
     * @return the script host, or null if the document has none (scripts are disabled, the sandbox forbids them, or nothing was loaded) / 스크립트 호스트.
     *         문서에 없으면(스크립트가 꺼짐, 샌드박스가 금지, 로드된 것이 없음) null
     */
    PageScripts getScripts();

    /**
     * Returns the script host that receives a message which another window posts to the window of this context. A top-level window
     * that is loading a new page gives the host of that page as soon as its scripts can run, so that a page which announces itself
     * to its opener while it is parsed gets the answer; otherwise it is {@link #getScripts()}.
     * <p>
     * 다른 창이 이 문맥의 window 로 보낸 메시지를 받는 스크립트 호스트를 반환한다. 새 페이지를 읽는 최상위 창은 그 페이지의 스크립트가 실행될 수
     * 있게 되면 곧 그 페이지의 호스트를 준다. 그래서 파싱 중에 여는 창에 자신을 알린 페이지가 답을 받는다. 그 밖에는 {@link #getScripts()} 와 같다.
     *
     * @return the script host, or null / 스크립트 호스트. 없으면 null
     */
    PageScripts getMessageScripts();

    /**
     * Returns the DOM document that the context shows now.
     * <p>
     * 문맥이 지금 보여 주는 DOM 문서를 반환한다.
     *
     * @return the document or null / 문서. 없으면 null
     */
    Document getDocument();

    /**
     * Returns the parent context.
     * <p>
     * 부모 문맥을 반환한다.
     *
     * @return the parent, or null for the top-level context / 부모. 최상위 문맥이면 null
     */
    BrowsingContext getParent();

    /**
     * Returns the top-level context.
     * <p>
     * 최상위 문맥을 반환한다.
     *
     * @return the top-level context (this one if it is the top-level context) / 최상위 문맥(이것이 최상위이면 자기 자신)
     */
    BrowsingContext getTop();

    /**
     * Returns the nested contexts of the document that this context shows, in document order.
     * <p>
     * 이 문맥이 보여 주는 문서의 중첩 문맥을 문서 순서로 반환한다.
     *
     * @return a new list / 새 목록
     */
    List<BrowsingContext> getChildren();

    /**
     * Returns the <code>iframe</code> or <code>frame</code> element that this context belongs to, in the document of the parent.
     * <p>
     * 이 문맥이 속한 <code>iframe</code> 또는 <code>frame</code> 요소를 부모의 문서에서 반환한다.
     *
     * @return the element, or null for the top-level context / 요소. 최상위 문맥이면 null
     */
    Element getFrameElement();

    /**
     * Returns the name of the context (<code>window.name</code>).
     * <p>
     * 문맥의 이름을 반환한다(<code>window.name</code>).
     *
     * @return the name, empty if none / 이름. 없으면 빈 문자열
     */
    String getName();

    /**
     * Sets the name of the context.
     * <p>
     * 문맥의 이름을 정한다.
     *
     * @param name the name / 이름
     */
    void setName(String name);

    /**
     * Returns the address that the origin of the document is computed from (it is the document address, except for documents that inherit
     * their origin or have an opaque one).
     * <p>
     * 문서의 오리진을 계산하는 원천 주소를 반환한다(오리진을 물려받거나 불투명한 문서를 빼면 문서 주소이다).
     *
     * @return the address, <code>about:blank</code> if there is no document / 주소. 문서가 없으면 <code>about:blank</code>
     */
    String getOriginUrl();

    /**
     * Navigates the context to an address (an assignment to <code>location</code> of a window that scripts reach from another document). The
     * navigation policy of the component applies.
     * <p>
     * 문맥을 주소로 이동시킨다(스크립트가 다른 문서에서 닿은 window 의 <code>location</code> 대입). 컴포넌트의 이동 정책이 적용된다.
     *
     * @param url     the absolute address / 절대 주소
     * @param replace true to replace the entry of the history / 이력 항목을 바꾸려면 true
     */
    void navigate(String url, boolean replace);

    /**
     * Opens a window for a script (<code>window.open</code>), the way the rules for choosing a browsing context of the HTML Standard do: the
     * keywords <code>_self</code>, <code>_parent</code> and <code>_top</code> and the name of a frame select an existing context, which is
     * navigated to the address (if the address is not empty). A component has no windows, so a name that needs a new context, and a
     * sandbox that does not allow popups, give no window.
     * <p>
     * 스크립트를 위해 창을 연다(<code>window.open</code>). HTML 표준의 브라우징 문맥 선택 규칙대로: 키워드 <code>_self</code>, <code>_parent</code>,
     * <code>_top</code> 과 프레임의 이름은 기존 문맥을 고르며, 주소가 비어 있지 않으면 그 문맥을 주소로 이동시킨다. 컴포넌트에는 창이 없으므로 새 문맥이
     * 필요한 이름과 팝업을 허용하지 않는 샌드박스는 창을 주지 않는다.
     *
     * @param url    the absolute address, or an empty string to navigate nothing / 절대 주소. 아무것도 이동시키지 않으려면 빈 문자열
     * @param target the target name or keyword / 대상 이름 또는 키워드
     * @return the context that the target names, or null if no window is opened / 대상이 고르는 문맥. 창을 열지 않으면 null
     */
    BrowsingContext open(String url, String target);

    /**
     * Opens a window for a script (<code>window.open</code>) like {@link #open(String, String)}, with the window features text. A
     * name that needs a new context opens a new window if the component can show one and the popup blocker allows it (plan item
     * P31-08); the features can ask for a window without a connection to its opener (<code>noopener</code>), which gives null.
     * <p>
     * {@link #open(String, String)} 처럼 스크립트를 위해 창을 열되(<code>window.open</code>) 창 기능 문자열을 함께 받는다. 새 문맥이 필요한
     * 이름은 컴포넌트가 창을 보일 수 있고 팝업 차단이 허용하면 새 창을 연다(계획 P31-08). 기능으로 여는 창과 관계가 없는 창
     * (<code>noopener</code>)을 요청할 수 있고 그때는 null 을 준다.
     *
     * @param url      the absolute address, or an empty string to navigate nothing / 절대 주소. 아무것도 이동시키지 않으려면 빈 문자열
     * @param target   the target name or keyword / 대상 이름 또는 키워드
     * @param features the window features text, or the empty string / 창 기능 문자열 또는 빈 문자열
     * @return the context that the target names or the new window, or null / 대상이 고르는 문맥이나 새 창. 없으면 null
     */
    BrowsingContext open(String url, String target, String features);

    /**
     * Returns the context of the document that opened the window of this context with <code>window.open</code>.
     * <p>
     * <code>window.open</code> 으로 이 문맥의 창을 연 문서의 문맥을 반환한다.
     *
     * @return the opener, or null for a nested context and for a window without one / opener. 중첩 문맥이거나 opener 가 없는 창이면 null
     */
    BrowsingContext getOpener();

    /**
     * Tells whether a script of this context may navigate another context: a context that is not sandboxed may navigate any, a sandboxed
     * one only itself, the contexts nested in it, and the top-level context when its sandbox has <code>allow-top-navigation</code> (HTML5 REC
     * 2014-10-28 4.7.2).
     * <p>
     * 이 문맥의 스크립트가 다른 문맥을 이동시킬 수 있는지 알려 준다: 샌드박스가 아닌 문맥은 어떤 문맥이든 이동시킬 수 있고, 샌드박스된 문맥은 자기 자신,
     * 그 안에 중첩된 문맥, <code>allow-top-navigation</code> 이 있을 때의 최상위 문맥만 이동시킬 수 있다(HTML5 REC 2014-10-28 4.7.2).
     *
     * @param target the context to be navigated / 이동시킬 문맥
     * @return true if allowed / 허용이면 true
     */
    boolean mayNavigate(BrowsingContext target);

    /**
     * Tells whether the context was discarded (its frame element was removed or its page was replaced).
     * <p>
     * 문맥이 버려졌는지(프레임 요소가 제거되었거나 페이지가 교체되었는지) 알려 준다.
     *
     * @return true if discarded / 버려졌으면 true
     */
    boolean isClosed();

    /**
     * Tells whether the window of this context is being closed by a script (the "is closing" flag of the HTML Standard, which only a
     * top-level context ever sets). Together with {@link #isClosed()} it gives <code>window.closed</code>: the document of a closing window
     * still runs until the window is really closed.
     * <p>
     * 이 문맥의 창이 스크립트에 의해 닫히는 중인지 알려 준다(HTML 표준의 "is closing" 표시. 최상위 문맥만 설정한다). {@link #isClosed()} 와 함께
     * <code>window.closed</code> 의 값이 된다. 닫는 중인 창의 문서는 창이 실제로 닫힐 때까지 계속 실행된다.
     *
     * @return true while closing / 닫는 중이면 true
     */
    boolean isClosing();

    /**
     * Runs the steps of <code>window.close()</code> on the window of this context for a script (WHATWG HTML 7.2.2.1). Only a top-level
     * window that scripts may close is closed: a window that web content created, or the window of the component when the host allowed it
     * (API contract 5.23). The window becomes closing at once and is closed by a later task of the document queue. A nested context and
     * a window that is closing already ignore the call silently.
     * <p>
     * 스크립트를 위해 이 문맥의 창에 <code>window.close()</code> 의 단계를 실행한다(WHATWG HTML 7.2.2.1). 스크립트가 닫을 수 있는 최상위 창만
     * 닫는다: 웹 콘텐츠가 만든 창, 또는 호스트가 허용한 컴포넌트의 본 창(API 계약 5.23). 창은 곧바로 닫는 중이 되고 문서 큐의 뒤 작업에서 닫힌다.
     * 중첩 문맥과 이미 닫는 중인 창은 호출을 조용히 무시한다.
     *
     * @param caller the context of the script that called (the incumbent), or null when unknown / 호출한 스크립트의 문맥(incumbent). 모르면 null
     * @return null if the call was accepted or silently ignored, otherwise the reason why the window is not closed, for the console of
     *         the caller / 호출을 받았거나 조용히 무시했으면 null. 아니면 창을 닫지 않는 이유(호출한 쪽의 콘솔에 알린다)
     */
    String close(BrowsingContext caller);
}
