package org.duckdns.hjow.swingwebpanel.core;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.FindResult;
import org.duckdns.hjow.swingwebpanel.render.RenderedPage;
import org.duckdns.hjow.swingwebpanel.render.SelectionHighlight;
import org.duckdns.hjow.swingwebpanel.render.TextSearch;
import org.duckdns.hjow.swingwebpanel.render.TextSelections;

/**
 * The search in the page of a component (plan item P31-09, API contract 5.29). The host starts or continues a search from any thread;
 * the search itself runs on the document queue against the text of the current rendering ({@link TextSearch}), and its result (the
 * count, the current match, the highlights) is published on the event dispatch thread. All matches are highlighted, the current one in
 * another color, and the view is scrolled to the current match. After every new layout the search is done again, so the highlights
 * follow a page that changes; when the page is replaced the search ends.
 * <p>
 * 컴포넌트의 페이지 안 찾기(계획 P31-09, API 계약 5.29). 호스트는 어느 스레드에서나 찾기를 시작하거나 이어 간다. 찾기 자체는 문서 큐에서 현재
 * 렌더링의 글({@link TextSearch})을 대상으로 실행되고, 그 결과(개수, 현재 일치, 강조)는 이벤트 디스패치 스레드에서 게시된다. 모든 일치를 강조하되
 * 현재 일치는 다른 색으로 강조하고, 뷰를 현재 일치로 스크롤한다. 새 레이아웃마다 다시 찾으므로 강조는 바뀌는 페이지를 따라간다. 페이지가 교체되면
 * 찾기는 끝난다.
 */
public final class PageFind {

    /** The ARGB color of the matches: translucent yellow. / 일치의 ARGB 색: 반투명 노랑. */
    public static final int MATCH_COLOR = 0x80FFE14D;

    /** The ARGB color of the current match: translucent orange. / 현재 일치의 ARGB 색: 반투명 주황. */
    public static final int CURRENT_COLOR = 0xB0FF8A1F;

    /**
     * Receives the results of the search on the event dispatch thread.
     * <p>
     * 찾기의 결과를 이벤트 디스패치 스레드에서 받는다.
     */
    public interface Listener {

        /**
         * Called when the result of the search changed.
         * <p>
         * 찾기의 결과가 바뀌면 호출된다.
         *
         * @param result the new result / 새 결과
         */
        void findResultChanged(FindResult result);
    }

    /** The page component, which is repainted and scrolled. / 다시 그리고 스크롤할 페이지 컴포넌트. */
    private final JComponent component;

    /** The session whose document queue runs the search. / 문서 큐가 찾기를 실행하는 세션. */
    private final BrowserSession session;

    /** The view that knows the current rendering. / 현재 렌더링을 아는 뷰. */
    private final PageView view;

    /** The receiver of the results. / 결과를 받는 쪽. */
    private final Listener listener;

    /** The search text of the active search, empty for none (guarded by this object). / 활성 찾기의 검색어. 없으면 빈 문자열(이 객체로 보호). */
    private String text = "";

    /** Whether the active search tells upper and lower case apart (guarded by this object). / 활성 찾기가 대소문자를 구분하는지(이 객체로 보호). */
    private boolean matchCase;

    /** The page that the active search belongs to (written on the document thread; dropped by {@link #pageClosed()}). / 활성 찾기가 속한 페이지(문서 스레드에서 쓴다. {@link #pageClosed()} 가 버린다). */
    private volatile PageSnapshot searchedPage;

    /** The text of the rendering that was searched last (document thread; dropped by {@link #pageClosed()}). / 마지막으로 찾은 렌더링의 글(문서 스레드. {@link #pageClosed()} 가 버린다). */
    private volatile TextSearch searched;

    /** The matches of the last search (document thread). / 마지막 찾기의 일치(문서 스레드). */
    private List<int[]> matches = new ArrayList<int[]>();

    /** The position of the current match in {@link #matches}, -1 for none (document thread). / {@link #matches} 안에서 현재 일치의 위치. 없으면 -1(문서 스레드). */
    private int current = -1;

    /** The highlight of all matches but the current one, or null. / 현재 일치를 뺀 모든 일치의 강조. 없으면 null. */
    private volatile SelectionHighlight others;

    /** The highlight of the current match, or null. / 현재 일치의 강조. 없으면 null. */
    private volatile SelectionHighlight active;

    /** The result that was published last. / 마지막으로 게시한 결과. */
    private volatile FindResult result = FindResult.NONE;

    /** Set once the component was disposed. / 컴포넌트가 폐기되면 true. */
    private volatile boolean disposed;

