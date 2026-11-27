package org.duckdns.hjow.swingwebpanel.desktopapp.model;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * The file of the settings (plan item P31-07): <code>settings.properties</code> in the directory of the user data, UTF-8 text in the
 * format of <code>java.util.Properties</code> with one <code>key=value</code> per line, written in a fixed order. The line
 * <code>settings.version=1</code> names the format. A missing file means the defaults; a file that cannot be read as properties, or
 * that lacks the version line, is kept as <code>settings.properties.broken</code> and the application starts with the defaults. Keys
 * that this version does not know are ignored; a value that cannot be understood falls back to its default when it is used.
 * <p>
 * 설정의 파일(계획 P31-07): 사용자 데이터 디렉터리의 <code>settings.properties</code>. <code>java.util.Properties</code> 형식의 UTF-8 텍스트이며 한
 * 줄에 <code>키=값</code> 하나를 정해진 순서로 쓴다. <code>settings.version=1</code> 줄이 형식의 이름이다. 파일이 없으면 기본값이고, 속성 형식으로
 * 읽을 수 없거나 판 줄이 없는 파일은 <code>settings.properties.broken</code> 으로 보존한 뒤 기본값으로 시작한다. 이 판이 모르는 키는 무시하고,
 * 이해할 수 없는 값은 쓸 때 기본값으로 돌아간다.
 * <p>
 * 사용 예 / Example:
 * <pre>
 * SettingsStore store = new SettingsStore(BrowserApp.homeDirectory());
 * store.load(settings);
 * store.save(settings);
 * </pre>
 */
public final class SettingsStore {

    /** The name of the file. / 파일의 이름. */
    public static final String FILE_NAME = "settings.properties";

    /** The key of the line that names the format. / 형식의 이름을 적는 줄의 키. */
    static final String VERSION_KEY = "settings.version";

    /** The version of the format that this class writes. / 이 클래스가 쓰는 형식의 판. */
    static final String VERSION = "1";

    /** The file. / 파일. */
    private final File file;

    /**
     * Creates the store for a directory.
     * <p>
     * 디렉터리에 대한 저장소를 만든다.
     *
     * @param directory the directory of the user data / 사용자 데이터 디렉터리
     */
    public SettingsStore(File directory) {
        file = new File(directory, FILE_NAME);
    }

    /**
     * Returns the file of the settings.
     * <p>
     * 설정 파일을 반환한다.
     *
     * @return the file / 파일
     */
    public File getFile() {
        return file;
    }

    /**
     * Reads the file into settings. A missing file leaves the settings as they are. A file that cannot be used is kept under the name
     * <code>settings.properties.broken</code> and the settings are left as they are.
     * <p>
     * 파일을 설정으로 읽는다. 파일이 없으면 설정을 그대로 둔다. 쓸 수 없는 파일은 <code>settings.properties.broken</code> 이라는 이름으로 보존하고
     * 설정을 그대로 둔다.
     *
     * @param settings the settings to fill / 채울 설정
     * @return false if a file was there but could not be used / 파일이 있었지만 쓸 수 없었으면 false
     */
    public boolean load(Settings settings) {
        Map<String, String> read;
        try {
            String text = TextFiles.read(file);
            if (text == null) {
                return true;
            }
            read = parse(text);
        } catch (IOException e) {
            read = null;
        }
        // 해석하지 못한 파일은 사용자가 볼 수 있게 남겨 두고 기본값으로 시작한다.
        if (read == null) {
            TextFiles.keepBroken(file);
            return false;
        }
        settings.setAll(read);
        return true;
    }

    /**
     * Writes settings into the file.
     * <p>
     * 설정을 파일에 쓴다.
     *
     * @param settings the settings / 설정
     * @throws IOException if the file cannot be written / 파일을 쓸 수 없으면
     */
    public void save(Settings settings) throws IOException {
        TextFiles.write(file, format(settings.toMap()));
    }

    /**
     * Reads the text of the file.
     * <p>
     * 파일의 텍스트를 읽는다.
     *
     * @param text the text / 텍스트
     * @return the values of the known keys, or null if the text is not a settings file of a known version / 아는 키의 값. 텍스트가 아는
     *         판의 설정 파일이 아니면 null
     */
    static Map<String, String> parse(String text) {
        Properties p = new Properties();
        try {
            p.load(new StringReader(text));
        } catch (IOException e) {
            return null;
        } catch (IllegalArgumentException e) {
            // 잘못된 유니코드 이스케이프가 있으면 속성 형식이 아니다.
            return null;
        }
        if (!VERSION.equals(p.getProperty(VERSION_KEY, "").trim())) {
            return null;
        }
        Map<String, String> out = new LinkedHashMap<String, String>();
        List<String> keys = Settings.keys();
        // 아는 키만 가져온다.
        for (int i = 0; i < keys.size(); i++) {
            String v = p.getProperty(keys.get(i));
            if (v != null) {
                out.put(keys.get(i), v);
            }
        }
        return out;
    }

    /**
     * Writes values as the text of the file: the version line first, then one line per key in the given order.
     * <p>
     * 값을 파일의 텍스트로 적는다: 판 줄이 먼저 오고, 그 뒤에 주어진 순서대로 키마다 한 줄씩 쓴다.
     *
     * @param values the values by key / 키별 값
     * @return the text / 텍스트
     */
    static String format(Map<String, String> values) {
        StringBuilder out = new StringBuilder();
        out.append("# SwingWebPanel Browser settings\n");
        out.append(VERSION_KEY).append('=').append(VERSION).append('\n');
        // 키마다 한 줄을 쓴다.
        for (Map.Entry<String, String> e : values.entrySet()) {
            out.append(e.getKey()).append('=').append(escape(e.getValue())).append('\n');
        }
        return out.toString();
    }

    /**
     * Escapes a value so that <code>java.util.Properties</code> reads it back unchanged. Characters outside ASCII are written as they
     * are (the file is UTF-8).
     * <p>
     * <code>java.util.Properties</code> 가 그대로 다시 읽도록 값을 이스케이프한다. ASCII 밖의 문자는 그대로 쓴다(파일이 UTF-8 이다).
     *
     * @param value the value / 값
     * @return the escaped text / 이스케이프한 텍스트
     */
    static String escape(String value) {
        StringBuilder out = new StringBuilder(value.length() + 8);
        // 문자마다 속성 형식에서 의미가 있는 것을 이스케이프한다. 맨 앞의 공백은 값의 일부임을 표시한다.
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\') {
                out.append("\\\\");
            } else if (c == '\n') {
                out.append("\\n");
            } else if (c == '\r') {
                out.append("\\r");
            } else if (c == '\t') {
                out.append("\\t");
            } else if (c == '\f') {
                out.append("\\f");
            } else if (c == ' ' && i == 0) {
                out.append("\\ ");
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
