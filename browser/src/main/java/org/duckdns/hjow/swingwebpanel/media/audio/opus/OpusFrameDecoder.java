package org.duckdns.hjow.swingwebpanel.media.audio.opus;

/**
 * The decoder of one Opus stream of one or two channels at 48 kHz (RFC 6716 section 4, <code>src/opus_decoder.c</code>): it
 * parses the packet, selects SILK, CELT or the hybrid of both for each frame, handles the redundancy frames that smooth the
 * switches between the codecs, cross-fades the transitions and conceals lost packets. The output is floating point PCM in the
 * range -1..1, interleaved. One instance keeps the state between the packets of one stream. Not thread safe.
 * <p>
 * 채널 하나 또는 둘, 48 kHz 의 Opus 스트림 하나를 디코딩한다(RFC 6716 4장, <code>src/opus_decoder.c</code>). 패킷을 파싱하고 프레임마다
 * SILK, CELT, 둘의 하이브리드를 고르며, 코덱 전환을 부드럽게 하는 중복 프레임을 처리하고, 전환을 교차 페이드하며, 손실된 패킷을 은닉한다.
 * 출력은 -1..1 범위의 인터리브된 부동소수점 PCM 이다. 인스턴스는 한 스트림의 패킷 사이 상태를 유지한다. 스레드에 안전하지 않다.
 */
final class OpusFrameDecoder {

    /**
     * Output sampling rate.
     * <p>
     * 출력 샘플링 주파수.
     */
    static final int FS = 48000;

    /**
     * Error code: an invalid packet.
     * <p>
     * 오류 코드: 올바르지 않은 패킷.
     */
    static final int ERR_INVALID_PACKET = -4;

    /**
     * Error code: a bad argument or an output buffer that is too small.
     * <p>
     * 오류 코드: 잘못된 인수 또는 너무 작은 출력 버퍼.
     */
    static final int ERR_BAD_ARG = -1;

    /**
     * Number of output channels (1 or 2).
     * <p>
     * 출력 채널 수(1 또는 2).
     */
    private final int channels;

    /**
     * The CELT decoder.
     * <p>
     * CELT 디코더.
     */
    private final CeltDecoder celt;

    /**
     * The SILK decoder.
     * <p>
     * SILK 디코더.
     */
    private final SilkDecoder silk = new SilkDecoder();

    /**
     * Parameters of the SILK decoder calls.
     * <p>
     * SILK 디코더 호출의 매개변수.
     */
    private final SilkDecoder.Control silkControl = new SilkDecoder.Control();

    /**
     * Number of channels coded in the current stream (from the TOC of the last packet).
     * <p>
     * 현재 스트림에 부호화된 채널 수(마지막 패킷의 TOC 에서 얻는다).
     */
    private int streamChannels;

    /**
     * Audio bandwidth of the last packet.
     * <p>
     * 마지막 패킷의 오디오 대역폭.
     */
    private int bandwidth;

    /**
     * Coding mode of the last packet.
     * <p>
     * 마지막 패킷의 부호화 모드.
     */
    private int mode;

    /**
     * Coding mode of the previous frame, 0 before the first frame.
     * <p>
     * 이전 프레임의 부호화 모드. 첫 프레임 전에는 0.
     */
    private int prevMode;

    /**
     * Samples per frame of the last packet.
     * <p>
     * 마지막 패킷의 프레임당 샘플 수.
     */
    private int frameSize;

    /**
     * Whether the previous frame ended with a redundancy frame that is to be faded out.
     * <p>
     * 이전 프레임이 페이드 아웃할 중복 프레임으로 끝났는지.
     */
    private boolean prevRedundancy;

    /**
     * Final range coder state of the last frame (XOR of the range decoder and the redundancy range decoder).
     * <p>
     * 마지막 프레임의 최종 범위 디코더 상태(범위 디코더와 중복 프레임 범위 디코더의 XOR).
     */
    private int rangeFinal;

    /**
     * Creates a decoder.
     * <p>
     * 디코더를 만든다.
     *
     * @param channels number of output channels, 1 or 2 / 출력 채널 수(1 또는 2)
     */
    OpusFrameDecoder(int channels) {
        if (channels < 1 || channels > 2) {
            throw new IllegalArgumentException("channels");
        }
        this.channels = channels;
        this.celt = new CeltDecoder(channels);
        silkControl.apiSampleRate = FS;
        silkControl.nChannelsApi = channels;
        reset();
    }

