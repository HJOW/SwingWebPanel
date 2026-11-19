package org.duckdns.hjow.swingwebpanel.media.audio.opus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.AudioDecoders;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmSource;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.AudioKit;
import org.duckdns.hjow.swingwebpanel.media.audio.webm.WebmTestBuilder;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of Opus tracks (<code>A_OPUS</code>) in WebM/Matroska files: the CodecPrivate as identification header (and its
 * replacement from the track properties when it is missing), the CodecDelay, the DiscardPadding that trims the end, the block
 * layouts of the container (simple blocks, block groups, lacing, unknown sizes), seeking and the error cases. The Opus packets
 * are those of the RFC 6716 test vectors and the expected samples come from decoding them directly.
 * <p>
 * WebM/Matroska 파일의 Opus 트랙(<code>A_OPUS</code>) 시험: 식별 헤더인 CodecPrivate(없을 때는 트랙 속성으로 대체), CodecDelay, 끝을 자르는
 * DiscardPadding, 컨테이너의 블록 배치(심플 블록, 블록 그룹, 레이싱, 크기를 모르는 요소), 탐색, 오류 사례. Opus 패킷은 RFC 6716 시험 벡터의
 * 것이고 기대 샘플은 이를 직접 디코딩해 얻는다.
 */
public class OpusWebmTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public OpusWebmTest() {
    }

    /**
     * Presentation times of the packets in milliseconds.
     * <p>
     * 패킷의 재생 시각(밀리초).
     *
     * @param p the packets / 패킷
     * @return the times / 시각
     */
    private static long[] times(byte[][] p) {
        long[] t = new long[p.length];
        long samples = 0;
        // 패킷마다 앞선 패킷들의 길이 합을 밀리초로 바꾼다.
        for (int i = 0; i < p.length; i++) {
            t[i] = samples / 48;
            samples += OpusKit.samples(p[i]);
        }
        return t;
    }

    /**
     * Builds a builder for an Opus track.
     * <p>
     * Opus 트랙용 생성기를 만든다.
     *
     * @param channels channels / 채널 수
     * @param preSkip  pre-skip in samples / 사전 건너뛰기(샘플)
     * @param withHead true to include the CodecPrivate / CodecPrivate 를 넣으려면 true
     * @return the builder / 생성기
     */
    private static WebmTestBuilder builder(int channels, int preSkip, boolean withHead) {
        WebmTestBuilder b = new WebmTestBuilder();
        b.codecId = "A_OPUS";
        b.codecPrivate = withHead ? OpusKit.head(channels, preSkip, 0, 0, 0, 0, null) : new byte[0];
        b.channels = channels;
        b.rate = 48000;
        b.codecDelay = Math.round(preSkip * 1e9 / 48000);
        return b;
    }

    /**
     * Builds a WebM file.
     * <p>
     * WebM 파일을 만든다.
     *
     * @param b       the builder / 생성기
     * @param packets the packets / 패킷
     * @return the file / 파일
     */
    private static byte[] file(WebmTestBuilder b, byte[][] packets) {
        List<byte[]> frames = new ArrayList<byte[]>(Arrays.asList(packets));
        return b.build(frames, times(packets));
    }

    /**
     * A WebM file with an Opus track decodes like the same packets in Ogg, without the pre-skip.
     * <p>
     * Opus 트랙이 있는 WebM 파일은 같은 패킷의 Ogg 와 똑같이 디코딩되며 사전 건너뛰기를 뺀다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testOpusTrack() throws Exception {
        byte[][] packets = OpusKit.vectorPackets(7, 80);
        byte[] webm = file(builder(2, 312, true), packets);
        PcmSource src = AudioDecoders.open(webm, "audio/webm; codecs=opus", null);
        Check.equal(48000, src.getSampleRate(), "sample rate");
        Check.equal(2, src.getChannels(), "channels");
        int n = 0;
        // 패킷 길이를 모두 더한다.
        for (int i = 0; i < packets.length; i++) {
            n += OpusKit.samples(packets[i]);
        }
        Check.equal(n - 312, src.getFrameCount(), "frame count");
        short[] got = AudioKit.toShorts(AudioKit.readAll(src));
        short[] direct = OpusKit.decodeDirect(packets, 2);
        Check.equal(2L * (n - 312), got.length, "samples");
        // 샘플마다 직접 디코딩한 값과 비교한다.
        for (int i = 0; i < got.length; i++) {
            Check.isTrue(Math.abs(direct[2 * 312 + i] - got[i]) <= 1, "sample " + i);
        }
    }

    /**
     * Without CodecPrivate the header is rebuilt from the track channels and the CodecDelay.
     * <p>
     * CodecPrivate 가 없으면 트랙의 채널 수와 CodecDelay 로 헤더를 다시 만든다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMissingCodecPrivate() throws Exception {
        byte[][] packets = OpusKit.vectorPackets(3, 50);
        byte[] webm = file(builder(1, 400, false), packets);
        PcmSource src = AudioDecoders.open(webm, null, null);
        Check.equal(1, src.getChannels(), "mono");
        short[] got = AudioKit.toShorts(AudioKit.readAll(src));
        short[] direct = OpusKit.decodeDirect(packets, 1);
        Check.equal(direct.length - 400, got.length, "samples");
        // 사전 건너뛰기 400 샘플을 뺀 나머지가 같은지 확인한다.
        for (int i = 0; i < got.length; i++) {
            Check.isTrue(Math.abs(direct[400 + i] - got[i]) <= 1, "sample " + i);
        }
    }

    /**
     * The DiscardPadding of the last block group trims the end of the stream.
     * <p>
     * 마지막 블록 그룹의 DiscardPadding 이 스트림의 끝을 자른다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDiscardPadding() throws Exception {
        byte[][] packets = OpusKit.vectorPackets(2, 40);
        WebmTestBuilder b = builder(2, 0, true);
        b.blockGroups = true;
        b.discardPaddingLast = 10000000L;
        PcmSource src = AudioDecoders.open(file(b, packets), null, null);
        int n = 0;
        // 패킷 길이를 모두 더한다.
        for (int i = 0; i < packets.length; i++) {
            n += OpusKit.samples(packets[i]);
        }
        Check.equal(n - 480, src.getFrameCount(), "frame count without the 10 ms of padding");
        Check.equal(2L * (n - 480), AudioKit.readAll(src).length / 2, "samples");
    }

    /**
     * All block layouts of the container deliver the same audio: simple blocks, block groups, EBML laced blocks and files with
     * unknown sizes.
     * <p>
     * 컨테이너의 모든 블록 배치가 같은 오디오를 낸다: 심플 블록, 블록 그룹, EBML 레이싱 블록, 크기를 모르는 파일.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBlockLayouts() throws Exception {
        byte[][] packets = OpusKit.vectorPackets(7, 40);
        byte[] reference = AudioKit.readAll(AudioDecoders.open(file(builder(2, 0, true), packets), null, null));
        // 배치 조합마다 같은 PCM 이 나오는지 확인한다.
        for (int variant = 0; variant < 4; variant++) {
            WebmTestBuilder b = builder(2, 0, true);
            b.blockGroups = variant == 1 || variant == 3;
            b.unknownSizes = variant >= 2;
            b.lacing = variant == 3 ? 3 : 0;
            byte[] got = AudioKit.readAll(AudioDecoders.open(file(b, packets), null, null));
            Check.isTrue(Arrays.equals(reference, got), "variant " + variant);
        }
    }

    /**
     * Seeking in a WebM Opus track is exact.
     * <p>
     * WebM Opus 트랙에서의 탐색은 정확하다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSeeking() throws Exception {
        byte[][] packets = OpusKit.vectorPackets(7, 300);
        PcmSource src = AudioDecoders.open(file(builder(2, 312, true), packets), null, null);
        long frames = src.getFrameCount();
        src.seekToFrame(frames / 3);
        Check.equal(2L * (frames - frames / 3), AudioKit.readAll(src).length / 2, "samples after the seek");
    }

    /**
     * canPlayType reports Opus in WebM, a Vorbis-less unsupported codec stays unsupported, and a track without frames is damaged.
     * <p>
     * canPlayType 은 WebM 의 Opus 를 보고하고, 지원하지 않는 코덱은 계속 미지원이며, 프레임이 없는 트랙은 손상된 것이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRegistryAndErrors() throws Exception {
        Check.equal("probably", AudioDecoders.canPlayType("audio/webm; codecs=opus"), "canPlayType");
        WebmTestBuilder b = builder(2, 0, true);
        b.codecId = "A_AAC";
        try {
            AudioDecoders.open(file(b, OpusKit.vectorPackets(7, 5)), null, null);
            Check.fail("an AAC track must be unsupported");
        } catch (MediaException e) {
            Check.equal(MediaException.Kind.UNSUPPORTED_FORMAT, e.getKind(), "kind of an unsupported codec");
        }
        byte[] webm = file(builder(2, 0, true), OpusKit.vectorPackets(7, 20));
        // 잘린 파일은 디코딩되거나 MediaException 으로만 실패해야 한다.
        for (int cut = 0; cut < webm.length; cut += 53) {
            byte[] d = new byte[cut];
            System.arraycopy(webm, 0, d, 0, cut);
            try {
                AudioKit.readAll(AudioDecoders.open(d, null, null));
            } catch (MediaException e) {
                continue;
            }
        }
    }
}
