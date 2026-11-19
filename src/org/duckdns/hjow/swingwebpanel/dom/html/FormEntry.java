package org.duckdns.hjow.swingwebpanel.dom.html;

/**
 * One entry of a form's entry list (HTML Standard 4.10.22.4): a name and a value that is either a string or a file. Instances are
 * immutable.
 * <p>
 * 폼의 항목 목록(HTML 표준 4.10.22.4)의 항목 하나: 이름과, 문자열 또는 파일인 값. 인스턴스는 불변이다.
 */
public final class FormEntry {

    /** The name. / 이름. */
    private final String name;

    /** The string value, or null for a file. / 문자열 값. 파일이면 null. */
    private final String value;

    /** The file, or null for a string value. / 파일. 문자열 값이면 null. */
    private final FileEntry file;

    /**
     * Creates a string entry.
     * <p>
     * 문자열 항목을 만든다.
     *
     * @param name  the name / 이름
     * @param value the value / 값
     */
    public FormEntry(String name, String value) {
        this.name = name;
        this.value = value;
        this.file = null;
    }

    /**
     * Creates a file entry.
     * <p>
     * 파일 항목을 만든다.
     *
     * @param name the name / 이름
     * @param file the file / 파일
     */
    public FormEntry(String name, FileEntry file) {
        this.name = name;
        this.value = null;
        this.file = file;
    }

    /**
     * Returns the name.
     * <p>
     * 이름을 반환한다.
     *
     * @return the name / 이름
     */
    public String getName() {
        return name;
    }

    /**
     * Tells whether the value is a file.
     * <p>
     * 값이 파일인지 알려 준다.
     *
     * @return true for a file / 파일이면 true
     */
    public boolean isFile() {
        return file != null;
    }

    /**
     * Returns the string value.
     * <p>
     * 문자열 값을 반환한다.
     *
     * @return the value, or null for a file entry / 값. 파일 항목이면 null
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the file.
     * <p>
     * 파일을 반환한다.
     *
     * @return the file, or null for a string entry / 파일. 문자열 항목이면 null
     */
    public FileEntry getFile() {
        return file;
    }

    /**
     * Describes the entry for debugging.
     * <p>
     * 디버깅용으로 항목을 설명한다.
     *
     * @return the description / 설명
     */
    public String toString() {
        return name + "=" + (file != null ? "<file " + file.getName() + ">" : value);
    }
}