    /**
     * Resets the decoder to the state of a new decoder.
     * <p>
     * 디코더를 새 디코더의 상태로 되돌린다.
     */
    void reset() {
        celt.reset();
        celt.setStartBand(0);
        celt.setEndBand(21);
        silk.init();
        streamChannels = channels;
        prevMode = 0;
        mode = 0;
        frameSize = FS / 400;
        prevRedundancy = false;
        rangeFinal = 0;
        bandwidth = 0;
    }

    /**
     * Selects the RFC 6716 behavior for the folding of hybrid frames (the default is the corrected behavior of RFC 8251 section 9).
     * <p>
     * 하이브리드 프레임의 접기에 RFC 6716 동작을 고른다(기본은 RFC 8251 9장의 수정된 동작).
     *
     * @param legacy true for the RFC 6716 behavior / RFC 6716 동작이면 true
     */
    void setLegacyFolding(boolean legacy) {
        celt.setLegacyFolding(legacy);
    }

    /**
     * Returns the final range coder state of the last decoded frame.
     * <p>
     * 마지막으로 디코딩한 프레임의 최종 범위 디코더 상태를 반환한다.
     *
     * @return the 32-bit state, 0 for a lost frame / 32비트 상태. 손실 프레임이면 0
     */
    int getFinalRange() {
        return rangeFinal;
    }

    /**
     * Returns the number of output channels.
     * <p>
     * 출력 채널 수를 반환한다.
     *
     * @return 1 or 2 / 1 또는 2
     */
    int getChannels() {
        return channels;
    }

    /**
     * Cross-fades two signals over the overlap window.
     * <p>
     * 겹침 창으로 두 신호를 교차 페이드한다.
     *
     * @param in1     fade-out signal / 사라지는 신호
     * @param in1Off  start of the first signal / 첫 신호의 시작
     * @param in2     fade-in signal / 나타나는 신호
     * @param in2Off  start of the second signal / 둘째 신호의 시작
     * @param out     output (may alias an input) / 출력(입력과 같은 배열일 수 있다)
     * @param outOff  start of the output / 출력의 시작
     * @param overlap number of samples per channel / 채널당 샘플 수
     */
    private void smoothFade(float[] in1, int in1Off, float[] in2, int in2Off, float[] out, int outOff, int overlap) {
        float[] window = CeltDecoder.getWindow();
        // 채널과 샘플마다 창의 제곱을 가중치로 섞는다.
        for (int c = 0; c < channels; c++) {
            for (int i = 0; i < overlap; i++) {
                float w = window[i] * window[i];
                out[outOff + i * channels + c] = w * in2[in2Off + i * channels + c] + (1.0f - w) * in1[in1Off + i * channels + c];
            }
        }
    }

    /**
     * Decodes one packet (all its frames).
     * <p>
     * 패킷 하나(모든 프레임)를 디코딩한다.
     *
     * @param data   the buffer, or null for a lost packet / 버퍼. 손실된 패킷이면 null
     * @param off    start of the packet / 패킷의 시작
     * @param len    size of the packet in bytes / 패킷의 바이트 수
     * @param pcm    receives the interleaved samples / 인터리브된 샘플을 받는 배열
     * @param pcmOff start in <code>pcm</code> / <code>pcm</code> 의 시작
     * @param maxSamples maximum number of samples per channel that fit in <code>pcm</code> / <code>pcm</code> 에 들어가는 채널당 최대 샘플 수
     * @return the number of samples per channel, or a negative error code / 채널당 샘플 수. 오류면 음수 코드
     */
    int decodePacket(byte[] data, int off, int len, float[] pcm, int pcmOff, int maxSamples) {
        if (data == null || len == 0) {
            return decodeFrame(null, 0, 0, pcm, pcmOff, maxSamples);
        }
        if (len < 0) {
            return ERR_BAD_ARG;
        }
        OpusPacket packet = OpusPacket.parse(data, off, len);
        if (packet == null) {
            return ERR_INVALID_PACKET;
        }
        return decodeParsed(data, packet, pcm, pcmOff, maxSamples);
    }

