package org.duckdns.hjow.swingwebpanel.dom;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The highlight registry of a document (CSS Custom Highlight API Level 1, section 3.2): a map from custom highlight names to {@link Highlight}s, in the
 * order in which the names were first set (as a JavaScript <code>Map</code>; setting an existing name keeps its place). A highlight that is registered later
 * has the higher effective priority among highlights of the same priority (4.2.5). Every change of the registry or of a registered highlight is reported
 * to the listener that the page installs, which paints the page again (section 5.1).
 * <p>
 * Use: {@link Document#getHighlights()} returns the registry of a document; the renderer reads {@link #getEntries()} while it paints the text. Change it
 * on the document thread only.
 * <p>
 * 문서의 하이라이트 등록부(CSS Custom Highlight API Level 1, 3.2 절): 사용자 정의 하이라이트 이름에서 {@link Highlight} 로 가는 맵이며 이름을 처음 정한 순서를 지킨다
 * (JavaScript <code>Map</code> 처럼 이미 있는 이름을 다시 정하면 자리를 지킨다). 같은 우선순위의 하이라이트 중에서는 나중에 등록한 것의 실제 우선순위가 더 높다(4.2.5 절).
 * 등록부나 등록된 하이라이트의 모든 변경은 페이지가 설치한 리스너에 알려 페이지를 다시 그리게 한다(5.1 절).
 * <p>
 * 사용법: {@link Document#getHighlights()} 가 문서의 등록부를 반환하고, 렌더러는 텍스트를 그릴 때 {@link #getEntries()} 를 읽는다. 문서 스레드에서만 바꾼다.
 */
public final class HighlightRegistry {

    /** The registered highlights by name, in registration order. 이름별로 등록한 하이라이트(등록 순서). */
    private final LinkedHashMap<String, Highlight> map = new LinkedHashMap<String, Highlight>();

    /** The receiver of changes (the page paints again), or null. 변경을 받는 쪽(페이지가 다시 그림). 없으면 null. */
    private volatile Runnable listener;

    /**
     * Creates an empty registry.
     * <p>
     * 빈 등록부를 만든다.
     */
    public HighlightRegistry() {
    }

    /**
     * Installs the receiver of the changes (it must only post work).
     * <p>
     * 변경을 받는 쪽을 설치한다(작업을 올리기만 해야 한다).
     *
     * @param listener the receiver or null / 받는 쪽 또는 null
     */
    public void setChangeListener(Runnable listener) {
        this.listener = listener;
    }

    /**
     * Registers a highlight under a name (maplike <code>set</code>).
     * <p>
     * 이름으로 하이라이트를 등록한다(maplike <code>set</code>).
     *
     * @param name      the custom highlight name / 사용자 정의 하이라이트 이름
     * @param highlight the highlight / 하이라이트
     * @return this registry / 이 등록부
     * @throws IllegalArgumentException if the highlight is null / 하이라이트가 null 이면
     */
    public HighlightRegistry set(String name, Highlight highlight) {
        if (highlight == null) {
            throw new IllegalArgumentException("not a Highlight");
        }
        Highlight old = map.put(name, highlight);
        if (old != null) {
            old.detach(this);
        }
        highlight.attach(this);
        changed();
        return this;
    }

    /**
     * Returns the highlight of a name (maplike <code>get</code>).
     * <p>
     * 이름의 하이라이트를 반환한다(maplike <code>get</code>).
     *
     * @param name the name / 이름
     * @return the highlight or null / 하이라이트. 없으면 null
     */
    public Highlight get(String name) {
        return map.get(name);
    }

    /**
     * Tells whether a name is registered (maplike <code>has</code>).
     * <p>
     * 이름이 등록되어 있는지 알려 준다(maplike <code>has</code>).
     *
     * @param name the name / 이름
     * @return true if it is / 등록되어 있으면 true
     */
    public boolean has(String name) {
        return map.containsKey(name);
    }

    /**
     * Removes a name (maplike <code>delete</code>).
     * <p>
     * 이름을 지운다(maplike <code>delete</code>).
     *
     * @param name the name / 이름
     * @return true if it was registered / 등록되어 있었으면 true
     */
    public boolean delete(String name) {
        Highlight old = map.remove(name);
        if (old == null) {
            return false;
        }
        old.detach(this);
        changed();
        return true;
    }

    /**
     * Removes every name (maplike <code>clear</code>).
     * <p>
     * 모든 이름을 지운다(maplike <code>clear</code>).
     */
    public void clear() {
        if (map.isEmpty()) {
            return;
        }
        // 등록된 하이라이트마다 이 등록부의 기록을 지운다.
        for (Iterator<Highlight> it = map.values().iterator(); it.hasNext();) {
            it.next().detach(this);
        }
        map.clear();
        changed();
    }

    /**
     * Returns the number of registered names (maplike <code>size</code>).
     * <p>
     * 등록된 이름의 수를 반환한다(maplike <code>size</code>).
     *
     * @return the size / 크기
     */
    public int getSize() {
        return map.size();
    }

    /**
     * Returns a snapshot of the entries in registration order.
     * <p>
     * 등록 순서의 항목 스냅샷을 반환한다.
     *
     * @return the entries / 항목
     */
    public List<Map.Entry<String, Highlight>> getEntries() {
        return new ArrayList<Map.Entry<String, Highlight>>(map.entrySet());
    }

    /**
     * Tells the listener that what the highlights cover may have changed.
     * <p>
     * 하이라이트가 덮는 것이 바뀌었을 수 있음을 리스너에 알린다.
     */
    void changed() {
        Runnable l = listener;
        if (l != null) {
            l.run();
        }
    }
}
