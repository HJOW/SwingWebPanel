package org.duckdns.hjow.swingwebpanel.dom.html;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The list of the files selected in a file upload control (<code>input.files</code>): an immutable snapshot. A page can only read the name,
 * size, type and modification time of the files, and a page can never add a file by path: only the host creates {@link FileEntry} objects
 * (from what the user chose).
 * <p>
 * 파일 업로드 컨트롤에서 선택한 파일의 목록(<code>input.files</code>): 불변 스냅샷. 페이지는 파일의 이름, 크기, 타입, 수정 시각만 읽을 수 있으며 경로로 파일을 추가할
 * 수는 없다. {@link FileEntry} 는 호스트(사용자가 고른 것)만 만든다.
 */
public final class FileList {

    /** The files. / 파일들. */
    private final List<FileEntry> files;

    /**
     * Creates a list.
     * <p>
     * 목록을 만든다.
     *
     * @param files the files / 파일들
     */
    FileList(List<FileEntry> files) {
        this.files = Collections.unmodifiableList(new ArrayList<FileEntry>(files));
    }

    /**
     * Returns an empty list (for example the <code>files</code> of a data transfer that holds no files).
     * <p>
     * 빈 목록을 반환한다(예: 파일이 없는 데이터 전달 객체의 <code>files</code>).
     *
     * @return a new empty list / 새 빈 목록
     */
    public static FileList empty() {
        return new FileList(new ArrayList<FileEntry>());
    }

    /**
     * Returns the number of files.
     * <p>
     * 파일 수를 반환한다.
     *
     * @return the length / 길이
     */
    public int getLength() {
        return files.size();
    }

    /**
     * Returns the file at an index.
     * <p>
     * 위치의 파일을 반환한다.
     *
     * @param index the index / 위치
     * @return the file or null / 파일 또는 null
     */
    public FileEntry item(int index) {
        return index >= 0 && index < files.size() ? files.get(index) : null;
    }

    /**
     * Returns the files as an unmodifiable list.
     * <p>
     * 파일을 수정할 수 없는 목록으로 반환한다.
     *
     * @return the files / 파일들
     */
    public List<FileEntry> toList() {
        return files;
    }
}
