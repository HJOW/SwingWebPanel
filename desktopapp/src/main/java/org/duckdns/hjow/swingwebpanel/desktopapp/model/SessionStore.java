package org.duckdns.hjow.swingwebpanel.desktopapp.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The file of the last session (plan item P31-07): <code>session.txt</code> in the directory of the user data holds the addresses of
 * the tabs of the windows that were open when the program ended, for the setting that restores them at the next start. The first line
 * names the format; then each window is a line <code>W</code> with the position of its active tab, followed by one line
 * <code>T</code> and an address per tab (fields separated by a tab character). Only addresses are kept: the history and the content of
 * forms are not restored. A missing or broken file means that there is nothing to restore.
 * <p>
 * 지난 세션의 파일(계획 P31-07): 사용자 데이터 디렉터리의 <code>session.txt</code> 는 프로그램이 끝날 때 열려 있던 창의 탭 주소를 담는다. 다음에
 * 시작할 때 그것을 복원하는 설정이 쓴다. 첫 줄은 형식의 이름이고, 그 뒤에 창마다 활성 탭의 위치가 붙은 <code>W</code> 줄이 오고 탭마다
 * <code>T</code> 와 주소가 있는 줄이 이어진다(필드는 탭 문자로 구분한다). 주소만 보관하므로 이력과 폼의 내용은 복원되지 않는다. 파일이 없거나
 * 손상되었으면 복원할 것이 없는 것이다.
 * <p>
 * 사용 예 / Example:
 * <pre>
 * SessionStore store = new SessionStore(BrowserApp.homeDirectory());
 * store.save(windows);
 * List&lt;SessionStore.Window&gt; last = store.load();
 * </pre>
 */
public final class SessionStore {

    /** The name of the file. / 파일의 이름. */
    public static final String FILE_NAME = "session.txt";

    /** The first line of the file. / 파일의 첫 줄. */
    static final String HEADER = "swingwebpanel-session 1";

    /** The largest number of tabs that is restored, over all windows. / 모든 창을 합쳐 복원하는 탭의 최대 개수. */
    private static final int MAX_TABS = 200;

    /**
     * The tabs of one window of a session.
     * <p>
     * 세션에서 창 하나의 탭들.
     */
    public static final class Window {

        /** The addresses of the tabs in the order of the tab strip. / 탭 줄 순서대로의 탭 주소. */
        private final List<String> urls;

        /** The position of the active tab. / 활성 탭의 위치. */
        private final int active;

        /**
         * Creates the description of a window.
         * <p>
         * 창의 설명을 만든다.
         *
         * @param urls   the addresses of the tabs / 탭의 주소
         * @param active the position of the active tab / 활성 탭의 위치
         */
        public Window(List<String> urls, int active) {
            this.urls = new ArrayList<String>(urls);
            this.active = active;
        }

        /**
         * Returns the addresses of the tabs.
         * <p>
         * 탭의 주소를 반환한다.
         *
         * @return a copy of the list / 목록의 복사본
         */
        public List<String> getUrls() {
            return new ArrayList<String>(urls);
        }

        /**
         * Returns the position of the active tab.
         * <p>
         * 활성 탭의 위치를 반환한다.
         *
         * @return the position, inside the list of addresses / 주소 목록 안의 위치
         */
        public int getActive() {
            return active < 0 || active >= urls.size() ? 0 : active;
        }
    }

    /** The file. / 파일. */
    private final File file;

    /**
     * Creates the store for a directory.
     * <p>
     * 디렉터리에 대한 저장소를 만든다.
     *
     * @param directory the directory of the user data / 사용자 데이터 디렉터리
     */
    public SessionStore(File directory) {
        file = new File(directory, FILE_NAME);
    }

    /**
     * Writes the windows of a session into the file.
     * <p>
     * 세션의 창들을 파일에 쓴다.
     *
     * @param windows the windows / 창들
     * @throws IOException if the file cannot be written / 파일을 쓸 수 없으면
     */
    public void save(List<Window> windows) throws IOException {
        StringBuilder out = new StringBuilder();
        out.append(HEADER).append('\n');
        // 창마다 머리 줄과 탭 줄들을 쓴다.
        for (int w = 0; w < windows.size(); w++) {
            Window win = windows.get(w);
            out.append("W\t").append(win.getActive()).append('\n');
            // 탭마다 주소 한 줄을 쓴다.
            for (int t = 0; t < win.urls.size(); t++) {
                out.append("T\t").append(BookmarkStore.escape(win.urls.get(t))).append('\n');
            }
        }
        TextFiles.write(file, out.toString());
    }

    /**
     * Reads the windows of the last session.
     * <p>
     * 지난 세션의 창들을 읽는다.
     *
     * @return the windows that have at least one tab; empty if there is no usable file / 탭이 하나 이상 있는 창들. 쓸 수 있는 파일이 없으면
     *         비어 있다
     */
    public List<Window> load() {
        List<Window> out = new ArrayList<Window>();
        String text;
        try {
            text = TextFiles.read(file);
        } catch (IOException e) {
            return out;
        }
        if (text == null) {
            return out;
        }
        String[] lines = text.split("\n", -1);
        if (lines.length == 0 || !HEADER.equals(strip(lines[0]))) {
            return out;
        }
        List<String> urls = null;
        int active = 0;
        int total = 0;
        // 줄마다 창의 시작이나 탭 하나를 읽는다. 모르는 줄은 건너뛴다.
        for (int i = 1; i < lines.length; i++) {
            String[] f = strip(lines[i]).split("\t", -1);
            if ("W".equals(f[0])) {
                if (urls != null && !urls.isEmpty()) {
                    out.add(new Window(urls, active));
                }
                urls = new ArrayList<String>();
                active = number(f.length > 1 ? f[1] : "0");
            } else if ("T".equals(f[0]) && f.length > 1 && urls != null && total < MAX_TABS) {
                String url = BookmarkStore.unescape(f[1]);
                if (url != null && url.trim().length() > 0) {
                    urls.add(url);
                    total++;
                }
            }
        }
        if (urls != null && !urls.isEmpty()) {
            out.add(new Window(urls, active));
        }
        return out;
    }

    /**
     * Removes a carriage return at the end of a line.
     * <p>
     * 줄 끝의 캐리지 리턴을 뗀다.
     *
     * @param line the line / 줄
     * @return the line without it / 그것을 뗀 줄
     */
    private static String strip(String line) {
        return line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
    }

    /**
     * Reads a whole number that is not negative.
     * <p>
     * 음수가 아닌 정수를 읽는다.
     *
     * @param text the text / 글
     * @return the number, or 0 for text that is not such a number / 수. 그런 수가 아닌 글이면 0
     */
    private static int number(String text) {
        try {
            return Math.max(0, Integer.parseInt(text.trim()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
