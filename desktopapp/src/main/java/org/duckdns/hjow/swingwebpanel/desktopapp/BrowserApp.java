package org.duckdns.hjow.swingwebpanel.desktopapp;

import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import org.duckdns.hjow.swingwebpanel.CookieJar;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.BookmarkModel;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.BookmarkStore;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.Settings;
import org.duckdns.hjow.swingwebpanel.desktopapp.ui.BrowserWindow;

/**
 * The state of the whole browser program: the settings, the cookie jar that all tabs share, the open browser windows and what happens
 * when the last one is closed. One instance exists per process. Create it and call its methods on the event dispatch thread.
 * <p>
 * 브라우저 프로그램 전체의 상태: 설정, 모든 탭이 공유하는 쿠키 저장소, 열린 브라우저 창들, 마지막 창이 닫힐 때 일어나는 일. 프로세스에 인스턴스가
 * 하나 있다. 이벤트 디스패치 스레드에서 만들고 그 메서드를 부른다.
 * <p>
 * 사용 예 / Example:
 * <pre>
 * BrowserApp app = new BrowserApp(null);          // null: end the JVM when the last window closes
 * app.openWindow("https://example.com/").setVisible(true);
 * </pre>
 */
public final class BrowserApp {

    /** The name of the application, shown in window titles. / 창 제목에 보이는 응용 프로그램의 이름. */
    public static final String NAME = "SwingWebPanel Browser";

    /** The address of the empty page. / 빈 페이지의 주소. */
    public static final String BLANK = "about:blank";

    /** The system property that replaces the directory of the user data (tests and portable use). / 사용자 데이터의 디렉터리를 바꾸는 시스템 속성(시험과 휴대용 실행). */
    public static final String HOME_PROPERTY = "swingwebpanel.desktopapp.home";

    /**
     * Returns the directory of the user data (settings, bookmarks): the value of {@link #HOME_PROPERTY} if it is set, otherwise
     * <code>.swingwebpanel/desktopapp</code> in the home directory of the user. The directory is not created here.
     * <p>
     * 사용자 데이터(설정, 북마크)의 디렉터리를 반환한다: {@link #HOME_PROPERTY} 가 설정되어 있으면 그 값, 아니면 사용자 홈 디렉터리의
     * <code>.swingwebpanel/desktopapp</code>. 디렉터리를 여기서 만들지는 않는다.
     *
     * @return the directory / 디렉터리
     */
    public static File homeDirectory() {
        String forced = System.getProperty(HOME_PROPERTY);
        // 속성으로 지정했으면 그 디렉터리를 쓴다.
        if (forced != null && forced.trim().length() > 0) {
            return new File(forced.trim());
        }
        return new File(new File(System.getProperty("user.home", "."), ".swingwebpanel"), "desktopapp");
    }

    /**
     * Asks the user questions; the application shows dialogs, tests answer by themselves.
     * <p>
     * 사용자에게 질문한다. 응용 프로그램은 대화 상자를 보이고 시험은 스스로 답한다.
     */
    public interface Prompts {

        /**
         * Asks a yes-or-no question.
         * <p>
         * 예/아니요 질문을 한다.
         *
         * @param parent  the component that the question belongs to / 질문이 속한 컴포넌트
         * @param message the question / 질문
         * @return true if the user agreed / 사용자가 동의했으면 true
         */
        boolean confirm(Component parent, String message);

        /**
         * Asks for a file to open or to save into.
         * <p>
         * 열 파일이나 저장할 파일을 묻는다.
         *
         * @param parent        the component that the question belongs to / 질문이 속한 컴포넌트
         * @param save          true to choose a file to write, false to choose an existing file to open / 쓸 파일을 고르면 true, 열 기존 파일을
         *                      고르면 false
         * @param suggestedName the file name that is filled in for saving, or null / 저장할 때 미리 채우는 파일 이름. 없으면 null
         * @return the chosen file, or null if the user cancelled / 고른 파일. 사용자가 취소했으면 null
         */
        File chooseFile(Component parent, boolean save, String suggestedName);

