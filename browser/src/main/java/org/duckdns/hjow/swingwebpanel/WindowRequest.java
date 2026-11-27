package org.duckdns.hjow.swingwebpanel;

import org.duckdns.hjow.swingwebpanel.core.OpenRequest;

/**
 * A new window that a page of a {@link SwingWebPanel} asked for (extension, API contract 5.28). The component passes it to the
 * extension points {@link SwingWebPanel#openPopupWindow(WindowRequest)}, {@link SwingWebPanel#openNewWindow(WindowRequest)} and
 * {@link SwingWebPanel#popupBlocked(WindowRequest)}, always on the event dispatch thread. There are three kinds:
 * <ul>
 * <li>{@link #SCRIPT}: a script called <code>window.open</code>. The component of the new window exists already
 * ({@link #getPanel()}), because the script holds its window object; the host must show it somewhere or call {@link #reject()}.</li>
 * <li>{@link #LINK}: a link or a form whose target names no existing window. {@link #getPanel()} creates the component of a new
 * window (which loads the address, or receives the form) when the host decides to open one.</li>
 * <li>{@link #USER}: the user chose to open a link elsewhere (Ctrl or middle click for a new tab, Shift click for a new window;
 * {@link #getPlace()} tells which). The host can open {@link #getUrl()} in a component of its own or use {@link #getPanel()}.</li>
 * </ul>
 * <p>
 * {@link SwingWebPanel} 의 페이지가 요청한 새 창(확장, API 계약 5.28). 컴포넌트가 확장 지점
 * {@link SwingWebPanel#openPopupWindow(WindowRequest)}, {@link SwingWebPanel#openNewWindow(WindowRequest)},
 * {@link SwingWebPanel#popupBlocked(WindowRequest)} 에 넘기며, 항상 이벤트 디스패치 스레드에서 넘긴다. 세 종류가 있다:
 * <ul>
 * <li>{@link #SCRIPT}: 스크립트가 <code>window.open</code> 을 불렀다. 스크립트가 그 창 객체를 쥐고 있으므로 새 창의 컴포넌트가 이미
 * 있다({@link #getPanel()}). 호스트는 그것을 어딘가에 보이거나 {@link #reject()} 를 불러야 한다.</li>
 * <li>{@link #LINK}: 대상이 기존 창을 가리키지 않는 링크나 폼. 호스트가 창을 열기로 하면 {@link #getPanel()} 이 새 창의 컴포넌트를 만든다(주소를
 * 읽거나 폼을 받는다).</li>
 * <li>{@link #USER}: 사용자가 링크를 다른 곳에서 열기로 골랐다(Ctrl 이나 가운데 클릭은 새 탭, Shift 클릭은 새 창. {@link #getPlace()} 가
 * 알려 준다). 호스트는 {@link #getUrl()} 을 자기 컴포넌트에서 열거나 {@link #getPanel()} 을 쓸 수 있다.</li>
 * </ul>
 * <p>
 * 사용 예 / Example:
 * <pre>
 * protected void openPopupWindow(WindowRequest request) {
 *     JFrame frame = new JFrame();
 *     frame.add(new JScrollPane(request.getPanel()));
 *     frame.setSize(600, 400);
 *     frame.setVisible(true);
 * }
 * </pre>
 */
public final class WindowRequest {

    /** Kind: a script called <code>window.open</code>. / 종류: 스크립트가 <code>window.open</code> 을 불렀다. */
    public static final int SCRIPT = OpenRequest.SCRIPT;

    /** Kind: a link or a form whose target needs a new window. / 종류: 대상이 새 창을 필요로 하는 링크나 폼. */
    public static final int LINK = OpenRequest.LINK;

    /** Kind: the user chose to open a link in a new tab or window. / 종류: 사용자가 링크를 새 탭이나 창에서 열기로 골랐다. */
    public static final int USER = OpenRequest.USER;

    /** Place of a {@link #USER} request: a new tab. / {@link #USER} 요청의 장소: 새 탭. */
    public static final String PLACE_TAB = OpenRequest.PLACE_TAB;

    /** Place of a {@link #USER} request: a new window. / {@link #USER} 요청의 장소: 새 창. */
    public static final String PLACE_WINDOW = OpenRequest.PLACE_WINDOW;

