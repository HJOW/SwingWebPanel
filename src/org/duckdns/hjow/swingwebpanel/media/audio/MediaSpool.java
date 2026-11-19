package org.duckdns.hjow.swingwebpanel.media.audio;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * The receiver of the body of a media response while it streams in (plan item P18-08): the first {@link #MEMORY_LIMIT} bytes stay in memory;
 * when the body grows beyond that, everything moves to a temporary file (<code>File.createTempFile</code> in the default temporary directory)
 * and the rest is appended there, so a long recording never sits in the heap. A body larger than {@link #MAX_BYTES} fails with an
 * {@link IOException}, which aborts the request. {@link #finish()} turns the received bytes into a {@link MediaData} that owns the file;
 * {@link #discard()} deletes a partial file (failure, cancellation). Used by one fetching thread.
 * <p>
 * 미디어 응답의 본문이 흘러 들어오는 동안의 받는 쪽(계획 P18-08): 처음 {@link #MEMORY_LIMIT} 바이트는 메모리에 두고, 본문이 그보다 커지면 모두 임시
 * 파일(기본 임시 디렉터리의 <code>File.createTempFile</code>)로 옮겨 나머지를 거기에 붙이므로 긴 녹음이 힙에 놓이지 않는다. {@link #MAX_BYTES} 보다 큰
 * 본문은 {@link IOException} 으로 실패하여 요청을 중단한다. {@link #finish()} 는 받은 바이트를 파일을 소유하는 {@link MediaData} 로 바꾸고
 * {@link #discard()} 는 덜 받은 파일을 지운다(실패, 취소). 가져오는 스레드 하나가 쓴다.
 */
public final class MediaSpool extends OutputStream {

    /**
     * The largest body that stays in memory: 1 MiB (a short sound; anything longer goes to a temporary file).
     * <p>
     * 메모리에 남는 가장 큰 본문: 1 MiB(짧은 소리. 더 긴 것은 임시 파일로 간다).
     */
    public static final int MEMORY_LIMIT = 1024 * 1024;

    /**
     * The largest media body that is accepted on disk: 2 GiB minus one byte (<code>Integer.MAX_VALUE</code>), so that every position inside
     * the resource fits a Java <code>int</code> as the container parsers expect. A larger body fails like an unsupported resource.
     * <p>
     * 디스크에 받아들이는 가장 큰 미디어 본문: 2 GiB 에서 1 바이트 뺀 값(<code>Integer.MAX_VALUE</code>). 컨테이너 해석기가 기대하듯 리소스 안의 모든 위치가
     * Java <code>int</code> 에 들어가도록 한다. 더 큰 본문은 지원하지 않는 리소스처럼 실패한다.
     */
    public static final long MAX_BYTES = Integer.MAX_VALUE;

    /**
     * The prefix of the temporary file names.
     * <p>
     * 임시 파일 이름의 접두사.
     */
    static final String FILE_PREFIX = "swingwebpanel-media-";

    /**
     * The limit of this spool.
     * <p>
     * 이 받는 쪽의 한도.
     */
    private final long maxBytes;

    /**
     * The memory threshold of this spool.
     * <p>
     * 이 받는 쪽의 메모리 문턱.
     */
    private final int memoryLimit;

    /**
     * The bytes in memory, null after moving to the file.
     * <p>
     * 메모리의 바이트. 파일로 옮긴 뒤에는 null.
     */
    private ByteArrayOutputStream memory = new ByteArrayOutputStream();

    /**
     * The temporary file, null while in memory.
     * <p>
     * 임시 파일. 메모리에 있는 동안은 null.
     */
    private File file;

    /**
     * The stream into the file, null while in memory or after closing.
     * <p>
     * 파일로 가는 스트림. 메모리에 있거나 닫은 뒤에는 null.
     */
    private OutputStream out;

    /**
     * The bytes received.
     * <p>
     * 받은 바이트 수.
     */
    private long count;

    /**
     * True after {@link #finish()} or {@link #discard()}.
     * <p>
     * {@link #finish()} 나 {@link #discard()} 뒤에 true.
     */
    private boolean done;

    /**
     * Creates a spool with the default limits.
     * <p>
     * 기본 한도로 받는 쪽을 만든다.
     */
    public MediaSpool() {
        this(MEMORY_LIMIT, MAX_BYTES);
    }

    /**
     * Creates a spool with given limits (tests use small ones).
     * <p>
     * 정한 한도로 받는 쪽을 만든다(시험은 작은 값을 쓴다).
     *
     * @param memoryLimit the largest body kept in memory / 메모리에 두는 가장 큰 본문
     * @param maxBytes    the largest body accepted / 받아들이는 가장 큰 본문
     */
    public MediaSpool(int memoryLimit, long maxBytes) {
        this.memoryLimit = Math.max(0, memoryLimit);
        this.maxBytes = Math.min(MAX_BYTES, Math.max(0, maxBytes));
    }

    /**
     * Writes one byte.
     * <p>
     * 바이트 하나를 쓴다.
     *
     * @param b the byte / 바이트
     * @throws IOException over the limit or on a disk error / 한도 초과나 디스크 오류 시
     */
    public void write(int b) throws IOException {
        write(new byte[] { (byte) b }, 0, 1);
    }

    /**
     * Writes bytes, moving to the temporary file when the memory threshold is passed.
     * <p>
     * 바이트를 쓴다. 메모리 문턱을 넘으면 임시 파일로 옮긴다.
     *
     * @param b   the bytes / 바이트
     * @param off the start / 시작
     * @param len the number / 수
     * @throws IOException over the limit or on a disk error / 한도 초과나 디스크 오류 시
     */
    public void write(byte[] b, int off, int len) throws IOException {
        if (done) {
            throw new IOException("the media spool is closed");
        }
        if (count + len > maxBytes) {
            throw new IOException("the media resource is larger than " + maxBytes + " bytes");
        }
        // 메모리 문턱을 넘으면 지금까지 받은 것을 임시 파일로 옮긴다.
        if (memory != null && count + len > memoryLimit) {
            file = File.createTempFile(FILE_PREFIX, ".part");
            out = new BufferedOutputStream(new FileOutputStream(file), 64 * 1024);
            memory.writeTo(out);
            memory = null;
        }
        if (memory != null) {
            memory.write(b, off, len);
        } else {
            out.write(b, off, len);
        }
        count += len;
    }

    /**
     * Returns the bytes received so far.
     * <p>
     * 지금까지 받은 바이트 수를 반환한다.
     *
     * @return the count / 수
     */
    public long getCount() {
        return count;
    }

    /**
     * Returns the temporary file, or null while the body is in memory (for tests).
     * <p>
     * 임시 파일을 반환한다. 본문이 메모리에 있으면 null(시험용).
     *
     * @return the file or null / 파일 또는 null
     */
    public File getFile() {
        return file;
    }

    /**
     * Flushes the file stream; the fetch layer calls it at the end of the body.
     * <p>
     * 파일 스트림을 비운다. 가져오기 계층이 본문 끝에서 부른다.
     *
     * @throws IOException on a disk error / 디스크 오류 시
     */
    public void flush() throws IOException {
        if (out != null) {
            out.flush();
        }
    }

    /**
     * Closes the file stream (the fetch layer closes the sink); the received bytes stay until {@link #finish()} or {@link #discard()}.
     * <p>
     * 파일 스트림을 닫는다(가져오기 계층이 받는 쪽을 닫는다). 받은 바이트는 {@link #finish()} 나 {@link #discard()} 까지 남는다.
     *
     * @throws IOException on a disk error / 디스크 오류 시
     */
    public void close() throws IOException {
        OutputStream o = out;
        out = null;
        if (o != null) {
            o.close();
        }
    }

    /**
     * Ends the reception and returns the data; the caller owns it (and must release it).
     * <p>
     * 받기를 끝내고 데이터를 반환한다. 호출자가 소유한다(그리고 놓아야 한다).
     *
     * @return the data / 데이터
     * @throws IOException if the file cannot be completed / 파일을 마무리할 수 없으면
     */
    public MediaData finish() throws IOException {
        if (done) {
            throw new IOException("the media spool is finished");
        }
        close();
        done = true;
        if (memory != null) {
            byte[] b = memory.toByteArray();
            memory = null;
            return MediaData.of(b);
        }
        return MediaData.ofTemporaryFile(file);
    }

    /**
     * Drops what was received and deletes the temporary file (idempotent; nothing happens after {@link #finish()}).
     * <p>
     * 받은 것을 버리고 임시 파일을 지운다(여러 번 불러도 된다. {@link #finish()} 뒤에는 아무 일도 하지 않는다).
     */
    public void discard() {
        if (done) {
            return;
        }
        done = true;
        memory = null;
        try {
            close();
        } catch (IOException e) {
            // 닫기 실패는 무시하고 지우기를 시도한다.
            out = null;
        }
        if (file != null && file.exists() && !file.delete()) {
            // 지울 수 없으면 운영체제의 임시 파일 정리에 맡긴다.
            return;
        }
    }
}
