package org.duckdns.hjow.swingwebpanel.core;

/**
 * Opens new windows for the pages of a component (plan item P31-08). The component installs it on its session; the session calls it when
 * a page needs a window that does not exist yet.
 * <p>
 * 컴포넌트의 페이지를 위해 새 창을 연다(계획 P31-08). 컴포넌트가 자기 세션에 설치하고, 페이지가 아직 없는 창을 필요로 하면 세션이 부른다.
 */
public interface WindowOpener {

    /**
     * Creates the window of a <code>window.open</code> call. It is called on the thread that runs the calling script and must return
     * at once: the session of the new window has to exist when the call returns, because the script gets its window object as the
     * result. The window itself is shown later on the event dispatch thread.
     * <p>
     * <code>window.open</code> 호출의 창을 만든다. 호출한 스크립트를 실행하는 스레드에서 불리며 곧바로 돌아와야 한다: 스크립트가 결과로 그 창
     * 객체를 받으므로, 호출이 돌아올 때 새 창의 세션이 있어야 한다. 창 자체는 나중에 이벤트 디스패치 스레드에서 보인다.
     *
     * @param request what the script asked for / 스크립트가 요청한 것
     * @param shared  the document loop that the new window must share with its opener, or null for a loop of its own / 새 창이 여는
     *                창과 함께 써야 하는 문서 루프. 자기 루프이면 null
     * @return the session of the new window, or null if no window is opened / 새 창의 세션. 창을 열지 않으면 null
     */
    BrowserSession openFromScript(OpenRequest request, DocumentLoop shared);

    /**
     * Tells whether windows can be opened for scripts at all (a host without a screen cannot show any). It is called on the thread
     * that runs the calling script, before the popup blocker is asked.
     * <p>
     * 스크립트를 위해 창을 열 수 있는지 알려 준다(화면이 없는 호스트는 보일 수 없다). 호출한 스크립트를 실행하는 스레드에서, 팝업 차단을 묻기 전에
     * 불린다.
     *
     * @return true if a window can be shown / 창을 보일 수 있으면 true
     */
    boolean canOpenWindows();

    /**
     * Offers a link, a form submission or a choice of the user that asks for a new window to the host. The host answers later on the
     * event dispatch thread; if it does not take the request, the fallback runs (on the event dispatch thread) so that the page gets
     * the behavior that it had without a host for new windows.
     * <p>
     * 새 창을 요청하는 링크, 폼 제출, 사용자의 선택을 호스트에 제안한다. 호스트는 나중에 이벤트 디스패치 스레드에서 답한다. 호스트가 요청을 받지
     * 않으면 대체 동작이 (이벤트 디스패치 스레드에서) 실행되어, 새 창을 다루는 호스트가 없을 때의 동작을 페이지가 얻는다.
     *
     * @param request  what was asked for / 요청한 것
     * @param fallback what to do if the host does not take the request, or null / 호스트가 요청을 받지 않으면 할 일. 없으면 null
     */
    void openFromLink(OpenRequest request, Runnable fallback);

    /**
     * Tells the host that a window was not opened because the page asked for it without an action of the user.
     * <p>
     * 페이지가 사용자의 조작 없이 요청했기 때문에 창을 열지 않았음을 호스트에 알린다.
     *
     * @param request what was asked for / 요청한 것
     */
    void popupBlocked(OpenRequest request);
}
