package org.duckdns.hjow.swingwebpanel.desktopapp.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The settings of the browser application: named text values with defaults, shared by all windows of the process. A change is announced
 * to the listeners, which apply it to the open windows. The values are plain strings so that they can be stored in a text file; the typed
 * methods convert them and fall back to the default when a stored value cannot be understood.
 * <p>
 * 브라우저 응용 프로그램의 설정: 기본값이 있는 이름 붙은 텍스트 값이며 프로세스의 모든 창이 공유한다. 변경은 리스너에 알리고, 리스너가 열린 창에
 * 적용한다. 값은 텍스트 파일에 저장할 수 있도록 보통 문자열이다. 형식이 있는 메서드가 값을 변환하며, 저장된 값을 이해할 수 없으면 기본값을 쓴다.
 * <p>
 * 사용 예 / Example:
 * <pre>
 * Settings s = new Settings();
 * s.set(Settings.HOME_PAGE, "https://example.com/");
 * String home = s.get(Settings.HOME_PAGE);
 * </pre>
 */
public final class Settings {

    /** Key of the address that the home button opens. / 홈 버튼이 여는 주소의 키. */
    public static final String HOME_PAGE = "home.page";

    /** Key of the search address template (<code>%s</code> is the query); empty for none. / 검색 주소 서식의 키(<code>%s</code> 가 질의). 없으면 빈 값. */
    public static final String SEARCH_TEMPLATE = "search.template";

    /** Key of what a new tab shows: {@link #NEW_TAB_BLANK} or {@link #NEW_TAB_HOME}. / 새 탭이 보이는 것의 키: {@link #NEW_TAB_BLANK} 또는 {@link #NEW_TAB_HOME}. */
    public static final String NEW_TAB_PAGE = "tabs.newTabPage";

    /** Value of {@link #NEW_TAB_PAGE}: a new tab shows the empty page. / {@link #NEW_TAB_PAGE} 의 값: 새 탭이 빈 페이지를 보인다. */
    public static final String NEW_TAB_BLANK = "blank";

    /** Value of {@link #NEW_TAB_PAGE}: a new tab shows the home page. / {@link #NEW_TAB_PAGE} 의 값: 새 탭이 홈 페이지를 보인다. */
    public static final String NEW_TAB_HOME = "home";

    /** Key of whether closing a window with several tabs asks first (<code>true</code> or <code>false</code>). / 탭이 여럿인 창을 닫을 때 먼저 묻는지의 키(<code>true</code> 또는 <code>false</code>). */
    public static final String CONFIRM_CLOSE_TABS = "tabs.confirmClose";

    /**
     * Receives the changes of the settings.
     * <p>
     * 설정의 변경을 받는다.
     */
    public interface Listener {

        /**
         * Called after one or more values changed.
         * <p>
         * 값이 하나 이상 바뀐 뒤에 호출된다.
         *
         * @param settings the settings / 설정
         */
        void settingsChanged(Settings settings);
    }

    /** The default values by key, in the order they are stored. / 키별 기본값. 저장하는 순서대로이다. */
    private static final Map<String, String> DEFAULTS = new LinkedHashMap<String, String>();

    static {
        DEFAULTS.put(HOME_PAGE, "about:blank");
        DEFAULTS.put(SEARCH_TEMPLATE, "");
        DEFAULTS.put(NEW_TAB_PAGE, NEW_TAB_BLANK);
        DEFAULTS.put(CONFIRM_CLOSE_TABS, "true");
    }

    /** The values that differ from nothing: every key that was set. / 설정한 값들. */
    private final Map<String, String> values = new LinkedHashMap<String, String>();

    /** The listeners. / 리스너. */
    private final List<Listener> listeners = new ArrayList<Listener>();

    /**
     * Creates settings with the default values.
     * <p>
     * 기본값으로 설정을 만든다.
     */
    public Settings() {
    }

    /**
     * Returns the keys that have a default value, in the order they are stored.
     * <p>
     * 기본값이 있는 키를 저장하는 순서대로 반환한다.
     *
     * @return a copy of the keys / 키의 복사본
     */
    public static List<String> keys() {
        return new ArrayList<String>(DEFAULTS.keySet());
    }

