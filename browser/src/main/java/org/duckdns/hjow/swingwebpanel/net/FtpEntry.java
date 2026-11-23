package org.duckdns.hjow.swingwebpanel.net;

/**
 * One entry of an FTP directory listing. Entries that cannot be classified safely have the type {@link Type#UNKNOWN} and
 * keep the original <code>raw</code> line so that nothing is ever shown as a directory by guessing.
 * <p>
 * FTP 디렉터리 목록의 항목 하나. 안전하게 분류할 수 없는 항목은 {@link Type#UNKNOWN} 이며 원문 줄(<code>raw</code>)을 보관하여
 * 추측으로 디렉터리처럼 보이는 일이 없게 한다.
 */
public final class FtpEntry {

    /**
     * Kind of a listing entry.
     * <p>
     * 목록 항목의 종류.
     */
    public enum Type {
        /**
         * A regular file.
         * <p>
         * 일반 파일.
         */
        FILE,
        /**
         * A directory entry of the listing.
         * <p>
         * 디렉터리.
         */
        DIRECTORY,
        /**
         * A symbolic link whose target kind is not known.
         * <p>
         * 대상의 종류를 알 수 없는 심볼릭 링크.
         */
        SYMLINK,
        /**
         * Anything that could not be classified (devices, unparsable lines).
         * <p>
         * 분류할 수 없는 항목(장치, 해석할 수 없는 줄).
         */
        UNKNOWN
    }

    /**
     * Entry name without path.
     * <p>
     * 경로가 없는 항목 이름.
     */
    private final String name;
    /**
     * Entry type.
     * <p>
     * 항목 종류.
     */
    private final Type type;
    /**
     * Size in bytes or -1.
     * <p>
     * 바이트 크기. 모르면 -1.
     */
    private final long size;
    /**
     * Modification time text as sent by the server, or null.
     * <p>
     * 서버가 보낸 수정 시각 문자열, 없으면 null.
     */
    private final String modified;
    /**
     * Link target or null.
     * <p>
     * 링크 대상 또는 null.
     */
    private final String linkTarget;
    /**
     * Original listing line.
     * <p>
     * 원래의 목록 줄.
     */
    private final String raw;

    /**
     * Creates an entry.
     * <p>
     * 항목을 만든다.
     *
     * @param name       name / 이름
     * @param type       type / 종류
     * @param size       size or -1 / 크기 또는 -1
     * @param modified   modification text or null / 수정 시각 문자열 또는 null
     * @param linkTarget link target or null / 링크 대상 또는 null
     * @param raw        original line / 원래 줄
     */
    public FtpEntry(String name, Type type, long size, String modified, String linkTarget, String raw) {
        this.name = name;
        this.type = type;
        this.size = size;
        this.modified = modified;
        this.linkTarget = linkTarget;
        this.raw = raw;
    }

    /**
     * Returns the name.
     * <p>
     * 이름을 돌려준다.
     *
     * @return name / 이름
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the type.
     * <p>
     * 종류를 돌려준다.
     *
     * @return type / 종류
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the size or -1.
     * <p>
     * 크기를 돌려준다. 모르면 -1.
     *
     * @return size / 크기
     */
    public long getSize() {
        return size;
    }

    /**
     * Returns the modification time text or null.
     * <p>
     * 수정 시각 문자열을 돌려준다. 없으면 null.
     *
     * @return text / 문자열
     */
    public String getModified() {
        return modified;
    }

    /**
     * Returns the link target or null.
     * <p>
     * 링크 대상을 돌려준다. 없으면 null.
     *
     * @return target / 대상
     */
    public String getLinkTarget() {
        return linkTarget;
    }

    /**
     * Returns the original listing line.
     * <p>
     * 원래의 목록 줄을 돌려준다.
     *
     * @return raw line / 원문 줄
     */
    public String getRaw() {
        return raw;
    }

    /**
     * Returns a short description for logs and tests.
     * <p>
     * 로그와 시험용 짧은 설명을 돌려준다.
     *
     * @return description / 설명
     */
    public String toString() {
        return type + ":" + name + (linkTarget != null ? "->" + linkTarget : "") + (size >= 0 ? "(" + size + ")" : "");
    }
}
