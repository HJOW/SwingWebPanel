package org.duckdns.hjow.swingwebpanel.media.playback;

import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.AudioDecoder;
import org.duckdns.hjow.swingwebpanel.media.audio.AudioDecoders;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmSource;

/**
 * A decoder for tests whose behaviour is scripted by its data (plan items P18-08, P18-10): it produces a deterministic 8 kHz mono ramp so
 * that the player can be tested with a source that does not know its length and cannot seek, and with a source that fails in the middle. The
 * data are the ASCII bytes <code>SCRIPT</code>, a mode character and the number of frames as a big-endian 32-bit integer: mode
 * <code>U</code> is an unknown length and not seekable, <code>K</code> a known length and seekable, <code>C</code> fails with CORRUPT after
 * half of the frames (known length, seekable). The decoder is registered once in {@link AudioDecoders}.
 * <p>
 * 데이터가 동작을 정하는 시험용 디코더(계획 P18-08, P18-10): 결정적인 8 kHz 모노 램프를 만들어, 길이를 모르고 탐색할 수 없는 소스와 도중에 실패하는
 * 소스로 재생기를 시험할 수 있게 한다. 데이터는 ASCII <code>SCRIPT</code>, 모드 문자 하나, 빅 엔디언 32비트 정수의 프레임 수이다. 모드
 * <code>U</code> 는 길이를 모르고 탐색 불가, <code>K</code> 는 길이를 알고 탐색 가능, <code>C</code> 는 프레임의 절반 뒤에 CORRUPT 로 실패한다(길이를
 * 알고 탐색 가능). 디코더는 {@link AudioDecoders} 에 한 번 등록한다.
 */
public final class ScriptedDecoder implements AudioDecoder {

    /**
     * The sample rate of every scripted source.
     * <p>
     * 모든 스크립트 소스의 샘플링 주파수.
     */
    public static final int RATE = 8000;

    /**
     * True once the decoder was added to the registry.
     * <p>
     * 디코더를 등록부에 넣었으면 true.
     */
    private static boolean registered;

    /**
     * Private constructor: use {@link #install()}.
     * <p>
     * 비공개 생성자: {@link #install()} 을 쓴다.
     */
    private ScriptedDecoder() {
    }

    /**
     * Registers the decoder once.
     * <p>
     * 디코더를 한 번 등록한다.
     */
    public static synchronized void install() {
        if (!registered) {
            registered = true;
            AudioDecoders.register(new ScriptedDecoder());
        }
    }

    /**
     * Returns the sample number i of the ramp.
     * <p>
     * 램프의 i 번째 샘플을 반환한다.
     *
     * @param i the frame number / 프레임 번호
     * @return the sample / 샘플
     */
    public static short sample(long i) {
        return (short) ((i * 13) % 20000 - 10000);
    }

    /**
     * Builds the data of a scripted source.
     * <p>
     * 스크립트 소스의 데이터를 만든다.
     *
     * @param mode   <code>U</code>, <code>K</code> or <code>C</code> / <code>U</code>, <code>K</code>, <code>C</code>
     * @param frames the number of frames / 프레임 수
     * @return the data / 데이터
     */
    public static byte[] data(char mode, int frames) {
        byte[] d = new byte[11];
        byte[] magic = { 'S', 'C', 'R', 'I', 'P', 'T' };
        System.arraycopy(magic, 0, d, 0, 6);
        d[6] = (byte) mode;
        d[7] = (byte) (frames >> 24);
        d[8] = (byte) (frames >> 16);
        d[9] = (byte) (frames >> 8);
        d[10] = (byte) frames;
        return d;
    }

    /**
     * Returns the expected PCM bytes of a range of frames.
     * <p>
     * 프레임 범위의 기대 PCM 바이트를 반환한다.
     *
     * @param from the first frame / 첫 프레임
     * @param to   the frame after the last / 마지막 다음 프레임
     * @return the 16-bit little-endian PCM / 16비트 리틀 엔디언 PCM
     */
    public static byte[] pcm(long from, long to) {
        byte[] r = new byte[(int) (to - from) * 2];
        // 프레임마다 두 바이트로 푼다.
        for (long i = from; i < to; i++) {
            short s = sample(i);
            r[(int) (i - from) * 2] = (byte) s;
            r[(int) (i - from) * 2 + 1] = (byte) (s >> 8);
        }
        return r;
    }

