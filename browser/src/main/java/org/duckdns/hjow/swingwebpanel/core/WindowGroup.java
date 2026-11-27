package org.duckdns.hjow.swingwebpanel.core;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * The windows that can reach each other through <code>window.open</code> and <code>window.opener</code> (plan item P31-08-c): a window
 * starts in a group of its own, and a window that a page opens joins the group of its opener. A target name (of a link, a form or
 * <code>window.open</code>) that names no frame is looked up among the windows of the group, so that a second
 * <code>window.open(url, 'name')</code> uses the window that the first one opened. The group holds its windows weakly.
 * <p>
 * <code>window.open</code> 과 <code>window.opener</code> 로 서로 닿을 수 있는 창들(계획 P31-08-c): 창은 자기만의 그룹에서 시작하고, 페이지가 연
 * 창은 여는 창의 그룹에 들어간다. 프레임을 가리키지 않는 대상 이름(링크, 폼, <code>window.open</code>)은 그룹의 창에서 찾으므로, 두 번째
 * <code>window.open(url, 'name')</code> 은 첫 번째가 연 창을 쓴다. 그룹은 창을 약하게 붙든다.
 */
final class WindowGroup {

    /** The windows of the group. / 그룹의 창들. */
    private final List<WeakReference<BrowserSession>> sessions = new ArrayList<WeakReference<BrowserSession>>();

    /**
     * Creates an empty group.
     * <p>
     * 빈 그룹을 만든다.
     */
    WindowGroup() {
    }

    /**
     * Adds a window to the group.
     * <p>
     * 그룹에 창을 더한다.
     *
     * @param session the session of the window / 창의 세션
     */
    synchronized void add(BrowserSession session) {
        // 이미 있는 창은 다시 더하지 않고, 수거된 창의 자리는 치운다.
        for (int i = sessions.size() - 1; i >= 0; i--) {
            BrowserSession s = sessions.get(i).get();
            if (s == null) {
                sessions.remove(i);
            } else if (s == session) {
                return;
            }
        }
        sessions.add(new WeakReference<BrowserSession>(session));
    }

    /**
     * Takes a window out of the group.
     * <p>
     * 그룹에서 창을 뺀다.
     *
     * @param session the session of the window / 창의 세션
     */
    synchronized void remove(BrowserSession session) {
        // 그 창과 수거된 창의 자리를 치운다.
        for (int i = sessions.size() - 1; i >= 0; i--) {
            BrowserSession s = sessions.get(i).get();
            if (s == null || s == session) {
                sessions.remove(i);
            }
        }
    }

    /**
     * Returns the windows of the group that still exist, in the order they joined.
     * <p>
     * 아직 있는 그룹의 창을 들어온 순서대로 반환한다.
     *
     * @return the sessions / 세션들
     */
    synchronized List<BrowserSession> members() {
        List<BrowserSession> out = new ArrayList<BrowserSession>();
        // 수거되지 않은 창만 모은다.
        for (int i = 0; i < sessions.size(); i++) {
            BrowserSession s = sessions.get(i).get();
            if (s != null) {
                out.add(s);
            }
        }
        return out;
    }
}