        /**
         * Tells the user something.
         * <p>
         * 사용자에게 무언가를 알린다.
         *
         * @param parent  the component that the message belongs to / 메시지가 속한 컴포넌트
         * @param message the message / 메시지
         */
        void inform(Component parent, String message);

        /**
         * Asks for a line of text.
         * <p>
         * 글 한 줄을 묻는다.
         *
         * @param parent  the component that the question belongs to / 질문이 속한 컴포넌트
         * @param message the question / 질문
         * @param initial the text that is filled in, or null / 미리 채우는 글. 없으면 null
         * @return the text, or null if the user cancelled / 글. 사용자가 취소했으면 null
         */
        String ask(Component parent, String message, String initial);
    }

    /** The settings shared by all windows. / 모든 창이 공유하는 설정. */
    private final Settings settings = new Settings();

    /** The cookies shared by all tabs and windows, so that a login in one tab holds in the others. / 모든 탭과 창이 공유하는 쿠키. 한 탭의 로그인이 다른 탭에서도 유효하다. */
    private final CookieJar cookieJar = new CookieJar();

    /** The bookmarks shared by all windows. / 모든 창이 공유하는 북마크. */
    private final BookmarkModel bookmarks = new BookmarkModel();

    /** The file of the bookmarks. / 북마크의 파일. */
    private final BookmarkStore bookmarkStore;

    /** What went wrong when the user data was read or written last, or null. / 사용자 데이터를 마지막으로 읽거나 쓸 때 잘못된 것. 없으면 null. */
    private String storageProblem;

    /** The open windows in the order they were opened. / 열린 순서대로의 창. */
    private final List<BrowserWindow> windows = new ArrayList<BrowserWindow>();

    /** Runs when the last window was closed. / 마지막 창이 닫혔을 때 실행된다. */
    private final Runnable afterLastWindow;

