package org.duckdns.hjow.swingwebpanel.media.audio.spool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;
import org.duckdns.hjow.swingwebpanel.media.audio.ArrayByteSource;
import org.duckdns.hjow.swingwebpanel.media.audio.ByteSource;
import org.duckdns.hjow.swingwebpanel.media.audio.ByteSourceException;
import org.duckdns.hjow.swingwebpanel.media.audio.Bytes;
import org.duckdns.hjow.swingwebpanel.media.audio.FileByteSource;
import org.duckdns.hjow.swingwebpanel.media.audio.MediaData;
import org.duckdns.hjow.swingwebpanel.media.audio.MediaSpool;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the random-access byte sources of the audio pipeline (plan item P18-08): the array and the file source return the same bytes for
 * every kind of read (also across many window borders), positions outside fail like an array, a closed file fails with
 * {@link ByteSourceException}; the spool keeps a short body in memory and moves a long one to a temporary file, refuses a body over its limit
 * and deletes a discarded file; the media data closes its open readers and deletes its file when released.
 * <p>
 * 오디오 파이프라인의 임의 접근 바이트 원천 시험(계획 P18-08): 배열 원천과 파일 원천은 모든 종류의 읽기에서(창 경계를 많이 넘어도) 같은 바이트를
 * 돌려주고, 밖의 위치는 배열처럼 실패하며, 닫힌 파일은 {@link ByteSourceException} 으로 실패한다. 받는 쪽은 짧은 본문을 메모리에 두고 긴 본문을 임시
 * 파일로 옮기며, 한도를 넘는 본문을 거절하고 버린 파일을 지운다. 미디어 데이터는 놓을 때 열린 원천을 닫고 파일을 지운다.
 */
public class ByteSourceTest {

    /**
     * Makes deterministic bytes.
     * <p>
     * 결정적인 바이트를 만든다.
     *
     * @param n    the length / 길이
     * @param seed the seed / 씨앗
     * @return the bytes / 바이트
     */
    private static byte[] bytes(int n, long seed) {
        byte[] b = new byte[n];
        new Random(seed).nextBytes(b);
        return b;
    }

    /**
     * Writes bytes to a new temporary file.
     * <p>
     * 바이트를 새 임시 파일에 쓴다.
     *
     * @param b the bytes / 바이트
     * @return the file / 파일
     * @throws IOException on failure / 실패 시
     */
    private static File temp(byte[] b) throws IOException {
        File f = File.createTempFile("bytesourcetest", ".bin");
        FileOutputStream out = new FileOutputStream(f);
        try {
            out.write(b);
        } finally {
            out.close();
        }
        return f;
    }