    /**
     * Decodes a packet that was parsed before (all its frames).
     * <p>
     * 이미 파싱한 패킷(모든 프레임)을 디코딩한다.
     *
     * @param data       the buffer that the packet refers to / 패킷이 가리키는 버퍼
     * @param packet     the parsed packet / 파싱한 패킷
     * @param pcm        receives the interleaved samples / 인터리브된 샘플을 받는 배열
     * @param pcmOff     start in <code>pcm</code> / <code>pcm</code> 의 시작
     * @param maxSamples maximum number of samples per channel that fit in <code>pcm</code> / <code>pcm</code> 에 들어가는 채널당 최대 샘플 수
     * @return the number of samples per channel, or a negative error code / 채널당 샘플 수. 오류면 음수 코드
     */
    int decodeParsed(byte[] data, OpusPacket packet, float[] pcm, int pcmOff, int maxSamples) {
        int toc = packet.toc;
        mode = OpusPacket.getMode(toc);
        bandwidth = OpusPacket.getBandwidth(toc);
        frameSize = OpusPacket.getSamplesPerFrame(toc);
        streamChannels = OpusPacket.getChannels(toc);
        if (packet.count * frameSize > maxSamples) {
            return ERR_BAD_ARG;
        }
        int nbSamples = 0;
        int pos = pcmOff;
        // 프레임마다 디코딩해 출력을 이어 붙인다.
        for (int i = 0; i < packet.count; i++) {
            int ret = decodeFrame(data, packet.offsets[i], packet.sizes[i], pcm, pos, maxSamples - nbSamples);
            if (ret < 0) {
                return ret;
            }
            pos += ret * channels;
            nbSamples += ret;
        }
        return nbSamples;
    }

    /**
     * Conceals a lost packet of a given duration.
     * <p>
     * 주어진 길이의 손실된 패킷을 은닉한다.
     *
     * @param pcm     receives the interleaved samples / 인터리브된 샘플을 받는 배열
     * @param pcmOff  start in <code>pcm</code> / <code>pcm</code> 의 시작
     * @param samples number of samples per channel to produce (at least 120) / 만들 채널당 샘플 수(120 이상)
     * @return the number of samples per channel, or a negative error code / 채널당 샘플 수. 오류면 음수 코드
     */
    int decodeLost(float[] pcm, int pcmOff, int samples) {
        return decodeFrame(null, 0, 0, pcm, pcmOff, samples);
    }