    /**
     * Creates the search of a component.
     * <p>
     * 컴포넌트의 찾기를 만든다.
     *
     * @param component the page component / 페이지 컴포넌트
     * @param session   the session / 세션
     * @param view      the view / 뷰
     * @param listener  the receiver of the results / 결과를 받는 쪽
     */
    public PageFind(JComponent component, BrowserSession session, PageView view, Listener listener) {
        this.component = component;
        this.session = session;
        this.view = view;
        this.listener = listener;
    }

    /**
     * Starts a search, or goes to the next or the previous match if the same text is searched for already. An empty text ends the
     * search. It may be called from any thread; the work is queued.
     * <p>
     * 찾기를 시작한다. 이미 같은 글을 찾고 있으면 다음 또는 이전 일치로 간다. 빈 글은 찾기를 끝낸다. 어느 스레드에서나 부를 수 있으며 작업은 큐에
     * 들어간다.
     *
     * @param query     the search text / 검색어
     * @param caseSense true to tell upper and lower case apart / 대소문자를 구분하려면 true
     * @param forward   true for the next match, false for the previous one / 다음 일치이면 true, 이전 일치이면 false
     */
    public void find(String query, final boolean caseSense, final boolean forward) {
        final String wanted = TextSearch.normalize(query);
        // 공백뿐인 글은 찾을 것이 없다.
        if (wanted.trim().length() == 0) {
            clear();
            return;
        }
        if (disposed) {
            return;
        }
        if (!session.submitDocumentTask(new Runnable() {
            /** Searches on the document queue. / 문서 큐에서 찾는다. */
            public void run() {
                search(wanted, caseSense, forward);
            }
        })) {
            publish(FindResult.NONE, null, null, null);
        }
    }

    /**
     * Ends the search: the highlights disappear and the result becomes {@link FindResult#NONE}. It may be called from any thread.
     * <p>
     * 찾기를 끝낸다: 강조가 사라지고 결과는 {@link FindResult#NONE} 이 된다. 어느 스레드에서나 부를 수 있다.
     */
    public void clear() {
        synchronized (this) {
            text = "";
            matchCase = false;
        }
        if (disposed) {
            return;
        }
        if (!session.submitDocumentTask(new Runnable() {
            /** Forgets the matches on the document queue. / 문서 큐에서 일치를 잊는다. */
            public void run() {
                forget();
                publish(FindResult.NONE, null, null, null);
            }
        })) {
            publish(FindResult.NONE, null, null, null);
        }
    }

    /**
     * Forgets the matches of the last search (document thread).
     * <p>
     * 마지막 찾기의 일치를 잊는다(문서 스레드).
     */
    private void forget() {
        matches = new ArrayList<int[]>();
        current = -1;
        searched = null;
        searchedPage = null;
    }

    /**
     * Runs a search request on the document thread.
     * <p>
     * 문서 스레드에서 찾기 요청을 실행한다.
     *
     * @param wanted    the normalized search text / 손질한 검색어
     * @param caseSense true to tell upper and lower case apart / 대소문자를 구분하려면 true
     * @param forward   the direction / 방향
     */
    private void search(String wanted, boolean caseSense, boolean forward) {
        if (disposed) {
            return;
        }
        PageSnapshot snapshot = session.getSnapshot();
        RenderedPage page = view.getRenderedPage();
        boolean same;
        synchronized (this) {
            same = wanted.equals(text) && caseSense == matchCase && snapshot == searchedPage;
            text = wanted;
            matchCase = caseSense;
        }
        if (page == null) {
            // 그려진 것이 없는 페이지(빈 페이지, 아직 배치되지 않은 페이지)에는 일치가 없다. 배치가 생기면 다시 찾는다.
            forget();
            searchedPage = snapshot;
            publish(new FindResult(wanted, caseSense, 0, 0), null, null, null);
            return;
        }
        TextSearch before = searched;
        int anchor = current >= 0 && current < matches.size() ? matches.get(current)[0] : -1;
        TextSearch now = before != null && before.getPage() == page ? before : new TextSearch(page, new TextSelections(page));
        List<int[]> found = now.find(wanted, caseSense);
        int next;
        if (found.isEmpty()) {
            next = -1;
        } else if (same && before == now && current >= 0) {
            // 같은 찾기를 이어 가면 다음이나 이전 일치로 가고 끝에서는 반대쪽 끝으로 돈다.
            next = ((current + (forward ? 1 : -1)) % found.size() + found.size()) % found.size();
        } else if (before == now && anchor >= 0) {
            // 같은 렌더링에서 글만 바뀌었으면(이어 입력) 지금의 일치가 시작한 곳부터 찾는다.
            next = firstFrom(found, anchor, forward);
        } else {
            next = firstVisible(now, found, forward);
        }
        searched = now;
        searchedPage = snapshot;
        matches = found;
        current = next;
        show(now, wanted, caseSense, true);
    }

