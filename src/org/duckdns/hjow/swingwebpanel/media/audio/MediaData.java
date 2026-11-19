package org.duckdns.hjow.swingwebpanel.media.audio;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The encoded bytes of one fetched media resource (plan item P18-08), either in memory (a short resource) or in a temporary file that this
 * object owns (a long one, see {@link MediaSpool}). Readers get a {@link ByteSource} from {@link #open()}; nothing reads the whole file into
 * memory. {@link #release()} closes every source that is still open and deletes the temporary file; it is idempotent and safe from any thread.
 * The intended owner chain (not wired into the page yet, see <code>docs/progress/media-play.md</code>, MEDIA-E) is: the page fetches into a {@link MediaSpool} and hands the finished data to the media element, which gives it to its player;
 * the element releases it when the resource is replaced (a new load), the player releases it when it ends (also when the element was garbage
 * collected), and the page releases every data it created when it is closed (navigation, <code>clean()</code>, <code>dispose()</code>), so
 * no file outlives its page. No JVM-wide state is used (no <code>deleteOnExit</code>): a file that a crashed process left behind stays for the
 * temporary-file cleaning of the operating system, like the localStorage files.
 * <p>
 * 가져온 미디어 리소스 하나의 인코딩된 바이트(계획 P18-08). 메모리(짧은 리소스)나 이 객체가 소유한 임시 파일(긴 리소스, {@link MediaSpool} 참고)에 있다.
 * 읽는 쪽은 {@link #open()} 으로 {@link ByteSource} 를 받으며 파일 전체를 메모리로 읽는 곳은 없다. {@link #release()} 는 아직 열린 원천을 모두 닫고
 * 임시 파일을 지운다. 여러 번 불러도 되고 어느 스레드에서나 안전하다. 의도한 소유 관계(아직 페이지에 연결하지 않음. <code>docs/progress/media-play.md</code> 의 MEDIA-E 참고): 페이지가 {@link MediaSpool} 로 받아 완성된 데이터를 미디어 요소에
 * 넘기고 요소는 재생기에 준다. 리소스가 바뀌면(새 로드) 요소가, 재생기가 끝나면(요소가 가비지 컬렉션된 경우 포함) 재생기가, 페이지가 닫히면(이동,
 * <code>clean()</code>, <code>dispose()</code>) 페이지가 자기가 만든 데이터를 모두 놓으므로 파일이 페이지보다 오래 남지 않는다. JVM 전역 상태
 * (<code>deleteOnExit</code>)는 쓰지 않는다. 비정상 종료한 프로세스가 남긴 파일은 localStorage 파일처럼 운영체제의 임시 파일 정리에 맡긴다.
 */
public final class MediaData {

    /**
     * The bytes in memory, or null for a file.
     * <p>
     * 메모리의 바이트. 파일이면 null.
     */
    private byte[] bytes;

    /**
     * The temporary file, or null for memory.
     * <p>
     * 임시 파일. 메모리면 null.
     */
    private final File file;

    /**
     * The number of bytes.
     * <p>
     * 바이트 수.
     */
    private final long length;

    /**
     * The sources that were opened and are not closed yet; guarded by this object.
     * <p>
     * 열렸고 아직 닫히지 않은 원천. 이 객체로 보호한다.
     */
    private final List<ByteSource> open = new ArrayList<ByteSource>();

    /**
     * True after {@link #release()}; guarded by this object.
     * <p>
     * {@link #release()} 뒤에 true. 이 객체로 보호한다.
     */
    private boolean released;

    /**
     * Creates the data.
     * <p>
     * 데이터를 만든다.
     *
     * @param bytes  the bytes or null / 바이트 또는 null
     * @param file   the file or null / 파일 또는 null
     * @param length the length / 길이
     */
    private MediaData(byte[] bytes, File file, long length) {
        this.bytes = bytes;
        this.file = file;
        this.length = length;
    }

    /**
     * Wraps bytes that are in memory (not copied).
     * <p>
     * 메모리의 바이트를 감싼다(복사하지 않는다).
     *
     * @param bytes the bytes / 바이트
     * @return the data / 데이터
     */
    public static MediaData of(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("bytes must not be null");
        }
        return new MediaData(bytes, null, bytes.length);
    }

    /**
     * Takes ownership of a complete temporary file: {@link #release()} deletes it.
     * <p>
     * 완성된 임시 파일의 소유권을 가져온다. {@link #release()} 가 지운다.
     *
     * @param file the file / 파일
     * @return the data / 데이터
     */
    public static MediaData ofTemporaryFile(File file) {
        if (file == null) {
            throw new IllegalArgumentException("file must not be null");
        }
        return new MediaData(null, file, file.length());
    }

    /**
     * Returns the number of bytes.
     * <p>
     * 바이트 수를 반환한다.
     *
     * @return the length / 길이
     */
    public long length() {
        return length;
    }

    /**
     * Returns the temporary file, or null when the bytes are in memory (for diagnostics and tests).
     * <p>
     * 임시 파일을 반환한다. 바이트가 메모리에 있으면 null(진단과 시험용).
     *
     * @return the file or null / 파일 또는 null
     */
    public File getFile() {
        return file;
    }

    /**
     * Tells whether {@link #release()} was called.
     * <p>
     * {@link #release()} 가 불렸는지 알려 준다.
     *
     * @return true if released / 놓았으면 true
     */
    public synchronized boolean isReleased() {
        return released;
    }

    /**
     * Opens a reader of the bytes; close it when done (closing does not delete the file).
     * <p>
     * 바이트를 읽는 원천을 연다. 다 쓰면 닫는다(닫아도 파일은 지워지지 않는다).
     *
     * @return the source / 원천
     * @throws IOException if the data was released or the file cannot be opened / 데이터를 놓았거나 파일을 열 수 없으면
     */
    public synchronized ByteSource open() throws IOException {
        if (released) {
            throw new IOException("the media data was released");
        }
        if (bytes != null) {
            return new ArrayByteSource(bytes);
        }
        Handle h = new Handle(new FileByteSource(file));
        open.add(h);
        return h;
    }

    /**
     * Closes the open sources and deletes the temporary file (idempotent). Reads of a source that is still in use fail afterwards with a
     * {@link ByteSourceException}.
     * <p>
     * 열린 원천을 닫고 임시 파일을 지운다(여러 번 불러도 된다). 아직 쓰는 원천의 읽기는 그 뒤 {@link ByteSourceException} 으로 실패한다.
     */
    public void release() {
        List<ByteSource> toClose;
        synchronized (this) {
            if (released) {
                return;
            }
            released = true;
            bytes = null;
            toClose = new ArrayList<ByteSource>(open);
            open.clear();
        }
        // 열린 원천을 모두 닫아야(Windows) 파일을 지울 수 있다.
        for (ByteSource s : toClose) {
            ((Handle) s).inner.close();
        }
        if (file != null && file.exists() && !file.delete()) {
            // 지울 수 없으면(다른 프로세스가 열고 있음 등) 운영체제의 임시 파일 정리에 맡긴다.
            return;
        }
    }

    /**
     * Forgets a source that its user closed.
     * <p>
     * 사용자가 닫은 원천을 잊는다.
     *
     * @param h the source / 원천
     */
    private synchronized void closed(Handle h) {
        open.remove(h);
    }

    /**
     * A file source that tells its data when it is closed.
     * <p>
     * 닫힐 때 자기 데이터에 알리는 파일 원천.
     */
    private final class Handle implements ByteSource {

        /**
         * The file source.
         * <p>
         * 파일 원천.
         */
        final FileByteSource inner;

        /**
         * Wraps a file source.
         * <p>
         * 파일 원천을 감싼다.
         *
         * @param inner the source / 원천
         */
        Handle(FileByteSource inner) {
            this.inner = inner;
        }

        /**
         * Returns the length.
         * <p>
         * 길이를 반환한다.
         *
         * @return the length / 길이
         */
        public long length() {
            return inner.length();
        }

        /**
         * Returns a byte.
         * <p>
         * 바이트를 반환한다.
         *
         * @param position the position / 위치
         * @return the byte / 바이트
         */
        public int get(long position) {
            return inner.get(position);
        }

        /**
         * Copies bytes.
         * <p>
         * 바이트를 복사한다.
         *
         * @param position the first position / 첫 위치
         * @param buffer   the destination / 대상
         * @param offset   the start in the destination / 대상의 시작
         * @param count    the bytes wanted / 원하는 바이트 수
         * @return the bytes copied / 복사한 바이트 수
         */
        public int read(long position, byte[] buffer, int offset, int count) {
            return inner.read(position, buffer, offset, count);
        }

        /**
         * Closes the file handle and forgets it.
         * <p>
         * 파일 핸들을 닫고 잊는다.
         */
        public void close() {
            inner.close();
            closed(this);
        }
    }
}
