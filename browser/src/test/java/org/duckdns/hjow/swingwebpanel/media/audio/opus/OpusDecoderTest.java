package org.duckdns.hjow.swingwebpanel.media.audio.opus;

import java.util.Random;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.AudioDecoders;
import org.duckdns.hjow.swingwebpanel.media.audio.ChannelMixer;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmConverter;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmSource;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.AudioKit;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.OggMux;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of Ogg Opus decoding through the decoder registry (RFC 7845): the detection by the OpusHead signature and not by a MIME
 * type, the pre-skip, the trimming of the last page, the output gain, mono and stereo, the channel mapping family 1 with several
 * streams and the mixdown to stereo, chained streams, seeking, damaged packets and files, header errors, cancellation and the size
 * limit. The audio payload is the set of Opus packets of the RFC 6716 test vectors, packaged by the test-only Ogg writer of
 * {@link OpusKit}, and the expected samples come from decoding the same packets directly with the frame decoder, which the vector
 * tests have checked against the reference decoder.
 * <p>
 * 디코더 등록부를 통한 Ogg Opus 디코딩 시험(RFC 7845): MIME 타입이 아니라 OpusHead 시그니처로 알아보기, 사전 건너뛰기, 마지막 페이지 자르기,
 * 출력 이득, 모노와 스테레오, 스트림이 여럿인 채널 매핑 군 1 과 스테레오 다운믹스, 체인 스트림, 탐색, 손상된 패킷과 파일, 헤더 오류, 취소,
 * 크기 제한. 오디오 페이로드는 RFC 6716 시험 벡터의 Opus 패킷이며 {@link OpusKit} 의 시험 전용 Ogg 작성기로 포장한다. 기대 샘플은 같은 패킷을
 * 프레임 디코더로 직접 디코딩해 얻으며 이 디코더는 벡터 시험에서 참조 디코더와 대조되었다.
 */
