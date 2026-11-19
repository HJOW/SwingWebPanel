package org.duckdns.hjow.swingwebpanel.media.audio.registry;

import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.BlockPcmSource;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the block source base class: read contracts, errors after partial data, exact seeking over an inexact decoder and release.
 * <p>
 * 블록 소스 기반 클래스 시험: 읽기 규약, 부분 데이터 뒤의 오류, 정확하지 않은 디코더 위에서의 정확한 탐색, 해제.
 */
public class BlockPcmSourceTest {

    /**
     * A mono source that produces the frame index as the sample value, in blocks of ten frames, and can fail at a given block.
     * <p>
     * 프레임 번호를 샘플 값으로 하는 모노 소스. 열 프레임씩 블록으로 만들며 지정한 블록에서 실패할 수 있다.
     */
    private static final class Ramp extends BlockPcmSource {
        /**
         * Total frames.
         * <p>
         * 전체 프레임 수.
         */
        private final int total;

        /**
         * Block number that fails, or -1.
         * <p>
         * 실패하는 블록 번호. 없으면 -1.
         */
        private final int failBlock;

        /**
         * Whether seeking is allowed.
         * <p>
         * 탐색을 허용하는지 여부.
         */
        private final boolean seekable;

        /**
         * Next block.
         * <p>
         * 다음 블록.
         */
        private int block;

        /**
         * Number of times release was called.
         * <p>
         * release 가 호출된 횟수.
         */
        int released;

        /**
         * Creates the source.
         * <p>
         * 소스를 만든다.
         *
         * @param total     total frames / 전체 프레임 수
         * @param failBlock block that fails or -1 / 실패하는 블록 또는 -1
         * @param seekable  whether seeking is supported / 탐색을 지원하는지 여부
         */
        Ramp(int total, int failBlock, boolean seekable) {
            super(8000, 1, total, 10);
            this.total = total;
            this.failBlock = failBlock;
            this.seekable = seekable;
        }

        /**
         * Produces the next ten frames.
         * <p>
         * 다음 열 프레임을 만든다.
         *
         * @param out output / 출력
         * @return bytes or -1 / 바이트 수 또는 -1
         * @throws MediaException at the failing block / 실패하는 블록에서
         */
        protected int fill(byte[] out) throws MediaException {
            if (block == failBlock) {
                throw new MediaException(MediaException.Kind.CORRUPT, "bad block");
            }
            int first = block * 10;
            if (first >= total) {
                return -1;
            }
            int n = Math.min(10, total - first);
            // 프레임 번호를 샘플 값으로 쓴다.
            for (int i = 0; i < n; i++) {
                out[2 * i] = (byte) (first + i);
                out[2 * i + 1] = (byte) ((first + i) >> 8);
            }
            block++;
            return n * 2;
        }

        /**
         * Tells whether seeking is allowed.
         * <p>
         * 탐색을 허용하는지 알려 준다.
         *
         * @return the flag / 플래그
         */
        protected boolean canSeek() {
            return seekable;
        }

        /**
         * Seeks to the start of the block that holds the frame (inexact on purpose).
         * <p>
         * 프레임이 들어 있는 블록의 시작으로 이동한다(일부러 부정확하다).
         *
         * @param frame frame / 프레임
         * @return start of the block / 블록의 시작
         */
        protected long seekTo(long frame) {
            block = (int) (frame / 10);
            return block * 10L;
        }

        /**
         * Counts the releases.
         * <p>
         * 해제 횟수를 센다.
         */
        protected void release() {
            released++;
        }
    }

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public BlockPcmSourceTest() {
    }

    /**
     * Reads the remaining samples as ints.
     * <p>
     * 남은 샘플을 int 로 읽는다.
     *
     * @param s source / 소스
     * @return samples / 샘플
     * @throws MediaException on failure / 실패 시
     */
    private static int[] drain(BlockPcmSource s) throws MediaException {
        byte[] buf = new byte[14];
        int[] all = new int[1000];
        int n = 0;
        // 끝(-1)까지 14바이트(일곱 프레임)씩 읽는다.
        for (;;) {
            int got = s.read(buf, 0, buf.length);
            if (got < 0) {
                break;
            }
            // 읽은 바이트를 두 개씩 묶어 샘플로 만든다.
            for (int i = 0; i + 1 < got; i += 2) {
                all[n++] = (buf[i] & 0xFF) | ((buf[i + 1] & 0xFF) << 8);
            }
        }
        int[] r = new int[n];
        System.arraycopy(all, 0, r, 0, n);
        return r;
    }

