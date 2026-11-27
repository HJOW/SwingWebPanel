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
import org.duckdns.hjow.swingwebpanel.desktopapp.model.SessionStore;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.Settings;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.SettingsStore;
import org.duckdns.hjow.swingwebpanel.desktopapp.ui.BrowserWindow;
import org.duckdns.hjow.swingwebpanel.desktopapp.ui.LookAndFeels;

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
         * Asks for a directory.
         * <p>
         * 디렉터리를 묻는다.
         *
         * @param parent  the component that the question belongs to / 질문이 속한 컴포넌트
         * @param current the directory to start in, or null / 시작할 디렉터리. 없으면 null
         * @return the chosen directory, or null if the user cancelled / 고른 디렉터리. 사용자가 취소했으면 null
         */
        File chooseDirectory(Component parent, File current);

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

    /** The file of the settings. / 설정의 파일. */
    private final SettingsStore settingsStore;

    /** The file of the last session. / 지난 세션의 파일. */
    private final SessionStore sessionStore;

    /** What went wrong when the user data was read or written last, or null. / 사용자 데이터를 마지막으로 읽거나 쓸 때 잘못된 것. 없으면 null. */
    private String storageProblem;

    /** The look and feel setting that was applied last, or null before the first time. / 마지막으로 적용한 룩앤필 설정. 처음 적용하기 전이면 null. */
    private String requestedLookAndFeel;

    /** Why the look and feel of the settings could not be used, or null. / 설정의 룩앤필을 쓸 수 없었던 까닭. 없으면 null. */
    private String lookAndFeelProblem;

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
         * Shows a file chooser that only lets the user pick a directory.
         * <p>
         * 디렉터리만 고를 수 있는 파일 선택기를 보인다.
         *
         * @param parent  the component that the question belongs to / 질문이 속한 컴포넌트
         * @param current the directory to start in, or null / 시작할 디렉터리. 없으면 null
         * @return the chosen directory, or null if the user cancelled / 고른 디렉터리. 사용자가 취소했으면 null
         */
        public File chooseDirectory(Component parent, File current) {
            JFileChooser chooser = new JFileChooser(current != null ? current : lastDirectory);
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
                return null;
            }
            return chooser.getSelectedFile();
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
        settingsStore = new SettingsStore(home);
        sessionStore = new SessionStore(home);
        // 읽을 수 없는 설정 파일은 저장소가 다른 이름으로 보존하고, 응용 프로그램은 기본값으로 시작한다.
        if (!settingsStore.load(settings)) {
            storageProblem = Texts.of("The settings file could not be read and was kept as ", "설정 파일을 읽을 수 없어 다음 이름으로 보존했습니다: ")
                    + SettingsStore.FILE_NAME + ".broken";
        }
        // 창을 만들기 전에 룩앤필을 정해야 첫 창의 컴포넌트부터 그것으로 만들어진다.
        applyLookAndFeel();
        settings.addListener(new Settings.Listener() {
            /**
             * Applies a new look and feel and saves the settings after every change.
             * <p>
             * 변경이 있을 때마다 새 룩앤필을 적용하고 설정을 저장한다.
             *
             * @param changed the settings / 설정
             */
            public void settingsChanged(Settings changed) {
                applyLookAndFeel();
                try {
                    settingsStore.save(changed);
                } catch (java.io.IOException e) {
                    storageProblem = Texts.of("The settings could not be saved: ", "설정을 저장할 수 없습니다: ") + e.getMessage();
                }
            }
        });
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
     * Returns why the look and feel of the settings could not be used, and forgets it.
     * <p>
     * 설정의 룩앤필을 쓸 수 없었던 까닭을 반환하고 잊는다.
     *
     * @return the message, or null if no look and feel was refused since the last call / 메시지. 지난 호출 뒤로 거부된 룩앤필이 없으면 null
     */
    public String takeLookAndFeelProblem() {
        String p = lookAndFeelProblem;
        lookAndFeelProblem = null;
        return p;
    }

    /**
     * Switches to the look and feel of the settings when that setting changed (and at the start). A look and feel that cannot be used
     * leaves the one in use; the setting is kept and the problem is remembered for the user.
     * <p>
     * 룩앤필 설정이 바뀌었을 때(그리고 시작할 때) 그 룩앤필로 바꾼다. 쓸 수 없는 룩앤필이면 쓰던 것을 그대로 두고, 설정은 보존하며 문제를 사용자에게
     * 알릴 수 있게 기억한다.
     */
    private void applyLookAndFeel() {
        String wanted = settings.get(Settings.LOOK_AND_FEEL).trim();
        // 이 설정이 그대로이면 다른 설정의 변경 때마다 다시 시도하지 않는다.
        if (wanted.equals(requestedLookAndFeel)) {
            return;
        }
        requestedLookAndFeel = wanted;
        lookAndFeelProblem = LookAndFeels.apply(wanted) ? null
                : Texts.of("This look and feel cannot be used: ", "이 룩앤필을 쓸 수 없습니다: ") + wanted;
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
     * What the application uses to reach the print system (plan item P31-10): the page setup dialog and the print job. Tests replace
     * it, so that no test ever sends anything to a printer.
     * <p>
     * 응용 프로그램이 인쇄 시스템에 닿는 수단(계획 P31-10): 페이지 설정 대화 상자와 인쇄 작업. 시험은 이것을 바꿔, 어떤 시험도 프린터로 아무것도
     * 보내지 않게 한다.
     */
    public interface Printing {

        /**
         * Lets the user change the paper.
         * <p>
         * 사용자가 용지를 바꾸게 한다.
         *
         * @param parent  the window that asks / 묻는 창
         * @param current the paper now / 지금의 용지
         * @return the chosen paper, or null if the user cancelled / 고른 용지. 사용자가 취소했으면 null
         */
        java.awt.print.PageFormat pageSetup(Component parent, java.awt.print.PageFormat current);

        /**
         * Lets the user choose a printer and prints a document.
         * <p>
         * 사용자가 프린터를 고르게 하고 문서를 인쇄한다.
         *
         * @param parent   the window that asks / 묻는 창
         * @param document the pages to print / 인쇄할 쪽들
         * @param jobName  the name of the print job / 인쇄 작업의 이름
         * @return true if printing started / 인쇄를 시작했으면 true
         */
        boolean print(Component parent, java.awt.print.Pageable document, String jobName);
    }

    /** The print system: the dialogs and the jobs of <code>java.awt.print.PrinterJob</code>. / 인쇄 시스템: <code>java.awt.print.PrinterJob</code> 의 대화 상자와 작업. */
    private Printing printing = new Printing() {
        /**
         * Shows the page setup dialog of the platform.
         * <p>
         * 플랫폼의 페이지 설정 대화 상자를 보인다.
         *
         * @param parent  the window that asks / 묻는 창
         * @param current the paper now / 지금의 용지
         * @return the chosen paper, or null / 고른 용지 또는 null
         */
        public java.awt.print.PageFormat pageSetup(Component parent, java.awt.print.PageFormat current) {
            try {
                java.awt.print.PageFormat chosen = java.awt.print.PrinterJob.getPrinterJob().pageDialog(current);
                // 취소하면 넘긴 객체가 그대로 돌아온다.
                return chosen == current ? null : chosen;
            } catch (RuntimeException e) {
                prompts.inform(parent, Texts.of("The page setup cannot be shown: ", "페이지 설정을 보일 수 없습니다: ") + e);
                return null;
            }
        }

        /**
         * Shows the print dialog of the platform and prints on a thread of its own.
         * <p>
         * 플랫폼의 인쇄 대화 상자를 보이고 별도 스레드에서 인쇄한다.
         *
         * @param parent   the window that asks / 묻는 창
         * @param document the pages to print / 인쇄할 쪽들
         * @param jobName  the name of the print job / 인쇄 작업의 이름
         * @return true if printing started / 인쇄를 시작했으면 true
         */
        public boolean print(final Component parent, java.awt.print.Pageable document, String jobName) {
            final java.awt.print.PrinterJob job;
            try {
                job = java.awt.print.PrinterJob.getPrinterJob();
                job.setJobName(jobName == null || jobName.length() == 0 ? NAME : jobName);
                job.setPageable(document);
                if (!job.printDialog()) {
                    return false;
                }
            } catch (RuntimeException e) {
                prompts.inform(parent, Texts.of("Printing is not possible: ", "인쇄할 수 없습니다: ") + e);
                return false;
            }
            Thread worker = new Thread(new Runnable() {
                /** Sends the pages to the printer. / 쪽을 프린터로 보낸다. */
                public void run() {
                    try {
                        job.print();
                    } catch (final java.awt.print.PrinterException e) {
                        javax.swing.SwingUtilities.invokeLater(new Runnable() {
                            /** Tells the user what went wrong. / 무엇이 잘못됐는지 사용자에게 알린다. */
                            public void run() {
                                prompts.inform(parent, Texts.of("Printing failed: ", "인쇄에 실패했습니다: ") + e.getMessage());
                            }
                        });
                    }
                }
            }, "desktopapp-print");
            worker.start();
            return true;
        }
    };

    /**
     * Returns what reaches the print system.
     * <p>
     * 인쇄 시스템에 닿는 수단을 반환한다.
     *
     * @return the print system / 인쇄 시스템
     */
    public Printing getPrinting() {
        return printing;
    }

    /**
     * Replaces what reaches the print system; tests use it so that nothing is sent to a printer.
     * <p>
     * 인쇄 시스템에 닿는 수단을 바꾼다. 시험이 프린터로 아무것도 보내지 않으려고 쓴다.
     *
     * @param printing the new print system / 새 인쇄 시스템
     */
    public void setPrinting(Printing printing) {
        if (printing == null) {
            throw new IllegalArgumentException("printing must not be null");
        }
        this.printing = printing;
    }

    /**
     * Returns the paper of the page setup (plan item P31-10): what the settings keep, or the default paper.
     * <p>
     * 페이지 설정의 용지를 반환한다(계획 P31-10): 설정이 보관한 것, 없으면 기본 용지.
     *
     * @return a new format / 새 형식
     */
    public java.awt.print.PageFormat getPaper() {
        return org.duckdns.hjow.swingwebpanel.desktopapp.model.PaperFormat.parse(settings.get(Settings.PRINT_PAPER), null);
    }

    /**
     * Keeps the paper of the page setup in the settings.
     * <p>
     * 페이지 설정의 용지를 설정에 보관한다.
     *
     * @param paper the paper / 용지
     */
    public void setPaper(java.awt.print.PageFormat paper) {
        settings.set(Settings.PRINT_PAPER, org.duckdns.hjow.swingwebpanel.desktopapp.model.PaperFormat.format(paper));
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
     * Removes the private data of the application (plan item P31-09): every tab and popup of every window shows the empty page and
     * loses its history, cache and session storage, and the cookies and the local storage that all of them share are removed.
     * Bookmarks and settings stay.
     * <p>
     * 응용 프로그램의 개인 정보를 지운다(계획 P31-09): 모든 창의 모든 탭과 팝업이 빈 페이지를 보이고 이력, 캐시, sessionStorage 를 잃으며,
     * 그것들이 함께 쓰는 쿠키와 localStorage 가 지워진다. 북마크와 설정은 남는다.
     */
    public void clearPrivateData() {
        List<BrowserWindow> open = getWindows();
        // 창마다 그 페이지들을 비운다(컴포넌트의 clean() 이 공유 localStorage 도 비운다).
        for (int i = 0; i < open.size(); i++) {
            open.get(i).clearPages();
        }
        cookieJar.clear();
        // 지난 세션의 탭 목록도 방문의 흔적이므로 비운다.
        try {
            sessionStore.save(new ArrayList<SessionStore.Window>());
        } catch (java.io.IOException e) {
            storageProblem = Texts.of("The session could not be saved: ", "세션을 저장할 수 없습니다: ") + e.getMessage();
        }
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
     * Opens a new browser window whose one tab shows the component of a window that a page opened. The window is created but not
     * shown; the caller makes it visible.
     * <p>
     * 페이지가 연 창의 컴포넌트를 탭 하나로 보이는 새 브라우저 창을 연다. 창은 만들기만 하고 보이게 하지는 않는다. 보이게 하는 것은 호출자이다.
     *
     * @param panel the component of the window that a page opened, created by a tab of this application / 페이지가 연 창의 컴포넌트.
     *              이 응용 프로그램의 탭이 만든 것
     * @return the new window / 새 창
     */
    public BrowserWindow adoptWindow(org.duckdns.hjow.swingwebpanel.SwingWebPanel panel) {
        BrowserWindow window = new BrowserWindow(this, panel);
        windows.add(window);
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
     * Opens the windows that the program starts with. An address given by the caller (the command line) is opened in one window.
     * Otherwise the setting for the start decides: the home page, the empty page, or the windows and tabs of the last run (the home
     * page if there is nothing to restore). The windows are created but not shown.
     * <p>
     * 프로그램이 시작할 때의 창을 연다. 호출자(명령행)가 준 주소가 있으면 창 하나에 그 주소를 연다. 없으면 시작에 관한 설정이 정한다: 홈 페이지,
     * 빈 페이지, 또는 지난번의 창과 탭(복원할 것이 없으면 홈 페이지). 창은 만들기만 하고 보이지는 않는다.
     *
     * @param url the address from the command line, or null / 명령행의 주소. 없으면 null
     * @return the windows, at least one / 창들. 하나 이상이다
     */
    public List<BrowserWindow> start(String url) {
        List<BrowserWindow> opened = new ArrayList<BrowserWindow>();
        String[] choices = { Settings.START_HOME, Settings.START_BLANK, Settings.START_RESTORE };
        String mode = settings.getChoice(Settings.START_PAGE, choices);
        if (url != null && url.trim().length() > 0) {
            opened.add(openWindow(url));
            return opened;
        }
        if (Settings.START_RESTORE.equals(mode)) {
            List<SessionStore.Window> last = sessionStore.load();
            // 지난 세션의 창마다 창을 만들고 그 탭들을 연다.
            for (int w = 0; w < last.size(); w++) {
                List<String> urls = last.get(w).getUrls();
                BrowserWindow window = openWindow(urls.get(0));
                // 첫 탭 뒤의 탭들을 차례로 연다.
                for (int t = 1; t < urls.size(); t++) {
                    window.newTab(urls.get(t), false);
                }
                window.selectTab(last.get(w).getActive());
                opened.add(window);
            }
        }
        if (opened.isEmpty()) {
            opened.add(openWindow(Settings.START_BLANK.equals(mode) ? null : settings.get(Settings.HOME_PAGE)));
        }
        return opened;
    }

    /**
     * Remembers the open windows and their tabs as the last session, for the setting that restores them at the next start. It is
     * called before the windows are closed at the end of the program.
     * <p>
     * 열린 창과 그 탭을 지난 세션으로 기억한다. 다음에 시작할 때 복원하는 설정이 쓴다. 프로그램이 끝날 때 창을 닫기 전에 호출된다.
     */
    public void saveSession() {
        List<SessionStore.Window> session = new ArrayList<SessionStore.Window>();
        // 창마다 빈 페이지가 아닌 탭의 주소를 모은다.
        for (int w = 0; w < windows.size(); w++) {
            BrowserWindow window = windows.get(w);
            List<String> urls = window.getTabUrls();
            if (!urls.isEmpty()) {
                session.add(new SessionStore.Window(urls, window.getActiveTabPosition()));
            }
        }
        try {
            sessionStore.save(session);
        } catch (java.io.IOException e) {
            storageProblem = Texts.of("The session could not be saved: ", "세션을 저장할 수 없습니다: ") + e.getMessage();
        }
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
        saveSession();
        // 창을 하나씩 닫는 동안에는 세션을 다시 쓰지 않는다(모든 창이 열려 있던 지금의 것이 세션이다).
        exiting = true;
        try {
            // 최근 창부터 닫고, 한 창이라도 거절하면 멈춘다.
            for (int i = open.size() - 1; i >= 0; i--) {
                if (!open.get(i).requestClose()) {
                    return false;
                }
            }
            return true;
        } finally {
            exiting = false;
        }
    }

    /** True while {@link #exit()} closes the windows. / {@link #exit()} 가 창을 닫는 동안 true. */
    private boolean exiting;

    /**
     * Called by the last open window before it closes as the user asked: its tabs become the last session, unless the program is
     * already ending through {@link #exit()}, which remembered all windows.
     * <p>
     * 마지막으로 열린 창이 사용자의 요청으로 닫히기 전에 부른다: 그 창의 탭이 지난 세션이 된다. 단, 모든 창을 기억해 둔 {@link #exit()} 로
     * 프로그램이 이미 끝나는 중이면 그대로 둔다.
     */
    public void lastWindowClosing() {
        if (!exiting) {
            saveSession();
        }
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