public class OpusDecoderTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public OpusDecoderTest() {
    }

    /**
     * Total number of samples of packets.
     * <p>
     * 패킷들의 샘플 총수.
     *
     * @param p the packets / 패킷
     * @return the samples per channel / 채널당 샘플 수
     */
    private static int total(byte[][] p) {
        int n = 0;
        // 패킷마다 길이를 더한다.
        for (int i = 0; i < p.length; i++) {
            n += OpusKit.samples(p[i]);
        }
        return n;
    }

    /**
     * Compares samples with a tolerance and reports the first difference.
     * <p>
     * 허용 오차를 두고 샘플을 비교하며 첫 차이를 보고한다.
     *
     * @param exp   expected samples / 기대 샘플
     * @param expOff first expected index / 첫 기대 위치
     * @param got   actual samples / 실제 샘플
     * @param count number of samples to compare / 비교할 샘플 수
     * @param tol   allowed difference / 허용 차이
     * @param what  description / 설명
     */
    private static void same(short[] exp, int expOff, short[] got, int count, int tol, String what) {
        // 샘플마다 차이를 확인한다.
        for (int i = 0; i < count; i++) {
            int d = Math.abs(exp[expOff + i] - got[i]);
            if (d > tol) {
                Check.fail(what + ": sample " + i + " expected " + exp[expOff + i] + " but was " + got[i]);
            }
        }
    }

    /**
     * A plain stereo file decodes to the direct decoding minus the pre-skip, with the frame count of the granule position.
     * <p>
     * 일반 스테레오 파일은 직접 디코딩에서 사전 건너뛰기를 뺀 것으로 디코딩되며 프레임 수는 그래뉼 위치에서 나온다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testStereoWithPreSkip() throws Exception {
        byte[][] packets = OpusKit.vectorPackets(7, 80);
        byte[] data = OpusKit.oggFile(1, OpusKit.head(2, 312, 0, 0, 0, 0, null), packets, 255, -1, true);
        PcmSource src = AudioDecoders.open(data, "audio/ogg; codecs=opus", null);
        Check.equal(48000, src.getSampleRate(), "sample rate");
        Check.equal(2, src.getChannels(), "channels");
        int n = total(packets);
        Check.equal(n - 312, src.getFrameCount(), "frame count = last granule position - pre-skip");
        short[] got = AudioKit.toShorts(AudioKit.readAll(src));
        Check.equal(2L * (n - 312), got.length, "number of samples");
        same(OpusKit.decodeDirect(packets, 2), 312 * 2, got, got.length, 1, "decoded samples");
    }

    /**
     * The decoder is chosen by the OpusHead signature: a wrong MIME type hint changes nothing, and Vorbis files do not go to it.
     * <p>
     * 디코더는 OpusHead 시그니처로 정해진다: 잘못된 MIME 힌트는 영향이 없고 Vorbis 파일은 이 디코더로 가지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDetectionBySignature() throws Exception {
        byte[][] packets = OpusKit.vectorPackets(7, 10);
        byte[] data = OpusKit.oggFile(1, OpusKit.head(2, 0, 0, 0, 0, 0, null), packets, 255, -1, true);
        int expected = total(packets);
        String[] hints = { null, "audio/wav", "audio/mpeg", "audio/ogg", "audio/opus", "audio/ogg; codecs=opus", "video/webm" };
        // 힌트마다 같은 길이로 디코딩되는지 확인한다.
        for (String hint : hints) {
            PcmSource s = AudioDecoders.open(data, hint, null);
            Check.equal(expected, AudioKit.readAll(s).length / 4, "frames with hint " + hint);
        }
        Check.isTrue(new OpusDecoder().accepts(data), "accepts");
        Check.isFalse(new OpusDecoder().accepts("OggS not really".getBytes("US-ASCII")), "garbage");
        Check.isFalse(new OpusDecoder().accepts(new byte[100]), "zeros");
        Check.equal("probably", AudioDecoders.canPlayType("audio/ogg; codecs=opus"), "canPlayType ogg opus");
        Check.equal("probably", AudioDecoders.canPlayType("audio/opus; codecs=opus"), "canPlayType opus");
        Check.isTrue(AudioDecoders.isSupportedMime("audio/opus"), "audio/opus is supported");
    }

    /**
     * The end of the stream is trimmed to the granule position of its last page.
     * <p>
     * 스트림의 끝은 마지막 페이지의 그래뉼 위치에 맞춰 잘린다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testEndTrimming() throws Exception {
        byte[][] packets = OpusKit.vectorPackets(2, 70);
        int n = total(packets);
        long last = n - 700;
        byte[] data = OpusKit.oggFile(1, OpusKit.head(2, 100, 0, 0, 0, 0, null), packets, 255, last, true);
        PcmSource src = AudioDecoders.open(data, null, null);
        Check.equal(last - 100, src.getFrameCount(), "frame count");
        short[] got = AudioKit.toShorts(AudioKit.readAll(src));
        Check.equal(2 * (last - 100), got.length, "samples delivered");
        same(OpusKit.decodeDirect(packets, 2), 200, got, got.length, 1, "trimmed stream");
    }

    /**
     * The output gain of the header scales the samples.
     * <p>
     * 헤더의 출력 이득이 샘플의 크기를 조정한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testOutputGain() throws Exception {
        byte[][] packets = OpusKit.vectorPackets(11, 20);
        short[] direct = OpusKit.decodeDirect(packets, 2);
        // 이득 -6.02 dB(= -1541/256 dB)는 진폭을 절반으로 줄인다.
        byte[] data = OpusKit.oggFile(1, OpusKit.head(2, 0, -1541, 0, 0, 0, null), packets, 255, -1, true);
        short[] got = AudioKit.toShorts(AudioKit.readAll(AudioDecoders.open(data, null, null)));
        Check.equal(direct.length, got.length, "length");
        // 이득을 곱한 값과 반올림 오차 안에서 같은지 확인한다.
        double g = Math.pow(10.0, -1541 / (20.0 * 256.0));
        for (int i = 0; i < got.length; i++) {
            int e = (int) Math.round(direct[i] * g);
            Check.isTrue(Math.abs(e - got[i]) <= 2, "sample " + i + " expected about " + e + " but was " + got[i]);
        }
        // 이득 +10 dB 는 크기를 키우되 16비트 범위로 제한한다.
        byte[] loud = OpusKit.oggFile(1, OpusKit.head(2, 0, 2560, 0, 0, 0, null), packets, 255, -1, true);
        short[] gotLoud = AudioKit.toShorts(AudioKit.readAll(AudioDecoders.open(loud, null, null)));
        boolean clipped = false;
        // i 를 0 부터 gotLoud.length 까지 늘려 가며 항목마다 처리한다.
        for (int i = 0; i < gotLoud.length; i++) {
            if (gotLoud[i] == 32767 || gotLoud[i] == -32768) {
                clipped = true;
            }
            int e = (int) Math.max(-32768, Math.min(32767, Math.round(direct[i] * Math.pow(10.0, 10.0 / 20.0))));
            Check.isTrue(Math.abs(e - gotLoud[i]) <= 3, "loud sample " + i + " expected about " + e + " but was " + gotLoud[i]);
        }
        Check.isTrue(clipped, "the louder file reaches the limits of 16 bits");
    }

    /**
     * A mono file gives a mono source.
     * <p>
     * 모노 파일은 모노 소스가 된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMono() throws Exception {
        byte[][] packets = OpusKit.vectorPackets(3, 60);
        byte[] data = OpusKit.oggFile(1, OpusKit.head(1, 0, 0, 0, 0, 0, null), packets, 255, -1, true);
        PcmSource src = AudioDecoders.open(data, null, null);
        Check.equal(1, src.getChannels(), "channels");
        short[] got = AudioKit.toShorts(AudioKit.readAll(src));
        Check.equal(total(packets), got.length, "samples");
        same(OpusKit.decodeDirect(packets, 1), 0, got, got.length, 1, "mono samples");
    }

    /**
     * Channel mapping family 1 with six channels (two coupled streams and two mono streams) decodes all streams of each packet
     * and mixes the channels down to stereo.
     * <p>
     * 6채널 채널 매핑 군 1(결합 스트림 둘과 모노 스트림 둘)은 패킷마다 모든 스트림을 디코딩하고 채널을 스테레오로 다운믹스한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMultistreamFiveOne() throws Exception {
        byte[][] single = OpusKit.vectorPackets(7, 40);
        byte[][] multi = new byte[single.length][];
        // 패킷마다 같은 패킷을 스트림 네 개에 넣어 다중 스트림 패킷을 만든다.
        for (int i = 0; i < single.length; i++) {
            multi[i] = OpusKit.multistream(single[i], single[i], single[i], single[i]);
        }
        int[] mapping = { 0, 4, 1, 2, 3, 5 };
        byte[] data = OpusKit.oggFile(1, OpusKit.head(6, 0, 0, 1, 4, 2, mapping), multi, 255, -1, true);
        PcmSource src = AudioDecoders.open(data, null, null);
        Check.equal(2, src.getChannels(), "mixed to stereo");
        short[] got = AudioKit.toShorts(AudioKit.readAll(src));
        float[] st = OpusKit.decodeFloat(single, 2);
        float[] mo = OpusKit.decodeFloat(single, 1);
        Check.equal(st.length, got.length, "samples");
        ChannelMixer mixer = ChannelMixer.forChannels(6);
        float[] frame = new float[6];
        float[] mixed = new float[2];
        // 프레임마다 매핑에 따라 여섯 채널을 구성해 같은 믹서로 섞은 기대값과 비교한다.
        for (int i = 0; i < st.length / 2; i++) {
            float[] decoded = { st[2 * i], st[2 * i + 1], st[2 * i], st[2 * i + 1], mo[i], mo[i] };
            // c 를 0 부터 6 까지 늘려 가며 항목마다 처리한다.
            for (int c = 0; c < 6; c++) {
                frame[c] = decoded[mapping[c]];
            }
            mixer.mix(frame, mixed);
            Check.isTrue(Math.abs(PcmConverter.clip16(mixed[0]) - got[2 * i]) <= 1, "left " + i);
            Check.isTrue(Math.abs(PcmConverter.clip16(mixed[1]) - got[2 * i + 1]) <= 1, "right " + i);
        }
    }

    /**
     * A silent channel of the mapping (255) and several mappings of the same decoded channel are honored.
     * <p>
     * 매핑의 무음 채널(255)과 같은 디코딩 채널을 여러 곳에 매핑한 경우를 따른다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testChannelMappingSilenceAndDuplicates() throws Exception {
        byte[][] single = OpusKit.vectorPackets(7, 30);
        byte[][] multi = new byte[single.length][];
        // 결합 스트림 하나로 다중 스트림 패킷을 만든다(스트림이 하나면 일반 패킷과 같다).
        for (int i = 0; i < single.length; i++) {
            multi[i] = single[i];
        }
        // 세 채널: 매핑 군 1 은 Vorbis 순서(왼쪽, 중앙, 오른쪽)이므로 스트림의 두 채널이 왼쪽과 중앙이고 오른쪽은 무음이다.
        int[] mapping = { 0, 1, 255 };
        byte[] data = OpusKit.oggFile(1, OpusKit.head(3, 0, 0, 1, 1, 1, mapping), multi, 255, -1, true);
        PcmSource src = AudioDecoders.open(data, null, null);
        short[] got = AudioKit.toShorts(AudioKit.readAll(src));
        float[] st = OpusKit.decodeFloat(single, 2);
        ChannelMixer mixer = ChannelMixer.forVorbisOrder(3);
        float[] mixed = new float[2];
        // 무음 채널은 0 으로 섞여야 한다.
        for (int i = 0; i < st.length / 2; i++) {
            mixer.mix(new float[] { st[2 * i], st[2 * i + 1], 0 }, mixed);
            Check.isTrue(Math.abs(PcmConverter.clip16(mixed[0]) - got[2 * i]) <= 1, "left " + i);
            Check.isTrue(Math.abs(PcmConverter.clip16(mixed[1]) - got[2 * i + 1]) <= 1, "right " + i);
        }
    }

    /**
     * Two links (a chained file) play one after the other, each with its own pre-skip and with a decoder that starts anew.
     * <p>
     * 고리 둘(체인 파일)이 차례로 재생되며, 각각 자기 사전 건너뛰기를 쓰고 디코더를 새로 시작한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testChainedStreams() throws Exception {
        byte[][] a = OpusKit.vectorPackets(7, 30);
        byte[][] b = OpusKit.vectorPackets(2, 30);
        byte[] one = OpusKit.oggFile(11, OpusKit.head(2, 200, 0, 0, 0, 0, null), a, 255, -1, true);
        byte[] two = OpusKit.oggFile(22, OpusKit.head(2, 50, 0, 0, 0, 0, null), b, 255, -1, true);
        PcmSource src = AudioDecoders.open(OggMux.concat(one, two), null, null);
        long expected = (total(a) - 200) + (total(b) - 50);
        Check.equal(expected, src.getFrameCount(), "frame count of both links");
        short[] got = AudioKit.toShorts(AudioKit.readAll(src));
        Check.equal(expected * 2, got.length, "samples of both links");
        int n1 = (total(a) - 200) * 2;
        same(OpusKit.decodeDirect(a, 2), 400, got, n1, 1, "first link");
        short[] second = new short[got.length - n1];
        System.arraycopy(got, n1, second, 0, second.length);
        same(OpusKit.decodeDirect(b, 2), 100, second, second.length, 1, "second link");
    }

    /**
     * A chained file whose links have different channel counts delivers stereo, with the mono link duplicated into both channels.
     * <p>
     * 고리마다 채널 수가 다른 체인 파일은 스테레오로 전달하며, 모노 고리는 두 채널에 복제된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testChainedMonoAndStereo() throws Exception {
        byte[][] a = OpusKit.vectorPackets(3, 25);
        byte[][] b = OpusKit.vectorPackets(7, 25);
        byte[] one = OpusKit.oggFile(11, OpusKit.head(1, 0, 0, 0, 0, 0, null), a, 255, -1, true);
        byte[] two = OpusKit.oggFile(22, OpusKit.head(2, 0, 0, 0, 0, 0, null), b, 255, -1, true);
        PcmSource src = AudioDecoders.open(OggMux.concat(one, two), null, null);
        Check.equal(2, src.getChannels(), "stereo output");
        short[] got = AudioKit.toShorts(AudioKit.readAll(src));
        short[] mono = OpusKit.decodeDirect(a, 1);
        // 모노 고리는 같은 값이 양쪽 채널에 들어 있어야 한다.
        for (int i = 0; i < mono.length; i++) {
            Check.isTrue(Math.abs(mono[i] - got[2 * i]) <= 1 && Math.abs(mono[i] - got[2 * i + 1]) <= 1, "mono frame " + i);
        }
        same(OpusKit.decodeDirect(b, 2), 0, new short[0], 0, 1, "unused");
        short[] second = new short[got.length - 2 * mono.length];
        System.arraycopy(got, 2 * mono.length, second, 0, second.length);
        same(OpusKit.decodeDirect(b, 2), 0, second, second.length, 1, "stereo link");
    }

    /**
     * Seeking lands on the exact frame: a seek to the start equals the plain decoding, and a seek into the middle delivers the
     * rest of the stream with the exact length and, after the pre-roll, with samples close to the continuous decoding.
     * <p>
     * 탐색은 정확한 프레임에 닿는다: 처음으로의 탐색은 일반 디코딩과 같고, 중간으로의 탐색은 정확한 길이로 나머지를 전달하며 사전 롤 뒤에는
     * 연속 디코딩에 가까운 샘플을 낸다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSeeking() throws Exception {
        byte[][] packets = OpusKit.vectorPackets(7, 700);
        byte[] data = OpusKit.oggFile(1, OpusKit.head(2, 312, 0, 0, 0, 0, null), packets, 255, -1, true);
        PcmSource src = AudioDecoders.open(data, null, null);
        Check.isTrue(src.isSeekable(), "seekable");
        long frames = src.getFrameCount();
        short[] all = AudioKit.toShorts(AudioKit.readAll(src));
        Check.equal(2 * frames, all.length, "full length");
        // 처음으로 되돌아가면 일반 디코딩과 같다.
        src.seekToFrame(0);
        short[] again = AudioKit.toShorts(AudioKit.readAll(src));
        Check.equal(all.length, again.length, "length after seeking to 0");
        same(all, 0, again, all.length, 0, "after seeking to the start");
        long[] targets = { 1, 959, 960, 12345, frames / 2, frames - 5000, frames - 1 };
        // 여러 위치로 탐색해 길이가 정확하고 내용이 연속 디코딩에 가까운지 확인한다.
        for (int t = 0; t < targets.length; t++) {
            long f = targets[t];
            src.seekToFrame(f);
            short[] rest = AudioKit.toShorts(AudioKit.readAll(src));
            Check.equal(2 * (frames - f), rest.length, "samples after seeking to " + f);
            int cmp = (int) Math.min(rest.length / 2, 4800);
            double err = 0;
            double sig = 0;
            // 사전 롤이 지난 뒤이므로 연속 디코딩과 가까워야 한다(상태가 수렴한 정도를 신호 대 오차비로 잰다).
            for (int i = 0; i < cmp * 2; i++) {
                double e = rest[i] - all[(int) (2 * f) + i];
                err += e * e;
                sig += (double) all[(int) (2 * f) + i] * all[(int) (2 * f) + i];
            }
            double snr = err == 0 ? 200 : 10 * Math.log10(sig / err);
            Check.isTrue(snr >= 25, "seek to " + f + ": signal to error ratio " + snr + " dB against the continuous decoding");
        }
        src.seekToFrame(frames + 1000);
        Check.equal(-1, src.read(new byte[400], 0, 400), "reading after a seek beyond the end");
    }

    /**
     * Damaged packets are concealed and keep the length of the stream; a stream that is nothing but damaged packets is corrupt.
     * <p>
     * 손상된 패킷은 은닉되어 스트림 길이를 유지한다. 손상된 패킷으로만 이루어진 스트림은 손상된 파일이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDamagedPackets() throws Exception {
        byte[][] packets = OpusKit.vectorPackets(7, 60);
        byte[][] bad = packets.clone();
        // 가운데 패킷 몇 개를 올바르지 않은 형식(프레임 수 0 인 코드 3)으로 바꾼다.
        for (int i = 20; i < 24; i++) {
            bad[i] = new byte[] { (byte) (packets[i][0] | 3), 0, 1, 2, 3 };
        }
        bad[30] = new byte[0];
        byte[] data = OpusKit.oggFile(1, OpusKit.head(2, 0, 0, 0, 0, 0, null), bad, 255, -1, true);
        PcmSource src = AudioDecoders.open(data, null, null);
        short[] got = AudioKit.toShorts(AudioKit.readAll(src));
        Check.isTrue(got.length > 0, "decoded something");
        Check.equal(src.getFrameCount() * 2, got.length, "length is kept (damaged packets have the duration of the packet before)");
        byte[][] allBad = new byte[10][];
        // 모든 패킷이 올바르지 않으면 손상된 파일이다.
        for (int i = 0; i < allBad.length; i++) {
            allBad[i] = new byte[] { 0x03, 0, 1 };
        }
        byte[] garbage = OpusKit.oggFile(1, OpusKit.head(2, 0, 0, 0, 0, 0, null), allBad, 255, -1, true);
        try {
            AudioKit.readAll(AudioDecoders.open(garbage, null, null));
            Check.fail("a stream of only invalid packets must fail");
        } catch (MediaException e) {
            Check.equal(MediaException.Kind.CORRUPT, e.getKind(), "kind");
        }
    }

    /**
     * Header errors are reported as the documented kinds.
     * <p>
     * 헤더 오류는 문서화한 종류로 보고된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHeaderErrors() throws Exception {
        byte[][] packets = OpusKit.vectorPackets(7, 5);
        byte[] ok = OpusKit.head(2, 0, 0, 0, 0, 0, null);
        expect(withHead(ok, 8, 0x10, packets), MediaException.Kind.UNSUPPORTED_FORMAT, "version 16");
        expect(withHead(ok, 9, 0, packets), MediaException.Kind.CORRUPT, "zero channels");
        expect(withHead(ok, 18, 2, packets), MediaException.Kind.UNSUPPORTED_FORMAT, "mapping family 2");
        expect(withHead(ok, 18, 255, packets), MediaException.Kind.UNSUPPORTED_FORMAT, "mapping family 255");
        expect(withHead(ok, 9, 3, packets), MediaException.Kind.CORRUPT, "family 0 with three channels");
        byte[] shortHead = new byte[12];
        System.arraycopy(ok, 0, shortHead, 0, 12);
        expect(OpusKit.oggFile(1, shortHead, packets, 255, -1, true), MediaException.Kind.CORRUPT, "truncated header");
        // 채널 매핑 표가 잘렸거나 범위를 벗어난 경우.
        expect(OpusKit.oggFile(1, OpusKit.head(4, 0, 0, 1, 2, 2, new int[] { 0, 1, 2, 9 }), packets, 255, -1, true), MediaException.Kind.CORRUPT, "mapping out of range");
        expect(OpusKit.oggFile(1, OpusKit.head(2, 0, 0, 1, 9, 0, new int[] { 0, 1 }), packets, 255, -1, true), MediaException.Kind.CORRUPT, "too many streams");
        expect(OpusKit.oggFile(1, OpusKit.head(2, 0, 0, 1, 1, 2, new int[] { 0, 1 }), packets, 255, -1, true), MediaException.Kind.CORRUPT, "more coupled than streams");
        byte[] cut = OpusKit.head(4, 0, 0, 1, 2, 2, new int[] { 0, 1, 2, 3 });
        byte[] cutHead = new byte[cut.length - 2];
        System.arraycopy(cut, 0, cutHead, 0, cutHead.length);
        expect(OpusKit.oggFile(1, cutHead, packets, 255, -1, true), MediaException.Kind.CORRUPT, "truncated mapping table");
        // OpusTags 가 없으면 손상된 파일이다.
        byte[] h = OggMux.mux(1, 0, new byte[][] { ok }, new long[] { 0 }, 255, true, false);
        byte[] notTags = OggMux.mux(1, 1, new byte[][] { new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 } }, new long[] { 0 }, 255, false, false);
        byte[] a = OggMux.mux(1, 2, packets, new long[] { 960, 1920, 2880, 3840, 4800 }, 255, false, true);
        expect(OggMux.concat(h, notTags, a), MediaException.Kind.CORRUPT, "no OpusTags");
    }

    /**
     * Builds a file with a modified header byte.
     * <p>
     * 헤더 바이트 하나를 바꾼 파일을 만든다.
     *
     * @param head    the valid header / 올바른 헤더
     * @param index   index of the byte / 바이트의 위치
     * @param value   the new value / 새 값
     * @param packets audio packets / 오디오 패킷
     * @return the file / 파일
     */
    private static byte[] withHead(byte[] head, int index, int value, byte[][] packets) {
        byte[] h = head.clone();
        h[index] = (byte) value;
        return OpusKit.oggFile(1, h, packets, 255, -1, true);
    }

    /**
     * Requires that opening and reading the data fails with a kind.
     * <p>
     * 데이터를 열고 읽을 때 지정한 종류로 실패해야 한다.
     *
     * @param data the data / 데이터
     * @param kind the kind / 종류
     * @param what description / 설명
     */
    private static void expect(byte[] data, MediaException.Kind kind, String what) {
        try {
            AudioKit.readAll(AudioDecoders.open(data, null, null));
            Check.fail(what + ": expected a failure");
        } catch (MediaException e) {
            Check.equal(kind, e.getKind(), what + " (" + e.getMessage() + ")");
        }
    }

    /**
     * Truncated and bit-flipped files either decode or fail with a {@link MediaException}; nothing else is thrown and nothing hangs.
     * <p>
     * 잘렸거나 비트가 뒤집힌 파일은 디코딩되거나 {@link MediaException} 으로 실패한다. 그 밖의 예외는 던지지 않고 멈추지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTruncatedAndCorruptedFiles() throws Exception {
        byte[][] packets = OpusKit.vectorPackets(10, 40);
        byte[] data = OpusKit.oggFile(1, OpusKit.head(2, 312, 0, 0, 0, 0, null), packets, 255, -1, true);
        // 여러 길이로 자른 파일을 시험한다.
        for (int cut = 0; cut < data.length; cut += 37) {
            byte[] d = new byte[cut];
            System.arraycopy(data, 0, d, 0, cut);
            drain(d);
        }
        Random rnd = new Random(99);
        // 무작위로 바이트를 바꾼 파일을 시험한다.
        for (int round = 0; round < 300; round++) {
            byte[] d = data.clone();
            int flips = 1 + rnd.nextInt(8);
            // k 를 0 부터 flips 까지 늘려 가며 항목마다 처리한다.
            for (int k = 0; k < flips; k++) {
                d[rnd.nextInt(d.length)] ^= (byte) (1 << rnd.nextInt(8));
            }
            drain(d);
        }
        // 페이로드만 무작위로 채운 파일(OpusHead 와 OpusTags 는 유지)도 시험한다.
        for (int round = 0; round < 40; round++) {
            byte[][] noise = new byte[20][];
            // i 를 0 부터 noise.length 까지 늘려 가며 항목마다 처리한다.
            for (int i = 0; i < noise.length; i++) {
                noise[i] = new byte[1 + rnd.nextInt(300)];
                rnd.nextBytes(noise[i]);
            }
            drain(OpusKit.oggFile(1, OpusKit.head(2, 0, 0, 0, 0, 0, null), noise, 255, -1, true));
        }
    }

    /**
     * Opens and reads data to the end, allowing only a {@link MediaException}.
     * <p>
     * 데이터를 열어 끝까지 읽는다. {@link MediaException} 만 허용한다.
     *
     * @param d the data / 데이터
     */
    private static void drain(byte[] d) {
        try {
            PcmSource s = AudioDecoders.open(d, null, null);
            byte[] buf = new byte[4096];
            long total = 0;
            // 끝이나 오류가 날 때까지 읽는다(무한 출력이 아닌지 상한도 둔다).
            while (true) {
                int r = s.read(buf, 0, buf.length);
                if (r < 0) {
                    break;
                }
                total += r;
                if (total > 400L * 1000 * 1000) {
                    Check.fail("unbounded output");
                }
            }
            s.close();
        } catch (MediaException e) {
            return;
        }
    }

    /**
     * An interrupted thread cancels indexing and decoding with the kind CANCELLED, and the interrupt flag stays set.
     * <p>
     * 인터럽트된 스레드는 색인과 디코딩을 CANCELLED 로 취소하며 인터럽트 플래그는 유지된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCancellation() throws Exception {
        byte[][] base = OpusKit.vectorPackets(7, 200);
        byte[][] many = new byte[20000][];
        // 긴 스트림을 만들기 위해 패킷을 반복한다.
        for (int i = 0; i < many.length; i++) {
            many[i] = base[i % base.length];
        }
        byte[] data = OpusKit.oggFile(1, OpusKit.head(2, 0, 0, 0, 0, 0, null), many, 255, -1, true);
        PcmSource src = AudioDecoders.open(data, null, null);
        Thread.currentThread().interrupt();
        try {
            src.read(new byte[4096], 0, 4096);
            Check.fail("a read in an interrupted thread must fail");
        } catch (MediaException e) {
            Check.equal(MediaException.Kind.CANCELLED, e.getKind(), "read");
            Check.isTrue(Thread.currentThread().isInterrupted(), "the interrupt flag is kept");
        } finally {
            Thread.interrupted();
        }
        Thread.currentThread().interrupt();
        try {
            AudioDecoders.open(data, null, null);
            Check.fail("opening in an interrupted thread must fail");
        } catch (MediaException e) {
            Check.equal(MediaException.Kind.CANCELLED, e.getKind(), "open");
        } finally {
            Thread.interrupted();
        }
    }

    /**
     * The size limit of the decode limits applies.
     * <p>
     * 디코딩 제한의 크기 한도가 적용된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSizeLimit() throws Exception {
        byte[][] packets = OpusKit.vectorPackets(7, 40);
        byte[] data = OpusKit.oggFile(1, OpusKit.head(2, 0, 0, 0, 0, 0, null), packets, 255, -1, true);
        try {
            AudioDecoders.open(data, null, new DecodeLimits(1000, 1000, 10, 1000));
            Check.fail("a file over the limit must fail");
        } catch (MediaException e) {
            Check.equal(MediaException.Kind.TOO_LARGE, e.getKind(), "kind");
        }
    }

    /**
     * Pages that end in the middle of a packet (small pages) make no difference to decoding or seeking.
     * <p>
     * 패킷 중간에서 끝나는 페이지(작은 페이지)는 디코딩과 탐색에 차이를 주지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPacketsSplitOverPages() throws Exception {
        byte[][] packets = OpusKit.vectorPackets(11, 30);
        byte[] big = OpusKit.oggFile(1, OpusKit.head(2, 0, 0, 0, 0, 0, null), packets, 255, -1, true);
        byte[] small = OpusKit.oggFile(1, OpusKit.head(2, 0, 0, 0, 0, 0, null), packets, 3, -1, true);
        short[] a = AudioKit.toShorts(AudioKit.readAll(AudioDecoders.open(big, null, null)));
        PcmSource s = AudioDecoders.open(small, null, null);
        short[] b = AudioKit.toShorts(AudioKit.readAll(s));
        Check.equal(a.length, b.length, "length");
        same(a, 0, b, a.length, 0, "same samples");
        s.seekToFrame(5000);
        short[] rest = AudioKit.toShorts(AudioKit.readAll(s));
        Check.equal(a.length - 10000, rest.length, "rest after a seek");
    }
}