    /** Asks the user questions. / 사용자에게 질문한다. */
    private Prompts prompts = new Prompts() {
        /**
         * Shows a confirmation dialog.
         * <p>
         * 확인 대화 상자를 보인다.
         *
         * @param parent  the component that the question belongs to / 질문이 속한 컴포넌트
         * @param message the question / 질문
         * @return true if the user chose yes / 사용자가 예를 골랐으면 true
         */
        public boolean confirm(Component parent, String message) {
            return JOptionPane.showConfirmDialog(parent, message, NAME, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
        }

        /**
         * Shows a file chooser that starts in the directory used last.
         * <p>
         * 마지막에 쓴 디렉터리에서 시작하는 파일 선택기를 보인다.
         *
         * @param parent        the component that the question belongs to / 질문이 속한 컴포넌트
         * @param save          true to choose a file to write / 쓸 파일을 고르면 true
         * @param suggestedName the file name that is filled in for saving, or null / 저장할 때 미리 채우는 파일 이름. 없으면 null
         * @return the chosen file, or null if the user cancelled / 고른 파일. 사용자가 취소했으면 null
         */
        public File chooseFile(Component parent, boolean save, String suggestedName) {
            JFileChooser chooser = new JFileChooser(lastDirectory);
            if (save && suggestedName != null) {
                chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), suggestedName));
            }
            int answer = save ? chooser.showSaveDialog(parent) : chooser.showOpenDialog(parent);
            if (answer != JFileChooser.APPROVE_OPTION || chooser.getSelectedFile() == null) {
                return null;
            }
            File chosen = chooser.getSelectedFile();
            lastDirectory = chosen.getParentFile();
            // 이미 있는 파일에 저장하려면 덮어써도 되는지 먼저 묻는다.
            if (save && chosen.exists() && !confirm(parent, Texts.of("Replace the existing file?\n", "이미 있는 파일을 바꿀까요?\n") + chosen.getPath())) {
                return null;
            }
            return chosen;
        }

        /**
         * Shows a message dialog.
         * <p>
         * 메시지 대화 상자를 보인다.
         *
         * @param parent  the component that the message belongs to / 메시지가 속한 컴포넌트
         * @param message the message / 메시지
         */
        public void inform(Component parent, String message) {
            JOptionPane.showMessageDialog(parent, message, NAME, JOptionPane.INFORMATION_MESSAGE);
        }

        /**
         * Shows an input dialog.
         * <p>
         * 입력 대화 상자를 보인다.
         *
         * @param parent  the component that the question belongs to / 질문이 속한 컴포넌트
         * @param message the question / 질문
         * @param initial the text that is filled in, or null / 미리 채우는 글. 없으면 null
         * @return the text, or null if the user cancelled / 글. 사용자가 취소했으면 null
         */
        public String ask(Component parent, String message, String initial) {
            Object answer = JOptionPane.showInputDialog(parent, message, NAME, JOptionPane.QUESTION_MESSAGE, null, null, initial == null ? "" : initial);
            return answer == null ? null : answer.toString();
        }
    };

    /** The directory that the file chooser used last, or null for the default of the platform. / 파일 선택기가 마지막에 쓴 디렉터리. 플랫폼의 기본값이면 null. */
    private File lastDirectory;

    /**
     * Creates the application state.
     * <p>
     * 응용 프로그램의 상태를 만든다.
     *
     * @param afterLastWindow what to do when the last window was closed, or null to end the program with <code>System.exit(0)</code>
     *                        (tests pass their own action) / 마지막 창이 닫혔을 때 할 일. null 이면 <code>System.exit(0)</code> 으로 프로그램을
     *                        끝낸다(시험은 자기 동작을 넘긴다)
     */
    public BrowserApp(Runnable afterLastWindow) {
        this.afterLastWindow = afterLastWindow != null ? afterLastWindow : new Runnable() {
            /** Ends the program. / 프로그램을 끝낸다. */
            public void run() {
                System.exit(0);
            }
        };
        File home = homeDirectory();
        bookmarkStore = new BookmarkStore(home);
        // 읽을 수 없는 파일은 저장소가 다른 이름으로 보존한다. 그 사실을 사용자에게 알릴 수 있게 기억한다.
        if (!bookmarkStore.load(bookmarks)) {
            storageProblem = Texts.of("The bookmark file could not be read and was kept as ", "북마크 파일을 읽을 수 없어 다음 이름으로 보존했습니다: ")
                    + BookmarkStore.FILE_NAME + ".broken";
        }
        bookmarks.addListener(new BookmarkModel.Listener() {
            /**
             * Saves the bookmarks after every change.
             * <p>
             * 변경이 있을 때마다 북마크를 저장한다.
             *
             * @param model the bookmarks / 북마크
             */
            public void bookmarksChanged(BookmarkModel model) {
                try {
                    bookmarkStore.save(model);
                } catch (java.io.IOException e) {
                    storageProblem = Texts.of("The bookmarks could not be saved: ", "북마크를 저장할 수 없습니다: ") + e.getMessage();
                }
            }
        });
    }

    /**
     * Returns the bookmarks of the application.
     * <p>
     * 응용 프로그램의 북마크를 반환한다.
     *
     * @return the bookmarks shared by all windows / 모든 창이 공유하는 북마크
     */
    public BookmarkModel getBookmarks() {
        return bookmarks;
    }

    /**
     * Returns what went wrong when the user data was read or written last, and forgets it.
     * <p>
     * 사용자 데이터를 마지막으로 읽거나 쓸 때 잘못된 것을 반환하고 잊는다.
     *
     * @return the message, or null if nothing went wrong since the last call / 메시지. 지난 호출 뒤로 잘못된 것이 없으면 null
     */
    public String takeStorageProblem() {
        String p = storageProblem;
        storageProblem = null;
        return p;
    }

    /**
     * Returns the settings of the application.
     * <p>
     * 응용 프로그램의 설정을 반환한다.
     *
     * @return the settings shared by all windows / 모든 창이 공유하는 설정
     */
    public Settings getSettings() {
        return settings;
    }

    /**
     * Returns the cookie jar that every tab of the application uses.
     * <p>
     * 응용 프로그램의 모든 탭이 쓰는 쿠키 저장소를 반환한다.
     *
     * @return the shared jar / 공유 저장소
     */
    public CookieJar getCookieJar() {
        return cookieJar;
    }

    /**
     * Returns what asks the user questions.
     * <p>
     * 사용자에게 질문하는 것을 반환한다.
     *
     * @return the prompts / 질문 수단
     */
    public Prompts getPrompts() {
        return prompts;
    }

    /**
     * Replaces what asks the user questions (tests answer without a dialog).
     * <p>
     * 사용자에게 질문하는 것을 바꾼다(시험은 대화 상자 없이 답한다).
     *
     * @param prompts the new prompts / 새 질문 수단
     */
    public void setPrompts(Prompts prompts) {
        if (prompts == null) {
            throw new IllegalArgumentException("prompts must not be null");
        }
        this.prompts = prompts;
    }

    /**
     * Opens a new browser window with one tab that shows an address. The window is created but not shown; the caller makes it visible.
     * <p>
     * 주소를 보여 주는 탭 하나가 있는 새 브라우저 창을 연다. 창은 만들기만 하고 보이게 하지는 않는다. 보이게 하는 것은 호출자이다.
     *
     * @param url the address to show, or null for the empty page / 보여 줄 주소. 빈 페이지이면 null
     * @return the new window / 새 창
     */
    public BrowserWindow openWindow(String url) {
        BrowserWindow window = new BrowserWindow(this);
        windows.add(window);
        // 빈 페이지는 컴포넌트의 처음 상태이므로 다시 읽지 않는다.
        if (url != null && !BLANK.equals(url)) {
            window.getActiveTab().open(url);
        }
        return window;
    }

    /**
     * Returns the open windows.
     * <p>
     * 열린 창들을 반환한다.
     *
     * @return a copy of the list / 목록의 복사본
     */
    public List<BrowserWindow> getWindows() {
        return new ArrayList<BrowserWindow>(windows);
    }

    /**
     * Closes all windows as the user asks for it, the newest first; each window may ask before it closes, and a refusal stops the exit.
     * Closing the last window ends the program.
     * <p>
     * 사용자가 요청한 대로 모든 창을 최근 것부터 닫는다. 창마다 닫기 전에 물을 수 있고, 거절하면 종료를 그만둔다. 마지막 창이 닫히면 프로그램이
     * 끝난다.
     *
     * @return true if every window was closed / 모든 창을 닫았으면 true
     */
    public boolean exit() {
        List<BrowserWindow> open = getWindows();
        // 최근 창부터 닫고, 한 창이라도 거절하면 멈춘다.
        for (int i = open.size() - 1; i >= 0; i--) {
            if (!open.get(i).requestClose()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the text of the about dialog: the names and versions of the application, of the component and of Java. The versions come
     * from the manifest of the JAR (<code>Implementation-Version</code>); classes that were not loaded from a JAR have none.
     * <p>
     * 정보 대화 상자의 글을 반환한다: 응용 프로그램, 컴포넌트, Java 의 이름과 판. 판은 JAR 의 매니페스트(<code>Implementation-Version</code>)에서
     * 온다. JAR 에서 읽지 않은 클래스에는 판이 없다.
     *
     * @return the text / 글
     */
    public static String aboutText() {
        String unknown = Texts.of("(development build)", "(개발 빌드)");
        Package appPackage = BrowserApp.class.getPackage();
        Package panelPackage = org.duckdns.hjow.swingwebpanel.SwingWebPanel.class.getPackage();
        String appVersion = appPackage == null ? null : appPackage.getImplementationVersion();
        String panelVersion = panelPackage == null ? null : panelPackage.getImplementationVersion();
        return NAME + " " + (appVersion == null ? unknown : appVersion) + "\n"
                + "SwingWebPanel " + (panelVersion == null ? unknown : panelVersion) + "\n"
                + "Java " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")\n"
                + System.getProperty("os.name") + " " + System.getProperty("os.version");
    }

    /**
     * Forgets a window that was closed; called by the window itself. When it was the last one, the action for the end of the program runs.
     * <p>
     * 닫힌 창을 목록에서 뺀다. 창 자신이 부른다. 마지막 창이었으면 프로그램 종료 동작이 실행된다.
     *
     * @param window the closed window / 닫힌 창
     */
    public void windowClosed(BrowserWindow window) {
        // 목록에 있던 창이 마지막 창이었을 때만 종료 동작을 실행한다.
        if (windows.remove(window) && windows.isEmpty()) {
            afterLastWindow.run();
        }
    }
}
