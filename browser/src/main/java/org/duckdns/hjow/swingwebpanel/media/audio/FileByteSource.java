package org.duckdns.hjow.swingwebpanel.media.audio;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * A {@link ByteSource} over a complete file (the temporary file into which a long media resource was spooled, plan item P18-08). Only one
 * window of the file (64 KiB by default) is kept in memory: a read inside the window is a copy, a read outside loads the window that contains
 * it, and a large read goes to the file directly. The length is the file length when the source was opened (the file must not grow
 * afterwards). Thread-safe: all methods are synchronized. {@link #close()} releases the handle; on Windows a file can be deleted only after
 * every handle of it is closed, which is why the owner ({@link MediaData}) closes its sources before it deletes the file.
 * <p>
 * 완성된 파일 위의 {@link ByteSource}(긴 미디어 리소스를 받아 둔 임시 파일, 계획 P18-08). 파일의 창 하나(기본 64 KiB)만 메모리에 둔다: 창 안의 읽기는
 * 복사이고, 창 밖의 읽기는 그 위치를 담은 창을 읽어 들이며, 큰 읽기는 파일에서 바로 읽는다. 길이는 원천을 열 때의 파일 길이이다(그 뒤 파일이 자라면
 * 안 된다). 모든 메서드가 동기화되어 스레드 안전하다. {@link #close()} 는 핸들을 놓는다. Windows 에서는 모든 핸들을 닫아야 파일을 지울 수 있으므로
 * 소유자({@link MediaData})는 파일을 지우기 전에 원천을 닫는다.
 */
public final class FileByteSource implements ByteSource {

    /**
     * The default size of the window in bytes.
     * <p>
     * 창의 기본 크기(바이트).
     */
    public static final int DEFAULT_WINDOW = 64 * 1024;

    /**
     * The open file, null after closing.
     * <p>
     * 열린 파일. 닫은 뒤에는 null.
     */
    private RandomAccessFile file;

    /**
     * A description of the file for messages.
     * <p>
     * 메시지에 쓰는 파일 설명.
     */
    private final String name;

    /**
     * The length of the file.
     * <p>
     * 파일 길이.
     */
    private final long length;

    /**
     * The window: the bytes of the file that are kept in memory.
     * <p>
     * 창: 메모리에 두는 파일의 바이트.
     */
    private final byte[] window;

    /**
     * The file position of the first byte of the window.
     * <p>
     * 창의 첫 바이트의 파일 위치.
     */
    private long windowStart;

    /**
     * The number of valid bytes in the window (0 before the first load).
     * <p>
     * 창 안의 유효한 바이트 수(처음 읽기 전에는 0).
     */
    private int windowLength;

    /**
     * Opens a file with the default window.
     * <p>
     * 기본 창으로 파일을 연다.
     *
     * @param f the file / 파일
     * @throws IOException if it cannot be opened / 열 수 없으면
     */
    public FileByteSource(File f) throws IOException {
        this(f, DEFAULT_WINDOW);
    }

    /**
     * Opens a file with a window of a given size (tests use tiny windows to cross many window borders).
     * <p>
     * 정한 크기의 창으로 파일을 연다(시험은 창 경계를 많이 넘도록 아주 작은 창을 쓴다).
     *
     * @param f          the file / 파일
     * @param windowSize the window size, at least 1 / 창 크기(1 이상)
     * @throws IOException if it cannot be opened / 열 수 없으면
     */
    public FileByteSource(File f, int windowSize) throws IOException {
        if (windowSize < 1) {
            throw new IllegalArgumentException("windowSize must be positive");
        }
        this.file = new RandomAccessFile(f, "r");
        this.name = f.getName();
        try {
            this.length = file.length();
        } catch (IOException e) {
            file.close();
            throw e;
        }
        this.window = new byte[windowSize];
    }

    /**
     * Returns the file length.
     * <p>
     * 파일 길이를 반환한다.
     *
     * @return the length / 길이
     */
    public long length() {
        return length;
    }

    /**
     * Returns the unsigned byte at a position, from the window (loaded when needed).
     * <p>
     * 위치의 부호 없는 바이트를 창에서 반환한다(필요하면 창을 읽어 들인다).
     *
     * @param position the position / 위치
     * @return the byte / 바이트
     */
    public synchronized int get(long position) {
        if (position < 0 || position >= length) {
            throw new IndexOutOfBoundsException("position " + position + " outside 0.." + length);
        }
        long rel = position - windowStart;
        if (rel < 0 || rel >= windowLength) {
            load(position);
            rel = position - windowStart;
        }
        return window[(int) rel] & 0xFF;
    }

    /**
     * Copies bytes, through the window for small reads and directly from the file for large ones.
     * <p>
     * 바이트를 복사한다. 작은 읽기는 창을 거치고 큰 읽기는 파일에서 바로 읽는다.
     *
     * @param position the first position / 첫 위치
     * @param buffer   the destination / 대상
     * @param offset   the start in the destination / 대상의 시작
     * @param count    the bytes wanted / 원하는 바이트 수
     * @return the bytes copied / 복사한 바이트 수
     */
    public synchronized int read(long position, byte[] buffer, int offset, int count) {
        if (position < 0 || offset < 0 || count < 0 || (long) offset + count > buffer.length) {
            throw new IndexOutOfBoundsException("invalid read range");
        }
        if (position >= length) {
            return 0;
        }
        int n = (int) Math.min(count, length - position);
        int done = 0;
        // 창에 있는 부분은 복사하고, 없는 부분은 창을 옮기거나(작은 나머지) 파일에서 바로 읽는다(큰 나머지).
        while (done < n) {
            long p = position + done;
            long rel = p - windowStart;
            if (rel >= 0 && rel < windowLength) {
                int c = (int) Math.min(n - done, windowLength - rel);
                System.arraycopy(window, (int) rel, buffer, offset + done, c);
                done += c;
            } else if (n - done >= window.length) {
                readFully(p, buffer, offset + done, n - done);
                done = n;
            } else {
                load(p);
            }
        }
        return n;
    }

    /**
     * Loads the window that contains a position (windows start at multiples of the window size).
     * <p>
     * 위치를 담은 창을 읽어 들인다(창은 창 크기의 배수에서 시작한다).
     *
     * @param position the position inside the file / 파일 안의 위치
     */
    private void load(long position) {
        long start = position - position % window.length;
        int n = (int) Math.min(window.length, length - start);
        windowLength = 0;
        readFully(start, window, 0, n);
        windowStart = start;
        windowLength = n;
    }

    /**
     * Reads exactly a number of bytes from the file.
     * <p>
     * 파일에서 정확히 그 수만큼 읽는다.
     *
     * @param position the file position / 파일 위치
     * @param buffer   the destination / 대상
     * @param offset   the start in the destination / 대상의 시작
     * @param count    the bytes / 바이트 수
     */
    private void readFully(long position, byte[] buffer, int offset, int count) {
        RandomAccessFile f = file;
        if (f == null) {
            throw new ByteSourceException("the media file " + name + " is closed", null);
        }
        try {
            f.seek(position);
            f.readFully(buffer, offset, count);
        } catch (IOException e) {
            throw new ByteSourceException("the media file " + name + " cannot be read: " + e.getMessage(), e);
        }
    }

    /**
     * Closes the file handle (idempotent).
     * <p>
     * 파일 핸들을 닫는다(여러 번 불러도 된다).
     */
    public synchronized void close() {
        RandomAccessFile f = file;
        file = null;
        windowLength = 0;
        if (f != null) {
            try {
                f.close();
            } catch (IOException e) {
                // 닫기 실패는 무시한다(읽기 전용 핸들).
                return;
            }
        }
    }
}