    /**
     * Returns the default value of a key.
     * <p>
     * 키의 기본값을 반환한다.
     *
     * @param key the key / 키
     * @return the default, or the empty string for an unknown key / 기본값. 모르는 키이면 빈 문자열
     */
    public static String defaultOf(String key) {
        String v = DEFAULTS.get(key);
        return v == null ? "" : v;
    }

    /**
     * Returns the value of a key, or its default when it was never set.
     * <p>
     * 키의 값을 반환한다. 설정한 적이 없으면 기본값을 반환한다.
     *
     * @param key the key / 키
     * @return the value; never null / 값. null 이 아니다
     */
    public synchronized String get(String key) {
        String v = values.get(key);
        return v == null ? defaultOf(key) : v;
    }

    /**
     * Returns the value of a key as a truth value: <code>true</code> or <code>false</code>; anything else gives the default of the key.
     * <p>
     * 키의 값을 참/거짓으로 반환한다: <code>true</code> 또는 <code>false</code>. 그 밖의 값이면 키의 기본값을 준다.
     *
     * @param key the key / 키
     * @return the truth value / 참/거짓 값
     */
    public boolean getBoolean(String key) {
        String v = get(key).trim();
        // 저장된 값을 이해할 수 없으면 기본값을 쓴다.
        if (!"true".equalsIgnoreCase(v) && !"false".equalsIgnoreCase(v)) {
            v = defaultOf(key);
        }
        return "true".equalsIgnoreCase(v);
    }

    /**
     * Sets the value of a key and tells the listeners if it changed.
     * <p>
     * 키의 값을 정하고, 바뀌었으면 리스너에 알린다.
     *
     * @param key   the key / 키
     * @param value the value; null means the default / 값. null 이면 기본값
     */
    public void set(String key, String value) {
        boolean changed;
        synchronized (this) {
            String next = value == null ? defaultOf(key) : value;
            changed = !next.equals(get(key));
            values.put(key, next);
        }
        if (changed) {
            fireChanged();
        }
    }

    /**
     * Replaces several values at once and tells the listeners once if anything changed.
     * <p>
     * 여러 값을 한 번에 바꾸고, 바뀐 것이 있으면 리스너에 한 번 알린다.
     *
     * @param updates the new values by key / 키별 새 값
     */
    public void setAll(Map<String, String> updates) {
        boolean changed = false;
        synchronized (this) {
            // 키마다 값을 바꾸면서 실제로 달라진 것이 있는지 기억한다.
            for (Map.Entry<String, String> e : updates.entrySet()) {
                String next = e.getValue() == null ? defaultOf(e.getKey()) : e.getValue();
                if (!next.equals(get(e.getKey()))) {
                    changed = true;
                }
                values.put(e.getKey(), next);
            }
        }
        if (changed) {
            fireChanged();
        }
    }

    /**
     * Returns all values, the defaults included, by key.
     * <p>
     * 기본값을 포함한 모든 값을 키별로 반환한다.
     *
     * @return a copy of the values / 값의 복사본
     */
    public synchronized Map<String, String> toMap() {
        Map<String, String> out = new LinkedHashMap<String, String>(DEFAULTS);
        out.putAll(values);
        return out;
    }

    /**
     * Adds a listener.
     * <p>
     * 리스너를 더한다.
     *
     * @param listener the listener / 리스너
     */
    public void addListener(Listener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a listener.
     * <p>
     * 리스너를 뺀다.
     *
     * @param listener the listener / 리스너
     */
    public void removeListener(Listener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    /**
     * Tells the listeners that values changed.
     * <p>
     * 값이 바뀌었음을 리스너에 알린다.
     */
    private void fireChanged() {
        List<Listener> copy;
        synchronized (listeners) {
            copy = new ArrayList<Listener>(listeners);
        }
        // 리스너마다 변경을 알린다.
        for (int i = 0; i < copy.size(); i++) {
            copy.get(i).settingsChanged(this);
        }
    }
}
