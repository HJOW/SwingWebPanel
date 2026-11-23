package org.duckdns.hjow.swingwebpanel.media.audio.opus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.OggMux;

/**
 * Test-only builders of Ogg Opus data: the Opus packets of the committed RFC 6716 test vectors are the audio payload (they are
 * valid Opus packets of every mode and frame size), and the Ogg framing, the identification header and the multistream packets
 * are written here from RFC 7845 and RFC 6716 appendix B independently of the decoder, so that decoding what these builders
 * write checks the decoder's handling of the container: pre-skip, gain, granule positions, chaining, channel mapping and so on.
 * <p>
 * Ogg Opus 데이터의 시험 전용 생성기: 저장소에 있는 RFC 6716 시험 벡터의 Opus 패킷을 오디오 페이로드로 쓰고(모든 모드와 프레임 크기의 올바른
 * Opus 패킷), Ogg 프레이밍, 식별 헤더, 다중 스트림 패킷은 RFC 7845 와 RFC 6716 부록 B 에서 디코더와 별개로 직접 작성한다. 이 생성기가 쓴 것을
 * 디코딩하면 사전 건너뛰기, 이득, 그래뉼 위치, 체인, 채널 매핑 같은 컨테이너 처리를 검증하게 된다.
 */
final class OpusKit {

    /**
     * Not instantiable.
     * <p>
     * 인스턴스를 만들 수 없다.
     */
    private OpusKit() {
    }

