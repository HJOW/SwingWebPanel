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

    /** Key of whether the navigation toolbar is shown (<code>true</code> or <code>false</code>). / 탐색 도구 모음을 보이는지의 키(<code>true</code> 또는 <code>false</code>). */
    public static final String SHOW_TOOLBAR = "view.toolbar";

    /** Key of whether the status bar is shown (<code>true</code> or <code>false</code>). / 상태 표시줄을 보이는지의 키(<code>true</code> 또는 <code>false</code>). */
    public static final String SHOW_STATUS_BAR = "view.statusBar";

    /** Key of the class name of the Swing look and feel of the application; empty for the default, Nimbus. / 응용 프로그램의 Swing 룩앤필 클래스 이름의 키. 기본값인 Nimbus 이면 빈 값. */
    public static final String LOOK_AND_FEEL = "view.lookAndFeel";

    /** Key of the zoom of new tabs and of the "actual size" command, as a percentage. / 새 탭과 "원래 크기" 명령의 확대 비율(백분율)의 키. */
    public static final String DEFAULT_ZOOM = "zoom.default";

    /** Key of what the program opens at start: {@link #START_HOME}, {@link #START_BLANK} or {@link #START_RESTORE}. / 프로그램이 시작할 때 여는 것의 키: {@link #START_HOME}, {@link #START_BLANK}, {@link #START_RESTORE} 중 하나. */
    public static final String START_PAGE = "startup.page";

    /** Value of {@link #START_PAGE}: the home page. / {@link #START_PAGE} 의 값: 홈 페이지. */
    public static final String START_HOME = "home";

    /** Value of {@link #START_PAGE}: the empty page. / {@link #START_PAGE} 의 값: 빈 페이지. */
    public static final String START_BLANK = "blank";

    /** Value of {@link #START_PAGE}: the windows and tabs of the last run. / {@link #START_PAGE} 의 값: 지난번의 창과 탭. */
    public static final String START_RESTORE = "restore";

    /** Key of whether a link opened in a new tab becomes the active tab at once (<code>true</code> or <code>false</code>). / 새 탭에서 연 링크가 바로 활성 탭이 되는지의 키(<code>true</code> 또는 <code>false</code>). */
    public static final String SELECT_NEW_TABS = "tabs.selectNew";

    /** Key of whether backgrounds are printed (<code>true</code> or <code>false</code>). / 배경을 인쇄하는지의 키(<code>true</code> 또는 <code>false</code>). */
    public static final String PRINT_BACKGROUNDS = "print.backgrounds";

    /** Key of whether headers and footers are printed (<code>true</code> or <code>false</code>). / 머리글과 바닥글을 인쇄하는지의 키(<code>true</code> 또는 <code>false</code>). */
    public static final String PRINT_HEADERS = "print.headers";

    /** Key of the paper of the page setup, as {@link PaperFormat} writes it; empty for the default paper. / 페이지 설정의 용지의 키. {@link PaperFormat} 이 적는 형식이며, 기본 용지이면 빈 값. */
    public static final String PRINT_PAPER = "print.paper";

    /** Key of the font family for content without an author font; empty for the built-in font. / 작성자 글꼴이 없는 내용의 글꼴 패밀리의 키. 내장 글꼴이면 빈 값. */
    public static final String FONT_STANDARD = "font.standard";

    /** Key of the font family behind <code>serif</code>; empty for the built-in font. / <code>serif</code> 뒤의 글꼴 패밀리의 키. 내장 글꼴이면 빈 값. */
    public static final String FONT_SERIF = "font.serif";

    /** Key of the font family behind <code>sans-serif</code>; empty for the built-in font. / <code>sans-serif</code> 뒤의 글꼴 패밀리의 키. 내장 글꼴이면 빈 값. */
    public static final String FONT_SANS_SERIF = "font.sansSerif";

    /** Key of the font family behind <code>monospace</code>; empty for the built-in font. / <code>monospace</code> 뒤의 글꼴 패밀리의 키. 내장 글꼴이면 빈 값. */
    public static final String FONT_MONOSPACE = "font.monospace";

    /** Key of the default font size in pixels. / 기본 글꼴 크기(픽셀)의 키. */
    public static final String FONT_SIZE = "font.size";

    /** Key of the font size of monospace elements in pixels. / 고정폭 요소의 글꼴 크기(픽셀)의 키. */
    public static final String FONT_MONOSPACE_SIZE = "font.monospaceSize";

    /** Key of whether pages may run scripts (<code>true</code> or <code>false</code>). / 페이지가 스크립트를 실행해도 되는지의 키(<code>true</code> 또는 <code>false</code>). */
    public static final String JAVASCRIPT = "javascript.enabled";

    /** Key of whether a script may close a tab that the user opened (<code>true</code> or <code>false</code>). / 사용자가 연 탭을 스크립트가 닫아도 되는지의 키(<code>true</code> 또는 <code>false</code>). */
    public static final String SCRIPTS_CLOSE_WINDOWS = "scripts.closeWindows";

    /** Key of the User-Agent text; empty for the default of the component. / User-Agent 글의 키. 컴포넌트의 기본값이면 빈 값. */
    public static final String USER_AGENT = "userAgent";

    /** Key of the folder for downloads; empty for the default of the component. / 다운로드 폴더의 키. 컴포넌트의 기본값이면 빈 값. */
    public static final String DOWNLOAD_DIRECTORY = "download.directory";

    /** Key of whether popups that were not asked for by the user are blocked (<code>true</code> or <code>false</code>). / 사용자가 요청하지 않은 팝업을 차단하는지의 키(<code>true</code> 또는 <code>false</code>). */
    public static final String BLOCK_POPUPS = "popups.block";

    /** Key of where a window that a script opens is shown: {@link #PLACE_POPUP}, {@link #PLACE_WINDOW} or {@link #PLACE_TAB}. / 스크립트가 연 창을 보이는 곳의 키: {@link #PLACE_POPUP}, {@link #PLACE_WINDOW}, {@link #PLACE_TAB} 중 하나. */
    public static final String POPUP_PLACE = "popups.place";

    /** Key of where a link or form that asks for a new window is shown: {@link #PLACE_TAB} or {@link #PLACE_WINDOW}. / 새 창을 요청하는 링크나 폼을 보이는 곳의 키: {@link #PLACE_TAB} 또는 {@link #PLACE_WINDOW}. */
    public static final String LINK_PLACE = "links.place";

    /** Value of a place: a small popup window. / 장소의 값: 작은 팝업 창. */
    public static final String PLACE_POPUP = "popup";

    /** Value of a place: a new browser window. / 장소의 값: 새 브라우저 창. */
    public static final String PLACE_WINDOW = "window";

    /** Value of a place: a new tab. / 장소의 값: 새 탭. */
    public static final String PLACE_TAB = "tab";

    /** Key of the left edge of the browser window on the screen; empty for the choice of the platform. / 화면에서 브라우저 창의 왼쪽 가장자리의 키. 플랫폼이 정하면 빈 값. */
    public static final String WINDOW_X = "window.x";

    /** Key of the top edge of the browser window on the screen; empty for the choice of the platform. / 화면에서 브라우저 창의 위쪽 가장자리의 키. 플랫폼이 정하면 빈 값. */
    public static final String WINDOW_Y = "window.y";

    /** Key of the width of the browser window. / 브라우저 창의 폭의 키. */
    public static final String WINDOW_WIDTH = "window.width";

    /** Key of the height of the browser window. / 브라우저 창의 높이의 키. */
    public static final String WINDOW_HEIGHT = "window.height";

    /** Key of whether the browser window is maximized (<code>true</code> or <code>false</code>). / 브라우저 창이 최대화되어 있는지의 키(<code>true</code> 또는 <code>false</code>). */
    public static final String WINDOW_MAXIMIZED = "window.maximized";

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
        DEFAULTS.put(SHOW_TOOLBAR, "true");
        DEFAULTS.put(SHOW_STATUS_BAR, "true");
        DEFAULTS.put(LOOK_AND_FEEL, "");
        DEFAULTS.put(DEFAULT_ZOOM, "100");
        DEFAULTS.put(START_PAGE, START_HOME);
        DEFAULTS.put(SELECT_NEW_TABS, "false");
        DEFAULTS.put(PRINT_BACKGROUNDS, "false");
        DEFAULTS.put(PRINT_HEADERS, "true");
        DEFAULTS.put(PRINT_PAPER, "");
        DEFAULTS.put(FONT_STANDARD, "");
        DEFAULTS.put(FONT_SERIF, "");
        DEFAULTS.put(FONT_SANS_SERIF, "");
        DEFAULTS.put(FONT_MONOSPACE, "");
        DEFAULTS.put(FONT_SIZE, "16");
        DEFAULTS.put(FONT_MONOSPACE_SIZE, "13");
        DEFAULTS.put(JAVASCRIPT, "true");
        DEFAULTS.put(SCRIPTS_CLOSE_WINDOWS, "false");
        DEFAULTS.put(USER_AGENT, "");
        DEFAULTS.put(DOWNLOAD_DIRECTORY, "");
        DEFAULTS.put(BLOCK_POPUPS, "true");
        DEFAULTS.put(POPUP_PLACE, PLACE_POPUP);
        DEFAULTS.put(LINK_PLACE, PLACE_TAB);
        DEFAULTS.put(WINDOW_X, "");
        DEFAULTS.put(WINDOW_Y, "");
        DEFAULTS.put(WINDOW_WIDTH, "1000");
        DEFAULTS.put(WINDOW_HEIGHT, "700");
        DEFAULTS.put(WINDOW_MAXIMIZED, "false");
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
     * Returns the value of a key as a whole number inside a range; text that is not a number, or a number outside the range, gives the
     * default of the key.
     * <p>
     * 키의 값을 범위 안의 정수로 반환한다. 수가 아닌 글이나 범위 밖의 수이면 키의 기본값을 준다.
     *
     * @param key the key / 키
     * @param min the smallest value / 가장 작은 값
     * @param max the largest value / 가장 큰 값
     * @return the number / 수
     */
    public int getInt(String key, int min, int max) {
        int fallback;
        try {
            fallback = Integer.parseInt(defaultOf(key).trim());
        } catch (NumberFormatException e) {
            fallback = min;
        }
        try {
            int v = Integer.parseInt(get(key).trim());
            return v < min || v > max ? fallback : v;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /**
     * Returns the value of a key if it is one of the allowed values, otherwise the default of the key.
     * <p>
     * 키의 값이 허용된 값 가운데 하나이면 그 값을, 아니면 키의 기본값을 반환한다.
     *
     * @param key     the key / 키
     * @param allowed the allowed values / 허용된 값
     * @return one of the allowed values, or the default / 허용된 값 가운데 하나, 또는 기본값
     */
    public String getChoice(String key, String[] allowed) {
        String v = get(key).trim();
        // 허용된 값 가운데 같은 것이 있으면 그것을 쓴다.
        for (int i = 0; i < allowed.length; i++) {
            if (allowed[i].equals(v)) {
                return v;
            }
        }
        return defaultOf(key);
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
