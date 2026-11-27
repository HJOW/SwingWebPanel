package org.duckdns.hjow.swingwebpanel.desktopapp.ui;

/**
 * The names of the commands of a browser window (plan item P31-05). Menu items, key bindings and tests all run a command through
 * {@link BrowserWindow#perform(String)} with one of these names; a menu item also carries the name as its component name, so that a test
 * can find it.
 * <p>
 * 브라우저 창의 명령 이름(계획 P31-05). 메뉴 항목, 키 바인딩, 시험이 모두 이 이름 가운데 하나로 {@link BrowserWindow#perform(String)} 을 불러
 * 명령을 실행한다. 메뉴 항목은 이 이름을 컴포넌트 이름으로도 가지므로 시험이 항목을 찾을 수 있다.
 * <p>
 * 사용 예 / Example:
 * <pre>
 * window.perform(Commands.NEW_TAB);
 * </pre>
 */
public final class Commands {

    /** Opens a new tab. / 새 탭을 연다. */
    public static final String NEW_TAB = "new-tab";

    /** Opens a new window. / 새 창을 연다. */
    public static final String NEW_WINDOW = "new-window";

    /** Moves the keyboard focus to the address bar. / 키보드 포커스를 주소 표시줄로 옮긴다. */
    public static final String OPEN_LOCATION = "open-location";

    /** Asks for a file and opens it in the active tab. / 파일을 물어 활성 탭에서 연다. */
    public static final String OPEN_FILE = "open-file";

    /** Saves the received text of the page into a file. / 페이지의 수신 원문을 파일에 저장한다. */
    public static final String SAVE_PAGE = "save-page";

    /** Shows the page setup dialog of printing. / 인쇄의 페이지 설정 대화 상자를 보인다. */
    public static final String PAGE_SETUP = "page-setup";

    /** Shows the print preview. / 인쇄 미리 보기를 보인다. */
    public static final String PRINT_PREVIEW = "print-preview";

    /** Prints the page. / 페이지를 인쇄한다. */
    public static final String PRINT = "print";

    /** Closes the active tab. / 활성 탭을 닫는다. */
    public static final String CLOSE_TAB = "close-tab";

    /** Closes the window. / 창을 닫는다. */
    public static final String CLOSE_WINDOW = "close-window";

    /** Closes all windows and ends the program. / 모든 창을 닫고 프로그램을 끝낸다. */
    public static final String EXIT = "exit";

    /** Cuts the selection. / 선택한 것을 잘라낸다. */
    public static final String CUT = "cut";

    /** Copies the selection. / 선택한 것을 복사한다. */
    public static final String COPY = "copy";

    /** Pastes the clipboard. / 클립보드를 붙여넣는다. */
    public static final String PASTE = "paste";

    /** Selects everything. / 모두 선택한다. */
    public static final String SELECT_ALL = "select-all";

    /** Shows the find bar. / 찾기 막대를 보인다. */
    public static final String FIND = "find";

    /** Moves to the next match of the find bar. / 찾기 막대의 다음 일치로 간다. */
    public static final String FIND_NEXT = "find-next";

    /** Moves to the previous match of the find bar. / 찾기 막대의 이전 일치로 간다. */
    public static final String FIND_PREVIOUS = "find-previous";

    /** Shows or hides the navigation toolbar. / 탐색 도구 모음을 보이거나 숨긴다. */
    public static final String TOGGLE_TOOLBAR = "toggle-toolbar";

    /** Shows or hides the status bar. / 상태 표시줄을 보이거나 숨긴다. */
    public static final String TOGGLE_STATUS_BAR = "toggle-status-bar";

    /** Zooms the page in by one step. / 페이지를 한 단계 확대한다. */
    public static final String ZOOM_IN = "zoom-in";

    /** Zooms the page out by one step. / 페이지를 한 단계 축소한다. */
    public static final String ZOOM_OUT = "zoom-out";

    /** Shows the page at the default zoom of the settings. / 페이지를 설정의 기본 확대 비율로 보인다. */
    public static final String ZOOM_RESET = "zoom-reset";

    /** Shows the received text of the page. / 페이지의 수신 원문을 보인다. */
    public static final String VIEW_SOURCE = "view-source";

    /** Shows the console window. / 콘솔 창을 보인다. */
    public static final String CONSOLE = "console";

    /** Goes back one history entry. / 이력에서 한 칸 뒤로 간다. */
    public static final String BACK = "back";

    /** Goes forward one history entry. / 이력에서 한 칸 앞으로 간다. */
    public static final String FORWARD = "forward";

    /** Opens the home page. / 홈 페이지를 연다. */
    public static final String HOME = "home";

    /** Reloads the page. / 페이지를 다시 읽는다. */
    public static final String RELOAD = "reload";

    /** Reloads the page without the cache. / 캐시 없이 페이지를 다시 읽는다. */
    public static final String RELOAD_HARD = "reload-hard";

    /** Stops loading. / 읽기를 멈춘다. */
    public static final String STOP = "stop";

    /** Shows the history of the active tab. / 활성 탭의 방문 기록을 보인다. */
    public static final String HISTORY = "history";

    /** Selects the next tab. / 다음 탭을 고른다. */
    public static final String NEXT_TAB = "next-tab";

    /** Selects the previous tab. / 이전 탭을 고른다. */
    public static final String PREVIOUS_TAB = "previous-tab";

    /** Opens the tab that was closed last again. / 마지막에 닫은 탭을 다시 연다. */
    public static final String REOPEN_TAB = "reopen-tab";

    /** The beginning of the commands that select a tab by its number: <code>select-tab-1</code> to <code>select-tab-8</code>. / 번호로 탭을 고르는 명령의 앞부분: <code>select-tab-1</code> ~ <code>select-tab-8</code>. */
    public static final String SELECT_TAB = "select-tab-";

    /** Selects the last tab. / 마지막 탭을 고른다. */
    public static final String LAST_TAB = "last-tab";

    /** Adds the page of the active tab to the bookmarks. / 활성 탭의 페이지를 북마크에 더한다. */
    public static final String ADD_BOOKMARK = "add-bookmark";

    /** Adds the pages of all tabs to a new bookmark folder. / 모든 탭의 페이지를 새 북마크 폴더에 더한다. */
    public static final String BOOKMARK_ALL_TABS = "bookmark-all-tabs";

    /** Shows the bookmark manager. / 북마크 관리 창을 보인다. */
    public static final String MANAGE_BOOKMARKS = "manage-bookmarks";

    /** Shows the settings dialog. / 설정 대화 상자를 보인다. */
    public static final String SETTINGS = "settings";

    /** Deletes the private data (cache, storage, cookies). / 개인 정보(캐시, 저장소, 쿠키)를 지운다. */
    public static final String CLEAR_PRIVATE_DATA = "clear-private-data";

    /** Shows the versions of the application, the component and Java. / 응용 프로그램, 컴포넌트, Java 의 판을 보인다. */
    public static final String ABOUT = "about";

    /**
     * Not instantiable.
     * <p>
     * 인스턴스를 만들지 않는다.
     */
    private Commands() {
    }
}