    /**
     * Picks the first match that starts at or behind a position of the text (or, backwards, the last one that starts at or before it).
     * <p>
     * 글의 한 위치에서 또는 그 뒤에서 시작하는 첫 일치를 고른다(뒤로 찾을 때는 그 위치에서 또는 그 앞에서 시작하는 마지막 일치).
     *
     * @param found   the matches / 일치
     * @param anchor  the position / 위치
     * @param forward the direction / 방향
     * @return the position of the match in the list / 목록에서 일치의 위치
     */
    private static int firstFrom(List<int[]> found, int anchor, boolean forward) {
        if (forward) {
            // 기준 위치 이후에 시작하는 첫 일치를 찾는다. 없으면 처음으로 돈다.
            for (int i = 0; i < found.size(); i++) {
                if (found.get(i)[0] >= anchor) {
                    return i;
                }
            }
            return 0;
        }
        // 기준 위치 이전에 시작하는 마지막 일치를 찾는다. 없으면 끝으로 돈다.
        for (int i = found.size() - 1; i >= 0; i--) {
            if (found.get(i)[0] <= anchor) {
                return i;
            }
        }
        return found.size() - 1;
    }

    /**
     * Picks the match that a new search starts with: the first one that is not above the visible area (or, backwards, the last one
     * that is not below it).
     * <p>
     * 새 찾기가 시작할 일치를 고른다: 보이는 영역보다 위에 있지 않은 첫 일치(뒤로 찾을 때는 보이는 영역보다 아래에 있지 않은 마지막 일치).
     *
     * @param now     the text of the rendering / 렌더링의 글
     * @param found   the matches / 일치
     * @param forward the direction / 방향
     * @return the position of the match in the list / 목록에서 일치의 위치
     */
    private int firstVisible(TextSearch now, List<int[]> found, boolean forward) {
        double top = view.viewportPosition().y;
        double bottom = top + view.viewportHeight();
        if (forward) {
            // 아래쪽 가장자리가 보이는 영역의 맨 위보다 아래인 첫 일치를 찾는다.
            for (int i = 0; i < found.size(); i++) {
                if (now.bottomOf(found.get(i)) > top) {
                    return i;
                }
            }
            return 0;
        }
        // 보이는 영역의 맨 아래보다 위에서 시작하는 마지막 일치를 찾는다.
        for (int i = found.size() - 1; i >= 0; i--) {
            if (now.bottomOf(found.get(i)) <= bottom) {
                return i;
            }
        }
        return found.size() - 1;
    }

    /**
     * Builds the highlights of the matches and publishes them with the result.
     * <p>
     * 일치의 강조를 만들어 결과와 함께 게시한다.
     *
     * @param now       the text of the rendering / 렌더링의 글
     * @param wanted    the search text / 검색어
     * @param caseSense whether case matters / 대소문자 구분 여부
     * @param reveal    true to scroll to the current match / 현재 일치로 스크롤하려면 true
     */
    private void show(TextSearch now, String wanted, boolean caseSense, boolean reveal) {
        SelectionHighlight all = now.highlight(matches, 0, matches.size(), current, MATCH_COLOR);
        SelectionHighlight one = null;
        Rectangle2D bounds = null;
        if (current >= 0) {
            one = now.highlight(matches, current, current + 1, -1, CURRENT_COLOR);
            bounds = one == null ? null : one.getBounds();
        }
        publish(new FindResult(wanted, caseSense, matches.size(), current + 1), all, one, reveal ? bounds : null);
    }

    /**
     * Called on the document thread after the page got a new rendering: an active search is done again on the new text, keeping the
     * number of the current match.
     * <p>
     * 페이지가 새 렌더링을 얻은 뒤 문서 스레드에서 호출된다: 활성 찾기를 새 글에서 다시 하고 현재 일치의 번호를 유지한다.
     *
     * @param snapshot the page / 페이지
     */
    void layoutChanged(PageSnapshot snapshot) {
        String wanted;
        boolean caseSense;
        synchronized (this) {
            wanted = text;
            caseSense = matchCase;
        }
        if (disposed || wanted.length() == 0 || session.getSnapshot() != snapshot) {
            return;
        }
        // 다른 페이지에서 시작한 찾기는 새 페이지로 넘어오지 않는다.
        if (searchedPage != null && searchedPage != snapshot) {
            return;
        }
        RenderedPage page = view.getRenderedPage();
        if (page == null) {
            return;
        }
        TextSearch now = new TextSearch(page, new TextSelections(page));
        List<int[]> found = now.find(wanted, caseSense);
        int next = found.isEmpty() ? -1 : current < 0 ? firstVisible(now, found, true) : Math.min(current, found.size() - 1);
        searched = now;
        searchedPage = snapshot;
        matches = found;
        current = next;
        show(now, wanted, caseSense, false);
    }

