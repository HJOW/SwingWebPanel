package org.duckdns.hjow.swingwebpanel.media.audio.opus;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the packet layer of RFC 6716 section 3: the table-of-contents byte for all 32 configurations, the four frame packing
 * codes (one frame, two equal frames, two frames of different size, any number of frames with padding), size fields of one and two
 * bytes, and the rejection of every malformed form.
 * <p>
 * RFC 6716 3장 패킷 계층의 시험: 32가지 구성 전부에 대한 목차(TOC) 바이트, 프레임 묶음 코드 넷(프레임 하나, 같은 크기 둘, 다른 크기 둘,
 * 패딩이 있는 임의 개수), 1~2 바이트 크기 필드, 올바르지 않은 모든 형식의 거부.
 */
public class OpusPacketTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public OpusPacketTest() {
    }

    /**
     * Builds a packet from a TOC byte and a body.
     * <p>
     * TOC 바이트와 본문으로 패킷을 만든다.
     *
     * @param toc  the TOC byte / TOC 바이트
     * @param body the bytes after it (values 0..255) / 그 뒤의 바이트(0~255)
     * @return the packet / 패킷
     */
    private static byte[] packet(int toc, int... body) {
        byte[] p = new byte[1 + body.length];
        p[0] = (byte) toc;
        // 본문을 바이트로 옮긴다.
        for (int i = 0; i < body.length; i++) {
            p[1 + i] = (byte) body[i];
        }
        return p;
    }

    /**
     * The mode, bandwidth and frame duration follow the configuration number of the TOC byte for all 32 configurations.
     * <p>
     * 32가지 구성 전부에서 모드, 대역폭, 프레임 길이는 TOC 바이트의 구성 번호를 따른다.
     */
    public void testAllConfigurations() {
        int[] samples = { 480, 960, 1920, 2880, 480, 960, 1920, 2880, 480, 960, 1920, 2880, 480, 960, 480, 960, 120, 240, 480, 960, 120, 240, 480, 960, 120, 240,
                480, 960, 120, 240, 480, 960 };
        // 구성마다 기대하는 값을 규격 표와 대조한다.
        for (int cfg = 0; cfg < 32; cfg++) {
            int toc = cfg << 3;
            int mode = cfg < 12 ? OpusPacket.MODE_SILK_ONLY : (cfg < 16 ? OpusPacket.MODE_HYBRID : OpusPacket.MODE_CELT_ONLY);
            int bw;
            if (cfg < 12) {
                bw = OpusPacket.BANDWIDTH_NARROWBAND + cfg / 4;
            } else if (cfg < 14) {
                bw = OpusPacket.BANDWIDTH_SUPERWIDEBAND;
            } else if (cfg < 16) {
                bw = OpusPacket.BANDWIDTH_FULLBAND;
            } else {
                int g = (cfg - 16) / 4;
                bw = g == 0 ? OpusPacket.BANDWIDTH_NARROWBAND : OpusPacket.BANDWIDTH_NARROWBAND + g + 1;
            }
            Check.equal(mode, OpusPacket.getMode(toc), "mode of config " + cfg);
            Check.equal(bw, OpusPacket.getBandwidth(toc), "bandwidth of config " + cfg);
            Check.equal(samples[cfg], OpusPacket.getSamplesPerFrame(toc), "frame size of config " + cfg);
        }
        Check.equal(1, OpusPacket.getChannels(0x00), "mono bit");
        Check.equal(2, OpusPacket.getChannels(0x04), "stereo bit");
    }

    /**
     * Code 0: one frame that takes the whole rest of the packet; an empty frame is allowed.
     * <p>
     * 코드 0: 패킷의 나머지 전체를 차지하는 프레임 하나. 빈 프레임도 허용한다.
     */
    public void testCode0() {
        OpusPacket p = OpusPacket.parse(packet(0x78, 1, 2, 3), 0, 4);
        Check.equal(1, p.count, "one frame");
        Check.equal(1, p.offsets[0], "offset");
        Check.equal(3, p.sizes[0], "size");
        OpusPacket e = OpusPacket.parse(packet(0x78), 0, 1);
        Check.equal(0, e.sizes[0], "an empty frame");
        Check.isNull(OpusPacket.parse(new byte[0], 0, 0), "an empty packet");
    }

    /**
     * Code 1: two frames of equal size; an odd payload is invalid.
     * <p>
     * 코드 1: 같은 크기의 프레임 둘. 페이로드가 홀수이면 올바르지 않다.
     */
    public void testCode1() {
        OpusPacket p = OpusPacket.parse(packet(0x79, 1, 2, 3, 4), 0, 5);
        Check.equal(2, p.count, "two frames");
        Check.equal(2, p.sizes[0], "size of frame 0");
        Check.equal(2, p.sizes[1], "size of frame 1");
        Check.equal(3, p.offsets[1], "offset of frame 1");
        Check.isNull(OpusPacket.parse(packet(0x79, 1, 2, 3), 0, 4), "odd payload");
    }

    /**
     * Code 2: two frames, the size of the first one coded in one or two bytes; a size beyond the data is invalid.
     * <p>
     * 코드 2: 프레임 둘, 첫 프레임의 크기는 1~2 바이트로 부호화한다. 데이터를 넘는 크기는 올바르지 않다.
     */
    public void testCode2() {
        OpusPacket p = OpusPacket.parse(packet(0x7A, 2, 9, 9, 7, 7, 7), 0, 7);
        Check.equal(2, p.count, "two frames");
        Check.equal(2, p.sizes[0], "size of frame 0");
        Check.equal(3, p.sizes[1], "size of frame 1");
        Check.equal(2, p.offsets[0], "offset of frame 0");
        Check.equal(4, p.offsets[1], "offset of frame 1");
        // 252 이상은 두 바이트(4 * 둘째 + 첫째)로 부호화한다.
        byte[] big = new byte[1 + 2 + 300 + 5];
        big[0] = 0x7A;
        big[1] = (byte) (252 + 0);
        big[2] = (byte) 12;
        OpusPacket q = OpusPacket.parse(big, 0, big.length);
        Check.equal(300, q.sizes[0], "two-byte size 4 * 12 + 252");
        Check.equal(5, q.sizes[1], "rest");
        Check.isNull(OpusPacket.parse(packet(0x7A, 9, 1, 2), 0, 4), "size beyond the data");
        Check.isNull(OpusPacket.parse(packet(0x7A), 0, 1), "no size field");
        Check.isNull(OpusPacket.parse(packet(0x7A, 253), 0, 2), "truncated two-byte size");
    }

    /**
     * Code 3: constant bit rate frames, variable bit rate frames, padding (including the 255 continuation) and the frame count limits.
     * <p>
     * 코드 3: 고정 비트율 프레임, 가변 비트율 프레임, 패딩(255 연속 포함), 프레임 수 한계.
     */
    public void testCode3() {
        // CBR 프레임 셋(헤더 바이트: 0x03).
        OpusPacket cbr = OpusPacket.parse(packet(0x7B, 0x03, 1, 2, 3, 4, 5, 6), 0, 8);
        Check.equal(3, cbr.count, "three CBR frames");
        Check.equal(2, cbr.sizes[2], "CBR size");
        Check.equal(2, cbr.offsets[0], "CBR offset");
        // VBR 프레임 셋(헤더 0x83, 크기 1 과 2, 나머지는 마지막 프레임).
        OpusPacket vbr = OpusPacket.parse(packet(0x7B, 0x83, 1, 2, 9, 8, 7, 6, 5), 0, 9);
        Check.equal(1, vbr.sizes[0], "VBR size 0");
        Check.equal(2, vbr.sizes[1], "VBR size 1");
        Check.equal(2, vbr.sizes[2], "VBR last size");
        // 패딩 3 바이트(헤더 0x41: 패딩 있음, CBR, 프레임 하나).
        OpusPacket pad = OpusPacket.parse(packet(0x7B, 0x41, 3, 7, 7, 0, 0, 0), 0, 8);
        Check.equal(2, pad.sizes[0], "frame after padding");
        // 패딩 길이 255 는 254 바이트를 뜻하고 다음 바이트가 이어진다.
        byte[] longPad = new byte[1 + 1 + 2 + 254 + 1 + 4];
        longPad[0] = 0x7B;
        longPad[1] = 0x41;
        longPad[2] = (byte) 255;
        longPad[3] = (byte) 1;
        OpusPacket lp = OpusPacket.parse(longPad, 0, longPad.length);
        Check.equal(4, lp.sizes[0], "padding of 255 means 254 more bytes");
        Check.isNull(OpusPacket.parse(packet(0x7B, 0x00, 1, 2), 0, 3), "zero frames");
        Check.isNull(OpusPacket.parse(packet(0x7B, 0x41, 9, 1), 0, 3), "padding longer than the data");
        Check.isNull(OpusPacket.parse(packet(0x7B), 0, 1), "no frame count byte");
        // 2.5 ms 프레임 48 개는 120 ms 로 한계이고 49 개는 넘는다.
        byte[] max = new byte[2 + 48];
        max[0] = (byte) (16 << 3 | 3);
        max[1] = 48;
        Check.notNull(OpusPacket.parse(max, 0, max.length), "48 frames of 2.5 ms (120 ms)");
        byte[] over = new byte[2 + 49];
        over[0] = (byte) (16 << 3 | 3);
        over[1] = 49;
        Check.isNull(OpusPacket.parse(over, 0, over.length), "49 frames of 2.5 ms");
        // 프레임 하나가 1275 바이트를 넘으면 올바르지 않다.
        byte[] huge = new byte[1 + 1276];
        huge[0] = 0x78;
        Check.isNull(OpusPacket.parse(huge, 0, huge.length), "frame of 1276 bytes");
        byte[] ok = new byte[1 + 1275];
        ok[0] = 0x78;
        Check.equal(1275, OpusPacket.parse(ok, 0, ok.length).sizes[0], "frame of 1275 bytes");
    }
}
