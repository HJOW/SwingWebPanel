package org.duckdns.hjow.swingwebpanel.desktopapp.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The settings, their file and the file of the last session (plan item P31-07): typed values fall back to the default when the stored
 * text cannot be understood, the file keeps every value, a missing file means the defaults and a broken file is kept under another
 * name; the session file keeps the tabs of the windows.
 * <p>
 * 설정, 그 파일, 지난 세션의 파일(계획 P31-07): 형식이 있는 값은 저장된 글을 이해할 수 없으면 기본값으로 돌아가고, 파일은 모든 값을 보존하며, 파일이
 * 없으면 기본값이고 손상된 파일은 다른 이름으로 보존한다. 세션 파일은 창의 탭들을 보존한다.
 */
public class SettingsStoreTest {

    /** The directory of the case. / 사례의 디렉터리. */
    private File dir;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public SettingsStoreTest() {
    }

    /**
     * Creates a temporary directory.
     * <p>
     * 임시 디렉터리를 만든다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void setUp() throws Exception {
        dir = File.createTempFile("swp-settings", "");
        Check.isTrue(dir.delete() && dir.mkdir(), "a temporary directory");
    }

    /**
     * Removes the temporary directory.
     * <p>
     * 임시 디렉터리를 지운다.
     */
    public void tearDown() {
        File[] files = dir.listFiles();
        // 사례가 쓴 파일을 지운다.
        for (int i = 0; files != null && i < files.length; i++) {
            files[i].delete();
        }
        dir.delete();
    }

    /**
     * Typed values: truth values, numbers in a range and choices fall back to the default of the key when the text is something else.
     * <p>
     * 형식이 있는 값: 참/거짓, 범위 안의 수, 선택 값은 글이 다른 것이면 키의 기본값으로 돌아간다.
     */
    public void testTypedValuesFallBackToDefaults() {
        Settings s = new Settings();
        Check.equal(16, s.getInt(Settings.FONT_SIZE, 6, 72), "the default font size");
        s.set(Settings.FONT_SIZE, " 20 ");
        Check.equal(20, s.getInt(Settings.FONT_SIZE, 6, 72), "a number with spaces");
        s.set(Settings.FONT_SIZE, "200");
        Check.equal(16, s.getInt(Settings.FONT_SIZE, 6, 72), "a number outside the range gives the default");
        s.set(Settings.FONT_SIZE, "big");
        Check.equal(16, s.getInt(Settings.FONT_SIZE, 6, 72), "text gives the default");
        String[] places = { Settings.PLACE_POPUP, Settings.PLACE_WINDOW, Settings.PLACE_TAB };
        Check.equal(Settings.PLACE_POPUP, s.getChoice(Settings.POPUP_PLACE, places), "the default place of popups");
        s.set(Settings.POPUP_PLACE, Settings.PLACE_TAB);
        Check.equal(Settings.PLACE_TAB, s.getChoice(Settings.POPUP_PLACE, places), "an allowed choice");
        s.set(Settings.POPUP_PLACE, "sidebar");
        Check.equal(Settings.PLACE_POPUP, s.getChoice(Settings.POPUP_PLACE, places), "an unknown choice gives the default");
        Check.isTrue(s.getBoolean(Settings.BLOCK_POPUPS), "popups are blocked by default");
        Check.isTrue(s.getBoolean(Settings.JAVASCRIPT), "scripts run by default");
        Check.isFalse(s.getBoolean(Settings.SCRIPTS_CLOSE_WINDOWS), "scripts do not close tabs by default");
        s.set(Settings.BLOCK_POPUPS, "maybe");
        Check.isTrue(s.getBoolean(Settings.BLOCK_POPUPS), "text that is neither true nor false gives the default");
        s.set(Settings.BLOCK_POPUPS, null);
        Check.equal("true", s.get(Settings.BLOCK_POPUPS), "null sets the default");
    }