    /**
     * Returns the format name.
     * <p>
     * 형식 이름을 반환한다.
     *
     * @return the name / 이름
     */
    public String getFormatName() {
        return "script";
    }

    /**
     * Returns the MIME type.
     * <p>
     * MIME 타입을 반환한다.
     *
     * @return the types / 타입
     */
    public String[] getMimeTypes() {
        return new String[] { "audio/x-script" };
    }

    /**
     * Tells whether the data start with the magic bytes.
     * <p>
     * 데이터가 매직 바이트로 시작하는지 알려 준다.
     *
     * @param d the data / 데이터
     * @return true if it matches / 맞으면 true
     */
    public boolean accepts(byte[] d) {
        return d.length >= 11 && d[0] == 'S' && d[1] == 'C' && d[2] == 'R' && d[3] == 'I' && d[4] == 'P' && d[5] == 'T';
    }

    /**
     * Opens a scripted source.
     * <p>
     * 스크립트 소스를 연다.
     *
     * @param d      the data / 데이터
     * @param limits the limits / 제한
     * @return the source / 소스
     */
    public PcmSource open(byte[] d, DecodeLimits limits) {
        final char mode = (char) d[6];
        final long frames = ((d[7] & 0xffL) << 24) | ((d[8] & 0xffL) << 16) | ((d[9] & 0xffL) << 8) | (d[10] & 0xffL);
        return new PcmSource() {
            /** The next frame. / 다음 프레임. */
            private long pos;

            /**
             * Returns the sample rate.
             * <p>
             * 샘플링 주파수를 반환한다.
             *
             * @return the rate / 주파수
             */
            public int getSampleRate() {
                return RATE;
            }

            /**
             * Returns 1.
             * <p>
             * 1 을 반환한다.
             *
             * @return the channels / 채널 수
             */
            public int getChannels() {
                return 1;
            }

            /**
             * Returns the length, -1 for mode U.
             * <p>
             * 길이를 반환한다. 모드 U 는 -1.
             *
             * @return the frames / 프레임 수
             */
            public long getFrameCount() {
                return mode == 'U' ? -1 : frames;
            }

            /**
             * Produces the next samples; mode C fails halfway.
             * <p>
             * 다음 샘플을 만든다. 모드 C 는 중간에 실패한다.
             *
             * @param b      the buffer / 버퍼
             * @param offset the start / 시작
             * @param length the bytes / 바이트 수
             * @return the bytes or -1 / 바이트 수 또는 -1
             * @throws MediaException CORRUPT in mode C / 모드 C 에서 CORRUPT
             */
            public int read(byte[] b, int offset, int length) throws MediaException {
                if (mode == 'C' && pos >= frames / 2) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "scripted damage");
                }
                long limit = mode == 'C' ? frames / 2 : frames;
                long n = Math.min(length / 2, limit - pos);
                if (n <= 0) {
                    return -1;
                }
                // 프레임마다 램프 샘플을 쓴다.
                for (long i = 0; i < n; i++) {
                    short s = sample(pos + i);
                    b[offset + (int) i * 2] = (byte) s;
                    b[offset + (int) i * 2 + 1] = (byte) (s >> 8);
                }
                pos += n;
                return (int) n * 2;
            }

            /**
             * Tells whether seeking works.
             * <p>
             * 탐색이 되는지 알려 준다.
             *
             * @return false for mode U / 모드 U 는 false
             */
            public boolean isSeekable() {
                return mode != 'U';
            }

            /**
             * Moves to a frame.
             * <p>
             * 프레임으로 옮긴다.
             *
             * @param frame the frame / 프레임
             * @throws MediaException if not seekable / 탐색할 수 없으면
             */
            public void seekToFrame(long frame) throws MediaException {
                if (mode == 'U') {
                    throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "not seekable");
                }
                pos = Math.min(frame, frames);
            }

            /**
             * Does nothing.
             * <p>
             * 아무것도 하지 않는다.
             */
            public void close() {
                return;
            }
        };
    }
}