    /**
     * Decodes or conceals one frame (<code>opus_decode_frame</code>).
     * <p>
     * 프레임 하나를 디코딩하거나 은닉한다(<code>opus_decode_frame</code>).
     *
     * @param dataIn     the frame, or null for a lost frame / 프레임. 손실이면 null
     * @param off        start of the frame / 프레임의 시작
     * @param lenIn      size of the frame / 프레임의 크기
     * @param pcm        receives the samples / 샘플을 받는 배열
     * @param pcmOff     start in <code>pcm</code> / <code>pcm</code> 의 시작
     * @param frameSizeIn capacity in samples per channel / 채널당 샘플 수 용량
     * @return the number of samples per channel, or an error code / 채널당 샘플 수. 오류면 코드
     */
    private int decodeFrame(byte[] dataIn, int off, int lenIn, float[] pcm, int pcmOff, int frameSizeIn) {
        byte[] data = dataIn;
        int len = lenIn;
        int frameSz = frameSizeIn;
        int f20 = FS / 50;
        int f10 = f20 >> 1;
        int f5 = f10 >> 1;
        int f25 = f5 >> 1;
        if (frameSz < f25) {
            return ERR_BAD_ARG;
        }
        if (len <= 1) {
            data = null;
            frameSz = Math.min(frameSz, frameSize);
        }
        int audiosize;
        int md;
        OpusRangeDecoder dec = null;
        if (data != null) {
            audiosize = frameSize;
            md = mode;
            dec = new OpusRangeDecoder(data, off, len);
        } else {
            audiosize = frameSz;
            if (prevMode == 0) {
                java.util.Arrays.fill(pcm, pcmOff, pcmOff + audiosize * channels, 0.f);
                return audiosize;
            }
            md = prevMode;
        }
        // 손실된 긴 프레임은 20 ms 씩 나누어 은닉한다.
        if (data == null && frameSz > f20 && md != OpusPacket.MODE_SILK_ONLY) {
            int nbSamples = 0;
            int p = pcmOff;
            // 조건이 만족되는 동안 반복해서 처리한다.
            do {
                int ret = decodeFrame(null, 0, 0, pcm, p, f20);
                if (ret != f20) {
                    return -3;
                }
                p += f20 * channels;
                nbSamples += f20;
            } while (nbSamples < frameSz);
            return frameSz;
        }
        float[] pcmTransition = new float[f5 * channels];
        boolean transition = false;
        // 코덱이 바뀌는 프레임이면 전환 구간의 신호를 따로 만들어 둔다.
        if (data != null && prevMode > 0
                && ((md == OpusPacket.MODE_CELT_ONLY && prevMode != OpusPacket.MODE_CELT_ONLY && !prevRedundancy)
                        || (md != OpusPacket.MODE_CELT_ONLY && prevMode == OpusPacket.MODE_CELT_ONLY))) {
            transition = true;
            if (md == OpusPacket.MODE_CELT_ONLY) {
                decodeFrame(null, 0, 0, pcmTransition, 0, Math.min(f5, audiosize));
            }
        }
        if (audiosize > frameSz) {
            return ERR_BAD_ARG;
        }
        frameSz = audiosize;
        short[] pcmSilk = new short[Math.max(f10, frameSz) * channels];
        float[] redundantAudio = new float[f5 * channels];
        if (md != OpusPacket.MODE_CELT_ONLY) {
            if (prevMode == OpusPacket.MODE_CELT_ONLY) {
                silk.init();
            }
            silkControl.payloadSizeMs = Math.max(10, 1000 * audiosize / FS);
            if (data != null) {
                silkControl.nChannelsInternal = streamChannels;
                if (md == OpusPacket.MODE_SILK_ONLY) {
                    if (bandwidth == OpusPacket.BANDWIDTH_NARROWBAND) {
                        silkControl.internalSampleRate = 8000;
                    } else if (bandwidth == OpusPacket.BANDWIDTH_MEDIUMBAND) {
                        silkControl.internalSampleRate = 12000;
                    } else {
                        silkControl.internalSampleRate = 16000;
                    }
                } else {
                    silkControl.internalSampleRate = 16000;
                }
            }
            int lostFlag = data == null ? 1 : 0;
            int decodedSamples = 0;
            int pos = 0;
            int[] silkFrameSize = new int[1];
            // SILK 프레임을 요청한 길이가 될 때까지 디코딩한다.
            do {
                boolean firstFrame = decodedSamples == 0;
                int silkRet = silk.decode(silkControl, lostFlag, firstFrame, dec, pcmSilk, pos, silkFrameSize);
                if (silkRet != 0) {
                    if (lostFlag != 0) {
                        silkFrameSize[0] = frameSz;
                        java.util.Arrays.fill(pcmSilk, pos, pos + frameSz * channels, (short) 0);
                    } else {
                        return ERR_INVALID_PACKET;
                    }
                }
                pos += silkFrameSize[0] * channels;
                decodedSamples += silkFrameSize[0];
            } while (decodedSamples < frameSz);
        }
        int startBand = 0;
        boolean redundancy = false;
        boolean celtToSilk = false;
        int redundancyBytes = 0;
        int redundantRng = 0;
        // 남은 비트가 충분하면 중복 CELT 프레임이 있는지 읽는다.
        if (md != OpusPacket.MODE_CELT_ONLY && data != null && dec.tell() + 17 + (mode == OpusPacket.MODE_HYBRID ? 20 : 0) <= 8 * len) {
            if (md == OpusPacket.MODE_HYBRID) {
                redundancy = dec.bitLogp(12) != 0;
            } else {
                redundancy = true;
            }
            if (redundancy) {
                celtToSilk = dec.bitLogp(1) != 0;
                redundancyBytes = md == OpusPacket.MODE_HYBRID ? (int) dec.readUint(256) + 2 : len - ((dec.tell() + 7) >> 3);
                len -= redundancyBytes;
                if (len * 8 < dec.tell()) {
                    len = 0;
                    redundancyBytes = 0;
                    redundancy = false;
                }
                dec.shrink(redundancyBytes);
            }
        }
        if (md != OpusPacket.MODE_CELT_ONLY) {
            startBand = 17;
        }
        int endband;
        switch (bandwidth) {
        case OpusPacket.BANDWIDTH_NARROWBAND:
            endband = 13;
            break;
        case OpusPacket.BANDWIDTH_MEDIUMBAND:
        case OpusPacket.BANDWIDTH_WIDEBAND:
            endband = 17;
            break;
        case OpusPacket.BANDWIDTH_SUPERWIDEBAND:
            endband = 19;
            break;
        default:
            endband = 21;
            break;
        }
        celt.setEndBand(endband);
        celt.setStreamChannels(streamChannels);
        if (redundancy) {
            transition = false;
        }
        if (transition && md != OpusPacket.MODE_CELT_ONLY) {
            decodeFrame(null, 0, 0, pcmTransition, 0, Math.min(f5, audiosize));
        }
        // 중복 프레임이 CELT 에서 SILK 로 바뀌는 쪽이면 먼저 디코딩해 둔다.
        if (redundancy && celtToSilk) {
            celt.setStartBand(0);
            celt.decode(data, off + len, redundancyBytes, redundantAudio, 0, f5, null);
            redundantRng = celt.getFinalRange();
        }
        celt.setStartBand(startBand);
        int celtRet = 0;
        if (md != OpusPacket.MODE_SILK_ONLY) {
            int celtFrameSize = Math.min(f20, frameSz);
            if (md != prevMode && prevMode > 0 && !prevRedundancy) {
                celt.reset();
            }
            celtRet = celt.decode(data, off, len, pcm, pcmOff, celtFrameSize, dec);
        } else {
            java.util.Arrays.fill(pcm, pcmOff, pcmOff + frameSz * channels, 0.f);
            // 하이브리드에서 SILK 로 바뀌면 CELT 의 꼬리를 무음 프레임으로 정리한다.
            if (prevMode == OpusPacket.MODE_HYBRID && !(redundancy && celtToSilk && prevRedundancy)) {
                celt.setStartBand(0);
                celt.decode(new byte[] { (byte) 0xFF, (byte) 0xFF }, 0, 2, pcm, pcmOff, f25, null);
            }
        }
        if (md != OpusPacket.MODE_CELT_ONLY) {
            // SILK 출력을 float 로 바꿔 CELT 출력에 더한다.
            for (int i = 0; i < frameSz * channels; i++) {
                pcm[pcmOff + i] = pcm[pcmOff + i] + (float) ((1. / 32768.) * pcmSilk[i]);
            }
        }
        // 중복 프레임이 SILK 에서 CELT 로 바뀌는 쪽이면 끝부분에서 교차 페이드한다.
        if (redundancy && !celtToSilk) {
            celt.reset();
            celt.setStartBand(0);
            celt.decode(data, off + len, redundancyBytes, redundantAudio, 0, f5, null);
            redundantRng = celt.getFinalRange();
            smoothFade(pcm, pcmOff + channels * (frameSz - f25), redundantAudio, channels * f25, pcm, pcmOff + channels * (frameSz - f25), f25);
        }
        if (redundancy && celtToSilk) {
            // 처음 2.5 ms 는 중복 프레임으로 대체하고 이어서 교차 페이드한다.
            for (int c = 0; c < channels; c++) {
                for (int i = 0; i < f25; i++) {
                    pcm[pcmOff + channels * i + c] = redundantAudio[channels * i + c];
                }
            }
            smoothFade(redundantAudio, channels * f25, pcm, pcmOff + channels * f25, pcm, pcmOff + channels * f25, f25);
        }
        if (transition) {
            if (audiosize >= f5) {
                System.arraycopy(pcmTransition, 0, pcm, pcmOff, channels * f25);
                smoothFade(pcmTransition, channels * f25, pcm, pcmOff + channels * f25, pcm, pcmOff + channels * f25, f25);
            } else {
                smoothFade(pcmTransition, 0, pcm, pcmOff, pcm, pcmOff, f25);
            }
        }
        if (len <= 1) {
            rangeFinal = 0;
        } else {
            rangeFinal = dec.getFinalRange() ^ redundantRng;
        }
        prevMode = md;
        prevRedundancy = redundancy && !celtToSilk;
        return celtRet < 0 ? celtRet : audiosize;
    }
}