    /**
     * The file keeps every value, also text with characters outside ASCII, backslashes, separators and spaces at the ends; unknown keys
     * of the file are ignored.
     * <p>
     * 파일은 모든 값을 보존한다. ASCII 밖의 문자, 역슬래시, 구분자, 양 끝의 공백이 든 글도 그렇다. 파일의 모르는 키는 무시한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFileRoundTrip() throws Exception {
        SettingsStore store = new SettingsStore(new File(dir, "sub"));
        Settings s = new Settings();
        Check.isTrue(store.load(s), "a missing file is not a problem");
        Check.equal("about:blank", s.get(Settings.HOME_PAGE), "a missing file leaves the defaults");
        s.set(Settings.HOME_PAGE, "http://example.com/한글?a=1&b=2#c");
        s.set(Settings.DOWNLOAD_DIRECTORY, "C:\\Users\\tester\\내 다운로드");
        s.set(Settings.USER_AGENT, " spaces at both ends ");
        s.set(Settings.SEARCH_TEMPLATE, "https://search.example/?q=%s&x=y:z!#");
        s.set(Settings.FONT_STANDARD, "맑은 고딕");
        s.set(Settings.FONT_SIZE, "20");
        s.set(Settings.JAVASCRIPT, "false");
        store.save(s);
        String text = TextFiles.read(store.getFile());
        Check.contains(text, "settings.version=1\n", "the file names its format");
        Check.contains(text, "font.standard=맑은 고딕\n", "text outside ASCII is written as it is");
        Check.contains(text, "download.directory=C:\\\\Users\\\\tester\\\\내 다운로드\n", "backslashes are escaped");
        Settings back = new Settings();
        Check.isTrue(store.load(back), "the file is read");
        Check.equal(s.toMap(), back.toMap(), "the read settings are the written ones");
        TextFiles.write(store.getFile(), "settings.version=1\nhome.page=http://kept/\nfuture.key=whatever\nfont.size=nonsense\n");
        Settings partial = new Settings();
        Check.isTrue(store.load(partial), "a file with unknown keys is read");
        Check.equal("http://kept/", partial.get(Settings.HOME_PAGE), "known keys are taken");
        Check.isFalse(partial.toMap().containsKey("future.key"), "unknown keys are ignored");
        Check.equal(16, partial.getInt(Settings.FONT_SIZE, 6, 72), "a value that cannot be understood gives the default when it is used");
        Check.equal("true", partial.get(Settings.JAVASCRIPT), "keys that the file lacks keep their default");
    }

    /**
     * A file that is not a settings file of this version is kept under another name and the settings stay at their defaults.
     * <p>
     * 이 판의 설정 파일이 아닌 파일은 다른 이름으로 보존하고 설정은 기본값으로 둔다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBrokenFileIsKept() throws Exception {
        SettingsStore store = new SettingsStore(dir);
        TextFiles.write(store.getFile(), "home.page=http://no.version/\n");
        Settings s = new Settings();
        Check.isFalse(store.load(s), "a file without the version line is reported");
        Check.equal("about:blank", s.get(Settings.HOME_PAGE), "and its values are not taken");
        File kept = new File(store.getFile().getPath() + TextFiles.BROKEN_SUFFIX);
        Check.equal("home.page=http://no.version/\n", TextFiles.read(kept), "the file is kept under another name");
        Check.isFalse(store.getFile().exists(), "the place of the file is free");
        TextFiles.write(store.getFile(), "settings.version=1\nhome.page=bad \\u00zz escape\n");
        Check.isFalse(store.load(s), "a broken escape is reported");
        TextFiles.write(store.getFile(), "settings.version=2\nhome.page=http://future/\n");
        Check.isFalse(store.load(s), "another version is reported");
        store.save(s);
        Check.isTrue(store.load(new Settings()), "a new file can be written in its place");
    }

    /**
     * The session file keeps the addresses of the tabs of each window and the position of the active tab; windows without tabs are
     * left out, and a missing or broken file gives nothing.
     * <p>
     * 세션 파일은 창마다 탭의 주소와 활성 탭의 위치를 보존한다. 탭이 없는 창은 빼고, 파일이 없거나 손상되었으면 아무것도 주지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSessionFile() throws Exception {
        SessionStore store = new SessionStore(dir);
        Check.equal(0, store.load().size(), "a missing file gives no windows");
        List<SessionStore.Window> windows = new ArrayList<SessionStore.Window>();
        List<String> first = new ArrayList<String>();
        first.add("http://a/");
        first.add("file:///C:/한글 폴더/b.html");
        first.add("http://c/?x=1\ty");
        windows.add(new SessionStore.Window(first, 1));
        windows.add(new SessionStore.Window(new ArrayList<String>(), 0));
        List<String> second = new ArrayList<String>();
        second.add("http://d/");
        windows.add(new SessionStore.Window(second, 7));
        store.save(windows);
        List<SessionStore.Window> back = store.load();
        Check.equal(2, back.size(), "the window without tabs is left out");
        Check.equal(first, back.get(0).getUrls(), "the tabs of the first window, in order");
        Check.equal(1, back.get(0).getActive(), "the active tab of the first window");
        Check.equal(second, back.get(1).getUrls(), "the tabs of the second window");
        Check.equal(0, back.get(1).getActive(), "an active position outside the tabs gives the first tab");
        TextFiles.write(new File(dir, SessionStore.FILE_NAME), "not a session\nT\thttp://x/\n");
        Check.equal(0, store.load().size(), "a file of another kind gives no windows");
        TextFiles.write(new File(dir, SessionStore.FILE_NAME), SessionStore.HEADER + "\r\nT\thttp://before.a.window/\r\nW\tx\r\nT\thttp://ok/\r\n?\r\n");
        back = store.load();
        Check.equal(1, back.size(), "lines that cannot be understood are skipped");
        Check.equal("http://ok/", back.get(0).getUrls().get(0), "the tab of the window");
    }
}
