package org.duckdns.hjow.swingwebpanel.media.audio.opus;

/**
 * The structure of an Opus packet (RFC 6716 section 3): the table-of-contents byte (coding mode, audio bandwidth, frame
 * duration, channel count, frame count code) and the splitting of the payload into frames for the frame packing codes 0 to 3
 * (one frame, two frames of equal size, two frames of different size, an arbitrary number of frames with optional padding and
 * variable sizes). Parsing rejects malformed packets: it returns null instead of throwing.
 * <p>
 * Opus 패킷의 구조(RFC 6716 3장): 목차(TOC) 바이트(부호화 모드, 오디오 대역폭, 프레임 길이, 채널 수, 프레임 수 코드)와, 프레임 묶음 코드 0~3
 * (프레임 하나, 크기가 같은 프레임 둘, 크기가 다른 프레임 둘, 임의 개수의 프레임과 선택적 패딩과 가변 크기)에 따라 페이로드를 프레임으로
 * 나누는 일. 올바르지 않은 패킷은 예외 대신 null 을 반환한다.
 */
final class OpusPacket {

    /**
     * Coding mode: SILK only.
     * <p>
     * 부호화 모드: SILK 전용.
     */
    static final int MODE_SILK_ONLY = 1000;

    /**
     * Coding mode: SILK for the low band and CELT for the high band.
     * <p>
     * 부호화 모드: 저대역은 SILK, 고대역은 CELT.
     */
    static final int MODE_HYBRID = 1001;

    /**
     * Coding mode: CELT only.
     * <p>
     * 부호화 모드: CELT 전용.
     */
    static final int MODE_CELT_ONLY = 1002;

    /**
     * Audio bandwidth: narrowband (4 kHz).
     * <p>
     * 오디오 대역폭: 협대역(4 kHz).
     */
    static final int BANDWIDTH_NARROWBAND = 1101;

    /**
     * Audio bandwidth: mediumband (6 kHz).
     * <p>
     * 오디오 대역폭: 중대역(6 kHz).
     */
    static final int BANDWIDTH_MEDIUMBAND = 1102;

    /**
     * Audio bandwidth: wideband (8 kHz).
     * <p>
     * 오디오 대역폭: 광대역(8 kHz).
     */
    static final int BANDWIDTH_WIDEBAND = 1103;

    /**
     * Audio bandwidth: super-wideband (12 kHz).
     * <p>
     * 오디오 대역폭: 초광대역(12 kHz).
     */
    static final int BANDWIDTH_SUPERWIDEBAND = 1104;

    /**
     * Audio bandwidth: fullband (20 kHz).
     * <p>
     * 오디오 대역폭: 전대역(20 kHz).
     */
    static final int BANDWIDTH_FULLBAND = 1105;

    /**
     * Largest number of samples of a packet at 48 kHz (120 ms).
     * <p>
     * 48 kHz 에서 패킷 하나의 최대 샘플 수(120 ms).
     */
    static final int MAX_PACKET_SAMPLES = 5760;

    /**
     * The table-of-contents byte.
     * <p>
     * 목차(TOC) 바이트.
     */
    final int toc;

    /**
     * Number of frames.
     * <p>
     * 프레임 수.
     */
    final int count;

    /**
     * Start of each frame in the packet buffer.
     * <p>
     * 패킷 버퍼에서 각 프레임의 시작 위치.
     */
    final int[] offsets;

    /**
     * Size of each frame in bytes.
     * <p>
     * 각 프레임의 바이트 수.
     */
    final int[] sizes;

    /**
     * Number of bytes of the buffer that the packet occupies (the whole packet, or for the self-delimited framing the part that belongs to
     * this packet).
     * <p>
     * 패킷이 차지하는 버퍼의 바이트 수(패킷 전체. 자체 구분 프레이밍이면 이 패킷에 속하는 부분).
     */
    final int length;

    /**
     * Creates a parsed packet.
     * <p>
     * 파싱한 패킷을 만든다.
     *
     * @param toc     the TOC byte / TOC 바이트
     * @param count   number of frames / 프레임 수
     * @param offsets frame offsets / 프레임 위치
     * @param sizes   frame sizes / 프레임 크기
     * @param length  bytes occupied / 차지하는 바이트 수
     */
    private OpusPacket(int toc, int count, int[] offsets, int[] sizes, int length) {
        this.toc = toc;
        this.count = count;
        this.offsets = offsets;
        this.sizes = sizes;
        this.length = length;
    }

