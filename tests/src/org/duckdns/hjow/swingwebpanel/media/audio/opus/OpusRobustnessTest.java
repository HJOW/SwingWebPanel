package org.duckdns.hjow.swingwebpanel.media.audio.opus;

import java.util.Random;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Robustness tests of the frame decoder against hostile or damaged data: random and truncated packets of every mode, lost packets
 * between valid ones, switches between streams of different modes and channel counts, the bounds of the packet padding. The decoder
 * may reject a packet with an error code, but it must never throw an exception, hang, or produce a sample that is not a finite
 * number (RFC 8251 section 8 fixes such a case of the reference decoder).
 * <p>
 * 악의적이거나 손상된 데이터에 대한 프레임 디코더의 견고성 시험: 모든 모드의 무작위 패킷과 잘린 패킷, 올바른 패킷 사이의 손실 패킷, 모드와 채널 수가 다른
 * 스트림 사이의 전환, 패킷 패딩의 한계. 디코더는 오류 코드로 패킷을 거부할 수는 있지만 예외를 던지거나 멈추거나 유한한 수가 아닌 샘플을 만들어서는 안
 * 된다(RFC 8251 8장이 참조 디코더의 그런 경우를 고쳤다).
 */
public class OpusRobustnessTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public OpusRobustnessTest() {
    }

    /**
     * Requires that the first samples of a buffer are finite numbers of a sane size.
     * <p>
     * 버퍼의 앞쪽 샘플이 적당한 크기의 유한한 수인지 확인한다.
     *
     * @param pcm   the samples / 샘플
     * @param count number of samples to check / 확인할 샘플 수
     * @param what  description / 설명
     */
    private static void finite(float[] pcm, int count, String what) {
        // 샘플마다 NaN 이나 무한대가 아닌지 확인한다.
        for (int i = 0; i < count; i++) {
            if (Float.isNaN(pcm[i]) || Float.isInfinite(pcm[i]) || Math.abs(pcm[i]) > 1e6f) {
                Check.fail(what + ": sample " + i + " is " + pcm[i]);
            }
        }
    }

    /**
     * Random bytes behind every table-of-contents byte decode (or are rejected) without exceptions, for mono and stereo decoders.
     * <p>
     * 모든 목차 바이트 뒤의 무작위 바이트는 예외 없이 디코딩되거나 거부된다(모노와 스테레오 디코더 모두).
     */
    public void testRandomPackets() {
        Random rnd = new Random(2026);
        float[] pcm = new float[OpusPacket.MAX_PACKET_SAMPLES * 2];
        // 채널 수가 다른 디코더 두 개로 시험한다.
        for (int ch = 1; ch <= 2; ch++) {
            OpusFrameDecoder dec = new OpusFrameDecoder(ch);
            // 목차 바이트 256 가지와 임의 길이의 임의 내용을 시험한다.
            for (int round = 0; round < 3; round++) {
                for (int toc = 0; toc < 256; toc++) {
                    int len = 1 + rnd.nextInt(round == 0 ? 20 : (round == 1 ? 300 : 1275));
                    byte[] p = new byte[len];
                    rnd.nextBytes(p);
                    p[0] = (byte) toc;
                    int n = dec.decodePacket(p, 0, len, pcm, 0, OpusPacket.MAX_PACKET_SAMPLES);
                    if (n > 0) {
                        finite(pcm, n * ch, "toc " + toc + " channels " + ch);
                    }
                }
            }
        }
    }

    /**
     * Payloads of all zeros and of all ones for every configuration.
     * <p>
     * 모든 구성에서 전부 0 인 페이로드와 전부 1 인 페이로드.
     */
    public void testConstantPayloads() {
        float[] pcm = new float[OpusPacket.MAX_PACKET_SAMPLES * 2];
        // 구성(0..31)과 스테레오 비트마다 시험한다.
        for (int cfg = 0; cfg < 32; cfg++) {
            for (int st = 0; st < 2; st++) {
                // fill 를 0 부터 2 까지 늘려 가며 항목마다 처리한다.
                for (int fill = 0; fill < 2; fill++) {
                    OpusFrameDecoder dec = new OpusFrameDecoder(2);
                    byte[] p = new byte[200];
                    java.util.Arrays.fill(p, (byte) (fill == 0 ? 0 : 0xFF));
                    p[0] = (byte) ((cfg << 3) | (st << 2));
                    int n = dec.decodePacket(p, 0, p.length, pcm, 0, OpusPacket.MAX_PACKET_SAMPLES);
                    if (n > 0) {
                        finite(pcm, n * 2, "config " + cfg + " fill " + fill);
                    }
                }
            }
        }
    }

    /**
     * Valid packets cut at every length, and sequences with lost packets in between, never fail and stay finite.
     * <p>
     * 모든 길이로 자른 올바른 패킷과 사이에 손실 패킷이 있는 열은 실패하지 않으며 유한한 값을 낸다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTruncatedAndLostPackets() throws Exception {
        float[] pcm = new float[OpusPacket.MAX_PACKET_SAMPLES * 2];
        Random rnd = new Random(5);
        int[] vectors = { 1, 2, 5, 8, 10, 12 };
        // 여러 벡터의 패킷을 자르거나 건너뛰며 디코딩한다.
        for (int v = 0; v < vectors.length; v++) {
            byte[][] packets = OpusKit.vectorPackets(vectors[v], 120);
            OpusFrameDecoder dec = new OpusFrameDecoder(2);
            // i 를 0 부터 packets.length 까지 늘려 가며 항목마다 처리한다.
            for (int i = 0; i < packets.length; i++) {
                byte[] p = packets[i];
                int kind = rnd.nextInt(5);
                int n;
                if (kind == 0) {
                    n = dec.decodePacket(null, 0, 0, pcm, 0, OpusPacket.MAX_PACKET_SAMPLES);
                } else if (kind == 1) {
                    n = dec.decodePacket(p, 0, 1 + rnd.nextInt(p.length), pcm, 0, OpusPacket.MAX_PACKET_SAMPLES);
                } else if (kind == 2) {
                    n = dec.decodeLost(pcm, 0, 120 * (1 + rnd.nextInt(48)));
                } else {
                    n = dec.decodePacket(p, 0, p.length, pcm, 0, OpusPacket.MAX_PACKET_SAMPLES);
                }
                if (n > 0) {
                    finite(pcm, n * 2, "vector " + vectors[v] + " packet " + i + " kind " + kind);
                }
            }
        }
    }

    /**
     * A decoder that goes through packets of different vectors (other modes, other channel counts, other frame sizes, no reset in
     * between) keeps working.
     * <p>
     * 서로 다른 벡터의 패킷(다른 모드, 채널 수, 프레임 크기를 리셋 없이)을 거쳐 가는 디코더는 계속 동작한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSwitchingBetweenStreams() throws Exception {
        float[] pcm = new float[OpusPacket.MAX_PACKET_SAMPLES * 2];
        OpusFrameDecoder dec = new OpusFrameDecoder(2);
        byte[][][] sets = new byte[12][][];
        // 벡터마다 앞쪽 패킷을 읽어 둔다.
        for (int v = 0; v < 12; v++) {
            sets[v] = OpusKit.vectorPackets(v + 1, 40);
        }
        Random rnd = new Random(8);
        // 임의의 벡터에서 임의의 패킷을 골라 디코딩한다.
        for (int i = 0; i < 1500; i++) {
            byte[][] s = sets[rnd.nextInt(12)];
            byte[] p = s[rnd.nextInt(s.length)];
            int n = dec.decodePacket(p, 0, p.length, pcm, 0, OpusPacket.MAX_PACKET_SAMPLES);
            if (n > 0) {
                finite(pcm, n * 2, "switch " + i);
            }
        }
    }

    /**
     * A packet whose padding field claims far more than the packet holds is rejected (the integer overflow of RFC 8251 section 4 does not
     * occur), and the padding length 255 continues in the next byte.
     * <p>
     * 패딩 필드가 패킷이 가진 것보다 훨씬 많다고 주장하는 패킷은 거부된다(RFC 8251 4장의 정수 넘침이 없다). 패딩 길이 255 는 다음 바이트로 이어진다.
     */
    public void testHugePaddingClaim() {
        byte[] p = new byte[3000];
        p[0] = (byte) 0x7B;
        p[1] = 0x41;
        // 패딩 길이 바이트를 모두 255 로 채운다(합이 패킷 크기보다 훨씬 크다).
        for (int i = 2; i < p.length; i++) {
            p[i] = (byte) 255;
        }
        Check.isNull(OpusPacket.parse(p, 0, p.length), "padding claim larger than the packet");
        byte[] q = new byte[4 + 3 + 254];
        q[0] = (byte) 0x7B;
        q[1] = 0x41;
        q[2] = (byte) 255;
        q[3] = 0;
        Check.equal(3, OpusPacket.parse(q, 0, q.length).sizes[0], "255 then 0 pads 254 bytes");
    }
}