    /**
     * Reads the payloads of the first packets of a committed test vector.
     * <p>
     * 저장소에 있는 시험 벡터의 앞쪽 패킷의 페이로드를 읽는다.
     *
     * @param vector vector number, 1..12 / 벡터 번호(1..12)
     * @param count  number of packets / 패킷 수
     * @return the packets / 패킷
     * @throws IOException if the resource is missing / 자료가 없으면
     */
    static byte[][] vectorPackets(int vector, int count) throws IOException {
        InputStream in = OpusKit.class.getResourceAsStream(String.format("/media/opus/vectors/testvector%02d.bit", Integer.valueOf(vector)));
        if (in == null) {
            throw new IOException("missing test vector " + vector);
        }
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] b = new byte[65536];
        int r;
        // 자료를 모두 읽는다.
        while ((r = in.read(b)) > 0) {
            bo.write(b, 0, r);
        }
        in.close();
        byte[] d = bo.toByteArray();
        List<byte[]> list = new ArrayList<byte[]>();
        int pos = 0;
        // 패킷마다 4 바이트 길이와 4 바이트 최종 범위 다음의 페이로드를 꺼낸다.
        while (pos + 8 <= d.length && list.size() < count) {
            int len = ((d[pos] & 255) << 24) | ((d[pos + 1] & 255) << 16) | ((d[pos + 2] & 255) << 8) | (d[pos + 3] & 255);
            pos += 8;
            byte[] p = new byte[len];
            System.arraycopy(d, pos, p, 0, len);
            list.add(p);
            pos += len;
        }
        return list.toArray(new byte[0][]);
    }

    /**
     * Number of samples at 48 kHz of a packet.
     * <p>
     * 패킷의 48 kHz 샘플 수.
     *
     * @param p the packet / 패킷
     * @return the samples / 샘플 수
     */
    static int samples(byte[] p) {
        OpusPacket pk = OpusPacket.parse(p, 0, p.length);
        return pk.count * OpusPacket.getSamplesPerFrame(pk.toc);
    }

    /**
     * Decodes packets directly with the frame decoder (no container) to 16-bit samples.
     * <p>
     * 패킷을 컨테이너 없이 프레임 디코더로 직접 디코딩해 16비트 샘플로 만든다.
     *
     * @param packets  the packets / 패킷
     * @param channels decoder channels, 1 or 2 / 디코더 채널 수(1 또는 2)
     * @return the interleaved samples / 인터리브된 샘플
     */
    static short[] decodeDirect(byte[][] packets, int channels) {
        OpusFrameDecoder dec = new OpusFrameDecoder(channels);
        float[] pcm = new float[OpusPacket.MAX_PACKET_SAMPLES * channels];
        short[] out = new short[0];
        int total = 0;
        // 패킷마다 디코딩해 이어 붙인다.
        for (int i = 0; i < packets.length; i++) {
            int n = dec.decodePacket(packets[i], 0, packets[i].length, pcm, 0, OpusPacket.MAX_PACKET_SAMPLES);
            if (total + n * channels > out.length) {
                out = java.util.Arrays.copyOf(out, Math.max(out.length * 2, total + n * channels));
            }
            // k 를 0 부터 n * channels 까지 늘려 가며 항목마다 처리한다.
            for (int k = 0; k < n * channels; k++) {
                out[total + k] = OpusVectorTest.toPcm16(pcm[k]);
            }
            total += n * channels;
        }
        return java.util.Arrays.copyOf(out, total);
    }

    /**
     * Decodes packets directly with the frame decoder to floating-point samples.
     * <p>
     * 패킷을 프레임 디코더로 직접 디코딩해 부동소수점 샘플로 만든다.
     *
     * @param packets  the packets / 패킷
     * @param channels decoder channels, 1 or 2 / 디코더 채널 수(1 또는 2)
     * @return the interleaved samples (full scale 1.0) / 인터리브된 샘플(풀 스케일 1.0)
     */
    static float[] decodeFloat(byte[][] packets, int channels) {
        OpusFrameDecoder dec = new OpusFrameDecoder(channels);
        float[] pcm = new float[OpusPacket.MAX_PACKET_SAMPLES * channels];
        float[] out = new float[0];
        int total = 0;
        // 패킷마다 디코딩해 이어 붙인다.
        for (int i = 0; i < packets.length; i++) {
            int n = dec.decodePacket(packets[i], 0, packets[i].length, pcm, 0, OpusPacket.MAX_PACKET_SAMPLES);
            if (total + n * channels > out.length) {
                out = java.util.Arrays.copyOf(out, Math.max(out.length * 2, total + n * channels));
            }
            System.arraycopy(pcm, 0, out, total, n * channels);
            total += n * channels;
        }
        return java.util.Arrays.copyOf(out, total);
    }

    /**
     * Builds an identification header.
     * <p>
     * 식별 헤더를 만든다.
     *
     * @param channels channels / 채널 수
     * @param preSkip  pre-skip / 사전 건너뛰기
     * @param gainQ8   output gain in Q7.8 dB / Q7.8 dB 출력 이득
     * @param family   mapping family / 매핑 군
     * @param streams  stream count (family 1) / 스트림 수(군 1)
     * @param coupled  coupled stream count (family 1) / 결합 스트림 수(군 1)
     * @param mapping  channel mapping (family 1) / 채널 매핑(군 1)
     * @return the header packet / 헤더 패킷
     */
    static byte[] head(int channels, int preSkip, int gainQ8, int family, int streams, int coupled, int[] mapping) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        byte[] magic = { 'O', 'p', 'u', 's', 'H', 'e', 'a', 'd' };
        o.write(magic, 0, 8);
        o.write(1);
        o.write(channels);
        o.write(preSkip);
        o.write(preSkip >> 8);
        o.write(48000 & 255);
        o.write((48000 >> 8) & 255);
        o.write(0);
        o.write(0);
        o.write(gainQ8);
        o.write(gainQ8 >> 8);
        o.write(family);
        if (family != 0) {
            o.write(streams);
            o.write(coupled);
            // 채널마다 매핑 값을 쓴다.
            for (int i = 0; i < channels; i++) {
                o.write(mapping[i]);
            }
        }
        return o.toByteArray();
    }

    /**
     * Builds a comment header with a vendor string and no comments.
     * <p>
     * 공급자 문자열만 있고 주석이 없는 주석 헤더를 만든다.
     *
     * @return the OpusTags packet / OpusTags 패킷
     */
    static byte[] tags() {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        byte[] magic = { 'O', 'p', 'u', 's', 'T', 'a', 'g', 's' };
        o.write(magic, 0, 8);
        o.write(4);
        o.write(0);
        o.write(0);
        o.write(0);
        o.write('t');
        o.write('e');
        o.write('s');
        o.write('t');
        o.write(0);
        o.write(0);
        o.write(0);
        o.write(0);
        return o.toByteArray();
    }

    /**
     * Builds one logical Ogg Opus stream: the head page, the tags page and the audio pages with the granule positions that follow
     * from the packet durations (the number of decoded samples, pre-skip included), with the last granule position optionally
     * replaced.
     * <p>
     * 논리 Ogg Opus 스트림 하나를 만든다: 머리 페이지, 태그 페이지, 패킷 길이에서 나오는 그래뉼 위치(사전 건너뛰기를 포함한 디코딩 샘플 수)를 가진
     * 오디오 페이지. 마지막 그래뉼 위치는 바꿀 수 있다.
     *
     * @param serial       serial number / 시리얼 번호
     * @param headPacket   the OpusHead packet / OpusHead 패킷
     * @param packets      audio packets / 오디오 패킷
     * @param maxSegments  segments per audio page / 오디오 페이지당 세그먼트 수
     * @param lastGranule  granule position of the last page, or -1 for the natural one / 마지막 페이지의 그래뉼 위치. 자연스러운 값이면 -1
     * @param eos          mark the last page as the end of the stream / 마지막 페이지를 스트림 끝으로 표시
     * @return the Ogg data / Ogg 데이터
     */
    static byte[] oggFile(int serial, byte[] headPacket, byte[][] packets, int maxSegments, long lastGranule, boolean eos) {
        long[] g = new long[packets.length];
        long sum = 0;
        int dur = 960;
        // 패킷의 끝 위치를 그래뉼 위치로 쌓는다(올바르지 않은 패킷은 디코더와 같은 규칙으로 길이를 정한다).
        for (int i = 0; i < packets.length; i++) {
            dur = OpusSource.duration(packets[i], dur);
            sum += dur;
            g[i] = sum;
        }
        if (lastGranule >= 0 && g.length > 0) {
            g[g.length - 1] = lastGranule;
        }
        byte[] h = OggMux.mux(serial, 0, new byte[][] { headPacket }, new long[] { 0 }, 255, true, false);
        byte[] t = OggMux.mux(serial, 1, new byte[][] { tags() }, new long[] { 0 }, 255, false, false);
        byte[] a = OggMux.mux(serial, 2, packets, g, maxSegments, false, eos);
        return OggMux.concat(h, t, a);
    }

    /**
     * Writes a size field of the packet framing (one byte below 252, else two bytes).
     * <p>
     * 패킷 프레이밍의 크기 필드를 쓴다(252 미만은 1 바이트, 그 외는 2 바이트).
     *
     * @param o    the output / 출력
     * @param size the size / 크기
     */
    private static void size(ByteArrayOutputStream o, int size) {
        if (size < 252) {
            o.write(size);
        } else {
            int first = 252 + (size & 3);
            o.write(first);
            o.write((size - first) >> 2);
        }
    }

    /**
     * Converts a regular packet into the self-delimited framing of RFC 6716 appendix B (as code 3 with variable frame sizes, or as
     * code 0 for a single frame), which also states the size of the last frame.
     * <p>
     * 일반 패킷을 RFC 6716 부록 B 의 자체 구분 프레이밍으로 바꾼다(프레임 크기가 가변인 코드 3, 또는 프레임 하나이면 코드 0). 마지막 프레임의
     * 크기도 적는다.
     *
     * @param p the regular packet / 일반 패킷
     * @return the self-delimited packet / 자체 구분 패킷
     */
    static byte[] selfDelimited(byte[] p) {
        OpusPacket pk = OpusPacket.parse(p, 0, p.length);
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        if (pk.count == 1) {
            o.write(pk.toc & 0xFC);
            size(o, pk.sizes[0]);
            o.write(p, pk.offsets[0], pk.sizes[0]);
            return o.toByteArray();
        }
        o.write((pk.toc & 0xFC) | 3);
        o.write(0x80 | pk.count);
        // 프레임마다 크기 필드를 쓴다(마지막 프레임 것까지).
        for (int i = 0; i < pk.count; i++) {
            size(o, pk.sizes[i]);
        }
        // 프레임 데이터를 이어 붙인다.
        for (int i = 0; i < pk.count; i++) {
            o.write(p, pk.offsets[i], pk.sizes[i]);
        }
        return o.toByteArray();
    }

    /**
     * Builds a multistream packet from the packets of its streams (all of the same duration).
     * <p>
     * 스트림들의 패킷(길이가 모두 같음)으로 다중 스트림 패킷을 만든다.
     *
     * @param streamPackets one regular packet per stream / 스트림마다 일반 패킷 하나
     * @return the multistream packet / 다중 스트림 패킷
     */
    static byte[] multistream(byte[]... streamPackets) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        // 마지막을 제외한 스트림은 자체 구분 프레이밍으로 쓴다.
        for (int i = 0; i < streamPackets.length; i++) {
            byte[] b = i < streamPackets.length - 1 ? selfDelimited(streamPackets[i]) : streamPackets[i];
            o.write(b, 0, b.length);
        }
        return o.toByteArray();
    }
}