    /**
     * Forgets everything that refers to the page that was shown (the page was replaced, cleaned or disposed) and ends the search. It
     * may be called from any thread.
     * <p>
     * 보이던 페이지를 가리키는 모든 것을 잊고(페이지가 교체, clean, dispose 되었다) 찾기를 끝낸다. 어느 스레드에서나 부를 수 있다.
     */
    void pageClosed() {
        boolean was;
        synchronized (this) {
            was = text.length() > 0;
            text = "";
            matchCase = false;
        }
        searched = null;
        searchedPage = null;
        if (was || others != null || active != null) {
            publish(FindResult.NONE, null, null, null);
        }
    }

    /**
     * Publishes a result and its highlights on the event dispatch thread, repaints, and scrolls to the current match if asked.
     * <p>
     * 결과와 그 강조를 이벤트 디스패치 스레드에서 게시하고, 다시 그리며, 요청했으면 현재 일치로 스크롤한다.
     *
     * @param next   the result / 결과
     * @param all    the highlight of the other matches or null / 다른 일치의 강조 또는 null
     * @param one    the highlight of the current match or null / 현재 일치의 강조 또는 null
     * @param reveal the bounds to scroll to (document coordinates), or null / 스크롤해 갈 경계(문서 좌표) 또는 null
     */
    private void publish(final FindResult next, final SelectionHighlight all, final SelectionHighlight one, final Rectangle2D reveal) {
        others = all;
        active = one;
        final double zoom = view.getZoom();
        Runnable post = new Runnable() {
            /** Announces the result and repaints. / 결과를 알리고 다시 그린다. */
            public void run() {
                if (disposed) {
                    return;
                }
                FindResult old = result;
                result = next;
                component.repaint();
                if (reveal != null) {
                    // 일치 주위에 여유를 두어 가장자리에 붙지 않게 한다.
                    int margin = 40;
                    Rectangle wanted = new Rectangle((int) Math.floor(reveal.getX() * zoom) - margin,
                            (int) Math.floor(reveal.getY() * zoom) - margin, (int) Math.ceil(reveal.getWidth() * zoom) + 2 * margin,
                            (int) Math.ceil(reveal.getHeight() * zoom) + 2 * margin);
                    // 여유 때문에 내용 밖으로 나간 부분은 잘라 낸다(뷰가 내용 밖으로 스크롤되지 않게 한다).
                    Rectangle content = new Rectangle(0, 0, Math.max(component.getWidth(), component.getPreferredSize().width),
                            Math.max(component.getHeight(), component.getPreferredSize().height));
                    component.scrollRectToVisible(wanted.intersection(content));
                }
                if (!old.equals(next)) {
                    listener.findResultChanged(next);
                }
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            post.run();
        } else {
            SwingUtilities.invokeLater(post);
        }
    }

    /**
     * Returns the result that was published last.
     * <p>
     * 마지막으로 게시한 결과를 반환한다.
     *
     * @return the result; {@link FindResult#NONE} while nothing is searched for / 결과. 아무것도 찾지 않는 동안에는 {@link FindResult#NONE}
     */
    public FindResult getResult() {
        return result;
    }

    /**
     * Returns the highlight of the matches other than the current one.
     * <p>
     * 현재 일치가 아닌 일치들의 강조를 반환한다.
     *
     * @return the highlight or null / 강조 또는 null
     */
    SelectionHighlight getMatches() {
        return others;
    }

    /**
     * Returns the highlight of the current match.
     * <p>
     * 현재 일치의 강조를 반환한다.
     *
     * @return the highlight or null / 강조 또는 null
     */
    SelectionHighlight getCurrent() {
        return active;
    }

    /**
     * Stops the search for good (the component is disposed).
     * <p>
     * 찾기를 영구히 멈춘다(컴포넌트가 폐기된다).
     */
    public void dispose() {
        disposed = true;
        synchronized (this) {
            text = "";
        }
        searched = null;
        others = null;
        active = null;
        result = FindResult.NONE;
    }
}