    /**
     * Whole frames are delivered in any chunking and the position follows.
     * <p>
     * 어떤 크기로 읽어도 완전한 프레임이 전달되고 위치가 따라온다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testChunkedReads() throws Exception {
        Ramp s = new Ramp(95, -1, true);
        int[] all = drain(s);
        Check.equal(95, all.length, "all frames");
        // 프레임 번호가 그대로 샘플 값인지 확인한다.
        for (int i = 0; i < 95; i++) {
            Check.equal(i, all[i], "frame " + i);
        }
        Check.equal(95, s.getPosition(), "position after the end");
        Check.equal(-1, s.read(new byte[4], 0, 4), "end of stream is sticky");
    }

    /**
     * An error in a later block is reported only after all earlier PCM was delivered, and again by every later read.
     * <p>
     * 뒤쪽 블록의 오류는 앞의 PCM 을 모두 전달한 뒤에 보고되며 이후의 모든 읽기에서 다시 보고된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testErrorAfterPartialData() throws Exception {
        Ramp s = new Ramp(100, 3, true);
        byte[] buf = new byte[1000];
        int got = s.read(buf, 0, 1000);
        Check.equal(60, got, "the 30 frames before the failing block come first");
        // 실패는 이후의 모든 읽기에서 되풀이해 보고된다.
        for (int i = 0; i < 2; i++) {
            try {
                s.read(buf, 0, 1000);
                Check.fail("the failure must be reported");
            } catch (MediaException e) {
                Check.equal(MediaException.Kind.CORRUPT, e.getKind(), "kind");
            }
        }
        s.seekToFrame(0);
        Check.equal(60, s.read(buf, 0, 1000), "a seek clears the failure");
    }

    /**
     * Seeking is exact although the decoder can only resume at block starts, also past the end and back.
     * <p>
     * 디코더가 블록 시작에서만 다시 시작할 수 있어도 탐색은 정확하며 끝을 넘거나 되돌아가도 그렇다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testExactSeek() throws Exception {
        Ramp s = new Ramp(95, -1, true);
        long[] targets = { 0, 1, 9, 10, 11, 57, 94, 95, 200, 3 };
        // 위치마다 이동한 뒤 첫 프레임이 그 위치의 값인지 확인한다.
        for (long t : targets) {
            s.seekToFrame(t);
            int[] rest = drain(s);
            long clamped = Math.min(t, 95);
            Check.equal(95 - clamped, rest.length, "remaining frames after " + t);
            if (rest.length > 0) {
                Check.equal(clamped, rest[0], "first frame after " + t);
            }
        }
        Check.equal(true, s.isSeekable(), "seekable");
    }

    /**
     * A source that cannot seek reports UNSUPPORTED_FORMAT, and close releases exactly once and makes reads and seeks fail.
     * <p>
     * 탐색할 수 없는 소스는 UNSUPPORTED_FORMAT 을 보고하고, close 는 정확히 한 번만 해제하며 이후의 읽기와 탐색을 실패하게 한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNoSeekAndClose() throws Exception {
        Ramp s = new Ramp(20, -1, false);
        Check.equal(false, s.isSeekable(), "not seekable");
        try {
            s.seekToFrame(5);
            Check.fail("seeking must fail");
        } catch (MediaException e) {
            Check.equal(MediaException.Kind.UNSUPPORTED_FORMAT, e.getKind(), "kind");
        }
        s.close();
        s.close();
        Check.equal(1, s.released, "release is called once");
        try {
            s.read(new byte[4], 0, 4);
            Check.fail("reading after close must fail");
        } catch (MediaException e) {
            Check.equal(MediaException.Kind.ACCESS_FAILED, e.getKind(), "read after close");
        }
        try {
            s.seekToFrame(0);
            Check.fail("seeking after close must fail");
        } catch (MediaException e) {
            Check.equal(MediaException.Kind.ACCESS_FAILED, e.getKind(), "seek after close");
        }
    }
}