    /**
     * Returns the coding mode of a packet.
     * <p>
     * 패킷의 부호화 모드를 반환한다.
     *
     * @param toc the TOC byte / TOC 바이트
     * @return one of the MODE constants / MODE 상수 중 하나
     */
    static int getMode(int toc) {
        if ((toc & 0x80) != 0) {
            return MODE_CELT_ONLY;
        }
        if ((toc & 0x60) == 0x60) {
            return MODE_HYBRID;
        }
        return MODE_SILK_ONLY;
    }

    /**
     * Returns the audio bandwidth of a packet.
     * <p>
     * 패킷의 오디오 대역폭을 반환한다.
     *
     * @param toc the TOC byte / TOC 바이트
     * @return one of the BANDWIDTH constants / BANDWIDTH 상수 중 하나
     */
    static int getBandwidth(int toc) {
        if ((toc & 0x80) != 0) {
            int bw = BANDWIDTH_MEDIUMBAND + ((toc >> 5) & 0x3);
            if (bw == BANDWIDTH_MEDIUMBAND) {
                bw = BANDWIDTH_NARROWBAND;
            }
            return bw;
        }
        if ((toc & 0x60) == 0x60) {
            return (toc & 0x10) != 0 ? BANDWIDTH_FULLBAND : BANDWIDTH_SUPERWIDEBAND;
        }
        return BANDWIDTH_NARROWBAND + ((toc >> 5) & 0x3);
    }

    /**
     * Returns the number of samples of one frame at 48 kHz.
     * <p>
     * 프레임 하나의 48 kHz 샘플 수를 반환한다.
     *
     * @param toc the TOC byte / TOC 바이트
     * @return 120, 240, 480, 960, 1920 or 2880 / 120, 240, 480, 960, 1920, 2880 중 하나
     */
    static int getSamplesPerFrame(int toc) {
        int fs = 48000;
        if ((toc & 0x80) != 0) {
            int a = (toc >> 3) & 0x3;
            return (fs << a) / 400;
        }
        if ((toc & 0x60) == 0x60) {
            return (toc & 0x08) != 0 ? fs / 50 : fs / 100;
        }
        int a = (toc >> 3) & 0x3;
        if (a == 3) {
            return fs * 60 / 1000;
        }
        return (fs << a) / 100;
    }

    /**
     * Returns the number of channels coded in a packet.
     * <p>
     * 패킷에 부호화된 채널 수를 반환한다.
     *
     * @param toc the TOC byte / TOC 바이트
     * @return 1 or 2 / 1 또는 2
     */
    static int getChannels(int toc) {
        return (toc & 0x4) != 0 ? 2 : 1;
    }

    /**
     * Reads a frame size field (one or two bytes).
     * <p>
     * 프레임 크기 필드(1 또는 2 바이트)를 읽는다.
     *
     * @param data the buffer / 버퍼
     * @param pos  index of the field / 필드의 위치
     * @param len  remaining bytes / 남은 바이트 수
     * @param out  receives the size at index 0 (-1 if invalid) / 인덱스 0 에 크기를 받는 배열(올바르지 않으면 -1)
     * @return the number of bytes of the field, or -1 / 필드의 바이트 수. 올바르지 않으면 -1
     */
    private static int parseSize(byte[] data, int pos, int len, int[] out) {
        if (len < 1) {
            out[0] = -1;
            return -1;
        }
        int b0 = data[pos] & 0xFF;
        if (b0 < 252) {
            out[0] = b0;
            return 1;
        }
        if (len < 2) {
            out[0] = -1;
            return -1;
        }
        out[0] = 4 * (data[pos + 1] & 0xFF) + b0;
        return 2;
    }

    /**
     * Parses a packet.
     * <p>
     * 패킷을 파싱한다.
     *
     * @param data the buffer / 버퍼
     * @param off  start of the packet / 패킷의 시작
     * @param len  size of the packet in bytes / 패킷의 바이트 수
     * @return the parsed packet, or null if it is malformed / 파싱한 패킷. 올바르지 않으면 null
     */
    static OpusPacket parse(byte[] data, int off, int len) {
        return parseImpl(data, off, len, false);
    }

    /**
     * Parses a packet in the self-delimited framing of RFC 6716 appendix B, in which the size of the last frame is coded as well so that
     * several packets can follow each other in a buffer (the streams of a multistream packet).
     * <p>
     * RFC 6716 부록 B 의 자체 구분 프레이밍으로 패킷을 파싱한다. 마지막 프레임의 크기도 부호화되어 있으므로 버퍼에 패킷 여러 개가 이어질 수 있다
     * (다중 스트림 패킷의 스트림들).
     *
     * @param data the buffer / 버퍼
     * @param off  start of the packet / 패킷의 시작
     * @param len  number of bytes available from <code>off</code> / <code>off</code> 부터 쓸 수 있는 바이트 수
     * @return the parsed packet (see {@link #length}), or null if it is malformed / 파싱한 패킷({@link #length} 참조). 올바르지 않으면 null
     */
    static OpusPacket parseSelfDelimited(byte[] data, int off, int len) {
        return parseImpl(data, off, len, true);
    }

