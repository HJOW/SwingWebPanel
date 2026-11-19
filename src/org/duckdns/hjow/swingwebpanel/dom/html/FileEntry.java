package org.duckdns.hjow.swingwebpanel.dom.html;

/**
 * One file that the user selected for a file upload control (<code>input type=file</code>): the file name without any directory, the MIME
 * type, the modification time and the bytes. The host (the file chooser of the Swing component) reads the file the user picked and
 * hands it over; a page can only see the name, size, type and modification time of such files and can never choose a path itself.
 * Instances are immutable.
 * <p>
 * 파일 업로드 컨트롤(<code>input type=file</code>)에 사용자가 선택한 파일 하나: 디렉터리가 없는 파일 이름, MIME 타입, 수정 시각, 바이트. 호스트(Swing 컴포넌트의
 * 파일 선택기)가 사용자가 고른 파일을 읽어 넘기며, 페이지는 그런 파일의 이름, 크기, 타입, 수정 시각만 볼 수 있고 스스로 경로를 고를 수는 없다. 인스턴스는
 * 불변이다.
 */
public final class FileEntry {

    /** The file name without a directory. / 디렉터리가 없는 파일 이름. */
    private final String name;

    /** The MIME type, empty if unknown. / MIME 타입. 모르면 빈 문자열. */
    private final String type;

    /** The modification time in milliseconds since the epoch. / 에포크 이후 밀리초의 수정 시각. */
    private final long lastModified;

    /** The content (never modified). / 내용(바뀌지 않는다). */
    private final byte[] content;

    /**
     * Creates a file entry; directory parts of the name are dropped and the bytes are copied.
     * <p>
     * 파일 항목을 만든다. 이름의 디렉터리 부분은 버리고 바이트는 복사한다.
     *
     * @param name         file name (a path is reduced to its last segment) / 파일 이름(경로는 마지막 구간만 남긴다)
     * @param type         MIME type or null / MIME 타입 또는 null
     * @param lastModified modification time in milliseconds since the epoch / 에포크 이후 밀리초의 수정 시각
     * @param content      the bytes / 바이트
     */
    public FileEntry(String name, String type, long lastModified, byte[] content) {
        String n = name == null ? "" : name;
        int cut = Math.max(n.lastIndexOf('/'), n.lastIndexOf('\\'));
        this.name = cut >= 0 ? n.substring(cut + 1) : n;
        this.type = type == null ? "" : HtmlText.lower(type);
        this.lastModified = lastModified;
        this.content = content == null ? new byte[0] : content.clone();
    }

    /**
     * Returns the file name.
     * <p>
     * 파일 이름을 반환한다.
     *
     * @return the name without directory / 디렉터리가 없는 이름
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the MIME type.
     * <p>
     * MIME 타입을 반환한다.
     *
     * @return the type, empty if unknown / 타입. 모르면 빈 문자열
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the modification time.
     * <p>
     * 수정 시각을 반환한다.
     *
     * @return milliseconds since the epoch / 에포크 이후 밀리초
     */
    public long getLastModified() {
        return lastModified;
    }

    /**
     * Returns the size in bytes.
     * <p>
     * 바이트 크기를 반환한다.
     *
     * @return the size / 크기
     */
    public long getSize() {
        return content.length;
    }

    /**
     * Returns a copy of the bytes.
     * <p>
     * 바이트의 복사본을 반환한다.
     *
     * @return the bytes / 바이트
     */
    public byte[] getContent() {
        return content.clone();
    }

    /**
     * Returns the bytes without copying (package use only; callers must not modify them).
     * <p>
     * 복사 없이 바이트를 반환한다(패키지 내부용. 호출자가 바꾸면 안 된다).
     *
     * @return the bytes / 바이트
     */
    byte[] contentRef() {
        return content;
    }
}