    /** The component whose page asked. / 페이지가 요청한 컴포넌트. */
    private final SwingWebPanel source;

    /** What the page asked for. / 페이지가 요청한 것. */
    private final OpenRequest request;

    /** The component of the new window, or null while none was created. / 새 창의 컴포넌트. 아직 만들지 않았으면 null. */
    private SwingWebPanel panel;

    /** True once {@link #reject()} was called. / {@link #reject()} 가 불린 뒤 true. */
    private boolean rejected;

    /** True once the popup blocker refused to create the component. / 팝업 차단이 컴포넌트 만들기를 거부한 뒤 true. */
    private boolean blocked;

    /**
     * Creates the request; only the component does this.
     * <p>
     * 요청을 만든다. 컴포넌트만 만든다.
     *
     * @param source  the component whose page asked / 페이지가 요청한 컴포넌트
     * @param request what the page asked for / 페이지가 요청한 것
     * @param panel   the component of the new window if it exists already, or null / 새 창의 컴포넌트가 이미 있으면 그것, 없으면 null
     */
    WindowRequest(SwingWebPanel source, OpenRequest request, SwingWebPanel panel) {
        this.source = source;
        this.request = request;
        this.panel = panel;
    }

    /**
     * Returns the kind of the request.
     * <p>
     * 요청의 종류를 반환한다.
     *
     * @return {@link #SCRIPT}, {@link #LINK} or {@link #USER} / {@link #SCRIPT}, {@link #LINK}, {@link #USER} 중 하나
     */
    public int getKind() {
        return request.getKind();
    }

    /**
     * Returns the component whose page asked for the window.
     * <p>
     * 페이지가 창을 요청한 컴포넌트를 반환한다.
     *
     * @return the component / 컴포넌트
     */
    public SwingWebPanel getSource() {
        return source;
    }

    /**
     * Returns the address that the window opens.
     * <p>
     * 창이 여는 주소를 반환한다.
     *
     * @return the absolute address, or the empty string for the empty page / 절대 주소. 빈 페이지이면 빈 문자열
     */
    public String getUrl() {
        return request.getUrl();
    }

    /**
     * Returns the target name that the page gave.
     * <p>
     * 페이지가 준 대상 이름을 반환한다.
     *
     * @return the name, or the empty string for <code>_blank</code> / 이름. <code>_blank</code> 이면 빈 문자열
     */
    public String getTargetName() {
        return request.getTarget();
    }

    /**
     * Returns the window features text of <code>window.open</code>.
     * <p>
     * <code>window.open</code> 의 창 기능 문자열을 반환한다.
     *
     * @return the text as the script gave it, or the empty string / 스크립트가 준 그대로의 글. 없으면 빈 문자열
     */
    public String getFeatures() {
        return request.getFeatures();
    }

    /**
     * Returns the address of the document that asked for the window.
     * <p>
     * 창을 요청한 문서의 주소를 반환한다.
     *
     * @return the address / 주소
     */
    public String getDocumentUrl() {
        return request.getDocumentUrl();
    }

    /**
     * Returns the width that the features ask for.
     * <p>
     * 창 기능이 요청한 폭을 반환한다.
     *
     * @return the width of the content in CSS pixels, or -1 if none was given / 내용의 폭(CSS 픽셀). 주지 않았으면 -1
     */
    public int getWidth() {
        return request.getWidth();
    }

    /**
     * Returns the height that the features ask for.
     * <p>
     * 창 기능이 요청한 높이를 반환한다.
     *
     * @return the height of the content in CSS pixels, or -1 if none was given / 내용의 높이(CSS 픽셀). 주지 않았으면 -1
     */
    public int getHeight() {
        return request.getHeight();
    }

    /**
     * Returns the left edge on the screen that the features ask for.
     * <p>
     * 창 기능이 요청한 화면의 왼쪽 가장자리를 반환한다.
     *
     * @return pixels, or -1 if none was given / 픽셀. 주지 않았으면 -1
     */
    public int getLeft() {
        return request.getLeft();
    }