    /**
     * Single bytes, reads of every size at random places and the helpers of {@link Bytes} give the same results for an array and for a
     * file with a tiny window (7 bytes), a medium one and the default one.
     * <p>
     * 바이트 하나, 무작위 위치의 여러 크기 읽기, {@link Bytes} 의 도우미가 배열과 아주 작은 창(7바이트), 중간 창, 기본 창의 파일에서 같은 결과를 낸다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFileMatchesArray() throws Exception {
        byte[] data = bytes(200003, 1);
        File f = temp(data);
        try {
            ByteSource a = new ArrayByteSource(data);
            int[] windows = { 7, 4096, FileByteSource.DEFAULT_WINDOW };
            // 창 크기마다 같은 질문을 던져 배열과 비교한다.
            for (int w : windows) {
                FileByteSource s = new FileByteSource(f, w);
                try {
                    Check.equal(a.length(), s.length(), "length");
                    Random r = new Random(w);
                    // 무작위 위치의 바이트 하나와 정수 도우미를 비교한다.
                    for (int i = 0; i < 3000; i++) {
                        long p = r.nextInt(data.length - 8);
                        Check.equal(a.get(p), s.get(p), "byte at " + p);
                        Check.equal(Bytes.le32(a, p), Bytes.le32(s, p), "le32 at " + p);
                        Check.equal(Bytes.be32(a, p), Bytes.be32(s, p), "be32 at " + p);
                        Check.equal(Bytes.le64(a, p), Bytes.le64(s, p), "le64 at " + p);
                    }
                    // 무작위 위치와 크기(창보다 큰 것 포함)의 읽기를 비교한다.
                    for (int i = 0; i < 400; i++) {
                        long p = r.nextInt(data.length);
                        int len = r.nextInt(i % 5 == 0 ? 150000 : 300);
                        byte[] x = new byte[len + 3];
                        byte[] y = new byte[len + 3];
                        int na = a.read(p, x, 3, len);
                        int nb = s.read(p, y, 3, len);
                        Check.equal(na, nb, "count at " + p);
                        Check.isTrue(java.util.Arrays.equals(x, y), "bytes at " + p + "+" + len);
                    }
                    // 뒤에서 앞으로 한 바이트씩 읽어도 같다.
                    for (long p = data.length - 1; p >= data.length - 5000; p--) {
                        Check.equal(data[(int) p] & 0xFF, s.get(p), "backwards " + p);
                    }
                    Check.equal(0, s.read(data.length, new byte[4], 0, 4), "nothing at the end");
                    Check.equal(2, s.read(data.length - 2, new byte[4], 0, 4), "a short read only at the end");
                } finally {
                    s.close();
                }
            }
        } finally {
            Check.isTrue(f.delete(), "the closed file can be deleted");
        }
    }

    /**
     * Positions outside fail like an array access; a closed file source fails with {@link ByteSourceException}, which becomes an
     * <code>ACCESS_FAILED</code> media failure.
     * <p>
     * 밖의 위치는 배열 접근처럼 실패하고, 닫힌 파일 원천은 {@link ByteSourceException} 으로 실패하며 이것은 <code>ACCESS_FAILED</code> 미디어 실패가 된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBoundsAndClosedFile() throws Exception {
        final ByteSource a = new ArrayByteSource(new byte[] { 1, 2, 3 });
        Check.throwsException(IndexOutOfBoundsException.class, new Block() {
            /** Reads past the end. / 끝 너머를 읽는다. */
            public void run() throws Exception {
                a.get(3);
            }
        }, "past the end of an array");
        Check.throwsException(IndexOutOfBoundsException.class, new Block() {
            /** Reads before the start. / 처음 앞을 읽는다. */
            public void run() throws Exception {
                a.get(-1);
            }
        }, "before the start of an array");
        File f = temp(new byte[] { 9, 8, 7 });
        final FileByteSource s = new FileByteSource(f, 2);
        Check.equal(7, s.get(2), "last byte");
        Check.throwsException(IndexOutOfBoundsException.class, new Block() {
            /** Reads past the end. / 끝 너머를 읽는다. */
            public void run() throws Exception {
                s.get(3);
            }
        }, "past the end of a file");
        s.close();
        s.close();
        Throwable t = Check.throwsException(ByteSourceException.class, new Block() {
            /** Reads a closed file. / 닫힌 파일을 읽는다. */
            public void run() throws Exception {
                s.get(0);
            }
        }, "a closed file");
        Check.equal("ACCESS_FAILED", ((ByteSourceException) t).toMediaException().getKind().name(), "the media failure kind");
        Check.isTrue(f.delete(), "deleted");
    }

    /**
     * The spool keeps a body up to its memory limit in memory, moves a longer body (written in pieces) to a temporary file with the same
     * bytes, refuses a body over its maximum with an <code>IOException</code>, and deletes the partial file when discarded.
     * <p>
     * 받는 쪽은 메모리 한도까지의 본문을 메모리에 두고, 더 긴 본문(조각으로 씀)을 같은 바이트로 임시 파일에 옮기며, 최대를 넘는 본문을
     * <code>IOException</code> 으로 거절하고, 버리면 덜 받은 파일을 지운다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSpoolMemoryFileAndLimit() throws Exception {
        byte[] small = bytes(1000, 2);
        MediaSpool m = new MediaSpool(1000, 5000);
        m.write(small, 0, small.length);
        Check.isNull(m.getFile(), "a body at the memory limit stays in memory");
        MediaData md = m.finish();
        Check.isNull(md.getFile(), "memory data");
        Check.equal(1000L, md.length(), "length");
        md.release();
        byte[] big = bytes(4999, 3);
        MediaSpool s = new MediaSpool(1000, 5000);
        // 크기가 다른 조각으로 나누어 쓴다.
        for (int off = 0; off < big.length;) {
            int n = Math.min(big.length - off, 1 + off % 777);
            s.write(big, off, n);
            off += n;
        }
        File file = s.getFile();
        Check.notNull(file, "a longer body went to a file");
        Check.isTrue(file.getName().startsWith("swingwebpanel-media-"), "the file name: " + file.getName());
        s.close();
        MediaData fd = s.finish();
        Check.same(file, fd.getFile(), "the data owns the file");
        ByteSource src = fd.open();
        byte[] back = new byte[big.length];
        Check.equal(big.length, src.read(0, back, 0, back.length), "everything");
        Check.isTrue(java.util.Arrays.equals(big, back), "the same bytes");
        fd.release();
        Check.isFalse(file.exists(), "release deletes the file, also with an open reader");
        final MediaSpool over = new MediaSpool(10, 100);
        over.write(new byte[90], 0, 90);
        File overFile = over.getFile();
        Check.throwsException(IOException.class, new Block() {
            /** Writes past the limit. / 한도를 넘게 쓴다. */
            public void run() throws Exception {
                over.write(new byte[20], 0, 20);
            }
        }, "a body over the limit");
        over.discard();
        Check.isFalse(overFile.exists(), "discard deletes the partial file");
        Check.equal((long) Integer.MAX_VALUE, MediaSpool.MAX_BYTES, "the disk limit keeps every position in an int");
    }

    /**
     * Released media data closes every reader that is still open (their reads fail afterwards), deletes the file, refuses new readers and
     * may be released again; a reader closed by its user is forgotten.
     * <p>
     * 놓은 미디어 데이터는 아직 열린 모든 원천을 닫고(그 뒤 읽기는 실패) 파일을 지우며 새 원천을 거절하고 다시 놓아도 된다. 사용자가 닫은 원천은 잊는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testReleaseClosesReadersAndDeletes() throws Exception {
        File f = temp(bytes(70000, 4));
        final MediaData d = MediaData.ofTemporaryFile(f);
        final ByteSource one = d.open();
        ByteSource two = d.open();
        two.close();
        Check.equal(70000L, one.length(), "length");
        one.get(65000);
        d.release();
        Check.isTrue(d.isReleased(), "released");
        Check.isFalse(f.exists(), "the file is gone");
        Check.throwsException(ByteSourceException.class, new Block() {
            /** Reads after the release. / 놓은 뒤 읽는다. */
            public void run() throws Exception {
                one.get(1);
            }
        }, "a reader of released data");
        Check.throwsException(IOException.class, new Block() {
            /** Opens after the release. / 놓은 뒤 연다. */
            public void run() throws Exception {
                d.open();
            }
        }, "no new reader");
        d.release();
    }
}