    /**
     * Parses a packet in either framing.
     * <p>
     * 두 프레이밍 중 하나로 패킷을 파싱한다.
     *
     * @param data          the buffer / 버퍼
     * @param off           start of the packet / 패킷의 시작
     * @param len           number of bytes available / 쓸 수 있는 바이트 수
     * @param selfDelimited true for the self-delimited framing / 자체 구분 프레이밍이면 true
     * @return the parsed packet, or null if it is malformed / 파싱한 패킷. 올바르지 않으면 null
     */
    private static OpusPacket parseImpl(byte[] data, int off, int len, boolean selfDelimited) {
        if (len < 1) {
            return null;
        }
        int toc = data[off] & 0xFF;
        int framesize = getSamplesPerFrame(toc);
        int pos = off + 1;
        int remaining = len - 1;
        int lastSize = remaining;
        int count;
        int[] size = new int[48];
        int[] tmp = new int[1];
        boolean cbr = false;
        switch (toc & 0x3) {
        case 0:
            count = 1;
            break;
        case 1:
            count = 2;
            cbr = true;
            if (!selfDelimited) {
                if ((remaining & 1) != 0) {
                    return null;
                }
                size[0] = lastSize = remaining / 2;
            }
            break;
        case 2: {
            count = 2;
            int bytes = parseSize(data, pos, remaining, tmp);
            size[0] = tmp[0];
            remaining -= bytes;
            if (size[0] < 0 || size[0] > remaining) {
                return null;
            }
            pos += bytes;
            lastSize = remaining - size[0];
            break;
        }
        default: {
            if (remaining < 1) {
                return null;
            }
            int ch = data[pos++] & 0xFF;
            count = ch & 0x3F;
            if (count <= 0 || framesize * count > MAX_PACKET_SAMPLES) {
                return null;
            }
            remaining--;
            if ((ch & 0x40) != 0) {
                int p;
                // 패딩 길이 바이트를 255 가 아닌 값이 나올 때까지 읽으며 남은 길이에서 바로 뺀다(RFC 8251 4장: 정수 넘침 방지).
                do {
                    if (remaining <= 0) {
                        return null;
                    }
                    p = data[pos++] & 0xFF;
                    remaining--;
                    remaining -= p == 255 ? 254 : p;
                } while (p == 255);
            }
            if (remaining < 0) {
                return null;
            }
            cbr = (ch & 0x80) == 0;
            if (!cbr) {
                lastSize = remaining;
                // 마지막을 제외한 프레임마다 크기 필드를 읽는다.
                for (int i = 0; i < count - 1; i++) {
                    int bytes = parseSize(data, pos, remaining, tmp);
                    size[i] = tmp[0];
                    remaining -= bytes;
                    if (size[i] < 0 || size[i] > remaining) {
                        return null;
                    }
                    pos += bytes;
                    lastSize -= bytes + size[i];
                }
                if (lastSize < 0) {
                    return null;
                }
            } else if (!selfDelimited) {
                lastSize = remaining / count;
                if (lastSize * count != remaining) {
                    return null;
                }
                // i 를 0 부터 count - 1 까지 늘려 가며 항목마다 처리한다.
                for (int i = 0; i < count - 1; i++) {
                    size[i] = lastSize;
                }
            }
            break;
        }
        }
        if (selfDelimited) {
            int bytes = parseSize(data, pos, remaining, tmp);
            size[count - 1] = tmp[0];
            remaining -= bytes;
            if (size[count - 1] < 0 || size[count - 1] > remaining) {
                return null;
            }
            pos += bytes;
            if (cbr) {
                if (size[count - 1] * count > remaining) {
                    return null;
                }
                // i 를 0 부터 count - 1 까지 늘려 가며 항목마다 처리한다.
                for (int i = 0; i < count - 1; i++) {
                    size[i] = size[count - 1];
                }
            } else if (size[count - 1] > lastSize) {
                return null;
            }
        } else {
            if (lastSize > 1275) {
                return null;
            }
            size[count - 1] = lastSize;
        }
        int[] offsets = new int[count];
        int[] sizes = new int[count];
        int p = pos;
        // 프레임마다 시작 위치를 누적한다.
        for (int i = 0; i < count; i++) {
            offsets[i] = p;
            sizes[i] = size[i];
            p += size[i];
        }
        if (p > off + len) {
            return null;
        }
        return new OpusPacket(toc, count, offsets, sizes, selfDelimited ? p - off : len);
    }
}