    /**
     * Returns the top edge on the screen that the features ask for.
     * <p>
     * 창 기능이 요청한 화면의 위쪽 가장자리를 반환한다.
     *
     * @return pixels, or -1 if none was given / 픽셀. 주지 않았으면 -1
     */
    public int getTop() {
        return request.getTop();
    }

    /**
     * Returns where the user wants a {@link #USER} request to open.
     * <p>
     * 사용자가 {@link #USER} 요청을 열고자 하는 곳을 반환한다.
     *
     * @return {@link #PLACE_TAB} or {@link #PLACE_WINDOW}; the empty string for the other kinds / {@link #PLACE_TAB} 또는
     *         {@link #PLACE_WINDOW}. 다른 종류이면 빈 문자열
     */
    public String getPlace() {
        return request.getPlace();
    }

    /**
     * Returns the component of the new window. For a {@link #SCRIPT} request it exists already. For the other kinds the first call
     * creates it and starts loading the address (or sending the form); a {@link #LINK} window counts as created by web content, so its
     * page may close it with <code>window.close()</code>. The host puts the component into a window or a tab and disposes it when that
     * is closed. Call it on the event dispatch thread.
     * <p>
     * 새 창의 컴포넌트를 반환한다. {@link #SCRIPT} 요청이면 이미 있다. 다른 종류이면 첫 호출이 컴포넌트를 만들고 주소 읽기(또는 폼 보내기)를
     * 시작한다. {@link #LINK} 창은 웹 콘텐츠가 만든 것으로 치므로 그 페이지가 <code>window.close()</code> 로 닫을 수 있다. 호스트는 컴포넌트를
     * 창이나 탭에 넣고, 그것이 닫힐 때 컴포넌트를 폐기한다. 이벤트 디스패치 스레드에서 부른다.
     *
     * @return the component, or null if the request was rejected or no component could be created / 컴포넌트. 요청을 거절했거나
     *         컴포넌트를 만들 수 없었으면 null
     */
    public SwingWebPanel getPanel() {
        if (rejected || blocked) {
            return null;
        }
        if (panel == null && request.getKind() != SCRIPT) {
            // 스크립트가 사용자의 조작 없이 새 창으로 제출한 폼은 팝업 차단의 대상이다. 창을 만들지 않고 호스트에 알린다.
            if (!request.isActivated() && source.isPopupBlockingEnabled()) {
                blocked = true;
                source.popupBlocked(this);
                return null;
            }
            panel = source.createLinkedPanel(request);
        }
        return panel;
    }

    /**
     * Tells whether the popup blocker refused the window when the host asked for its component: {@link #getPanel()} returned null and
     * {@link SwingWebPanel#popupBlocked(WindowRequest)} was told. The page then gets no window and no other navigation.
     * <p>
     * 호스트가 컴포넌트를 요청했을 때 팝업 차단이 창을 거부했는지 알려 준다: {@link #getPanel()} 이 null 을 돌려주었고
     * {@link SwingWebPanel#popupBlocked(WindowRequest)} 에 알렸다. 그때 페이지는 창도 다른 이동도 얻지 못한다.
     *
     * @return true if blocked / 차단했으면 true
     */
    public boolean isBlocked() {
        return blocked;
    }

    /**
     * Tells whether the component of the new window exists.
     * <p>
     * 새 창의 컴포넌트가 있는지 알려 준다.
     *
     * @return true once it was created / 만들어진 뒤 true
     */
    public boolean hasPanel() {
        return panel != null && !rejected;
    }

    /**
     * Declines the request: the component of the new window, if it exists, is disposed, so that the script that opened it sees the
     * window as closed. Calling it again does nothing.
     * <p>
     * 요청을 거절한다: 새 창의 컴포넌트가 있으면 폐기하여, 그 창을 연 스크립트가 창을 닫힌 것으로 보게 한다. 다시 불러도 아무 일도 없다.
     */
    public void reject() {
        if (rejected) {
            return;
        }
        rejected = true;
        if (panel != null) {
            panel.disposeKeepingStorage();
        }
    }

    /**
     * Tells whether the request was declined.
     * <p>
     * 요청을 거절했는지 알려 준다.
     *
     * @return true after {@link #reject()} / {@link #reject()} 뒤에 true
     */
    public boolean isRejected() {
        return rejected;
    }
}
