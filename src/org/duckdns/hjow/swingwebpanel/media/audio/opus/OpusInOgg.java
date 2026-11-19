package org.duckdns.hjow.swingwebpanel.media.audio.opus;

import java.io.ByteArrayOutputStream;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.webm.WebmFile;

/**
 * Wraps the Opus track of a WebM/Matroska file into an in-memory Ogg Opus stream so that the Ogg Opus decoder can play it: the
 * CodecPrivate of the track is the identification header (<code>OpusHead</code>, RFC 7845 section 5.1; it is rebuilt from the track
 * channels and the CodecDelay if it is missing or damaged), every block is one audio packet, and the granule positions of the pages
 * count the decoded samples, with the DiscardPadding of the last block trimmed from the end. The pre-skip comes from the header
 * (the Matroska CodecDelay is its equivalent in nanoseconds). The file bytes are copied once into the Ogg data.
 * <p>
 * WebM/Matroska 파일의 Opus 트랙을 메모리 안의 Ogg Opus 스트림으로 감싸서 Ogg Opus 디코더가 재생하게 한다: 트랙의 CodecPrivate 는 식별 헤더
 * (<code>OpusHead</code>, RFC 7845 5.1. 없거나 손상되었으면 트랙의 채널 수와 CodecDelay 로 다시 만든다)이고, 블록 하나가 오디오 패킷 하나이며,
 * 페이지의 그래뉼 위치는 디코딩한 샘플 수를 세고 마지막 블록의 DiscardPadding 은 끝에서 자른다. 사전 건너뛰기는 헤더에서 오며(Matroska 의
 * CodecDelay 는 그 나노초 표현이다) 파일 바이트는 Ogg 데이터로 한 번 복사한다.
 */
public final class OpusInOgg {

    /**
     * Table of the Ogg CRC-32 (polynomial 0x04C11DB7, no reflection).
     * <p>
     * Ogg CRC-32 표(다항식 0x04C11DB7, 반사 없음).
     */
    private static final int[] CRC = new int[256];

    static {
        // 표의 항목마다 8번 시프트하며 계산한다.
        for (int i = 0; i < 256; i++) {
            int r = i << 24;
            // k 를 0 부터 8 까지 늘려 가며 항목마다 처리한다.
            for (int k = 0; k < 8; k++) {
                r = (r & 0x80000000) != 0 ? (r << 1) ^ 0x04C11DB7 : r << 1;
            }
            CRC[i] = r;
        }
    }

    /**
     * Not instantiable.
     * <p>
     * 인스턴스를 만들 수 없다.
     */
    private OpusInOgg() {
    }

    /**
     * Builds the Ogg Opus data for the Opus track of a WebM file.
     * <p>
     * WebM 파일의 Opus 트랙으로 Ogg Opus 데이터를 만든다.
     *
     * @param t    the Opus track / Opus 트랙
     * @param data the WebM file bytes that the track's frame offsets refer to / 트랙의 프레임 위치가 가리키는 WebM 파일 바이트
     * @return Ogg Opus data / Ogg Opus 데이터
     * @throws MediaException CORRUPT if the track has no frames / 트랙에 프레임이 없으면 CORRUPT
     */
    public static byte[] build(WebmFile.Track t, byte[] data) throws MediaException {
        int count = t.getFrameCount();
        if (count < 1) {
            throw new MediaException(MediaException.Kind.CORRUPT, "the Opus track has no frames");
        }
        byte[] head = t.getCodecPrivate();
        boolean valid = head.length >= 19 && head[0] == 'O' && head[1] == 'p' && head[2] == 'u' && head[3] == 's' && head[4] == 'H' && head[5] == 'e'
                && head[6] == 'a' && head[7] == 'd';
        if (!valid) {
            head = synthesizeHead(t);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(data.length / 2 + 1024);
        int serial = 0x4F505553;
        PageWriter w = new PageWriter(out, serial);
        w.packet(head, 0, head.length);
        w.flush(0, true, false);
        byte[] tags = { 'O', 'p', 'u', 's', 'T', 'a', 'g', 's', 0, 0, 0, 0, 0, 0, 0, 0 };
        w.packet(tags, 0, tags.length);
        w.flush(0, false, false);
        long decoded = 0;
        int dur = 960;
        // 블록마다 패킷으로 쓰고 길이를 더해 그래뉼 위치를 쌓는다.
        for (int i = 0; i < count; i++) {
            int off = t.getOffset(i);
            int size = t.getSize(i);
            dur = duration(data, off, size, dur);
            decoded += dur;
            boolean last = i == count - 1;
            long granule = decoded;
            if (last && t.getDiscardPadding() > 0) {
                granule = Math.max(0, decoded - Math.round(t.getDiscardPadding() * 48000.0 / 1e9));
            }
            w.packet(data, off, size);
            // 페이지가 80 개 세그먼트 이상이거나 마지막 패킷이면 페이지를 내보낸다.
            if (w.segments() >= 80 || last) {
                w.flush(granule, false, last);
            }
        }
        return out.toByteArray();
    }

    /**
     * Builds an identification header from the track properties for a file whose CodecPrivate is missing or damaged.
     * <p>
     * CodecPrivate 가 없거나 손상된 파일을 위해 트랙의 속성으로 식별 헤더를 만든다.
     *
     * @param t the track / 트랙
     * @return the header packet / 헤더 패킷
     */
    private static byte[] synthesizeHead(WebmFile.Track t) {
        int channels = Math.max(1, Math.min(2, t.getChannels()));
        int preSkip = (int) Math.max(0, Math.min(65535, Math.round(t.getCodecDelay() * 48000.0 / 1e9)));
        byte[] h = new byte[19];
        byte[] magic = { 'O', 'p', 'u', 's', 'H', 'e', 'a', 'd' };
        System.arraycopy(magic, 0, h, 0, 8);
        h[8] = 1;
        h[9] = (byte) channels;
        h[10] = (byte) preSkip;
        h[11] = (byte) (preSkip >> 8);
        h[12] = (byte) (48000 & 255);
        h[13] = (byte) (48000 >> 8);
        return h;
    }

    /**
     * Duration of a packet in samples at 48 kHz, from its table-of-contents byte.
     * <p>
     * 패킷의 48 kHz 샘플 길이. 목차 바이트로 구한다.
     *
     * @param d        the bytes / 바이트
     * @param off      start of the packet / 패킷의 시작
     * @param size     size of the packet / 패킷의 크기
     * @param fallback duration to assume for an empty or invalid packet / 비었거나 올바르지 않은 패킷에 가정할 길이
     * @return the duration / 길이
     */
    private static int duration(byte[] d, int off, int size, int fallback) {
        if (size < 1) {
            return fallback;
        }
        int toc = d[off] & 0xFF;
        int per = OpusPacket.getSamplesPerFrame(toc);
        int c = toc & 3;
        int count;
        if (c == 0) {
            count = 1;
        } else if (c != 3) {
            count = 2;
        } else if (size < 2) {
            return fallback;
        } else {
            count = d[off + 1] & 0x3F;
        }
        int n = per * count;
        return count < 1 || n > OpusPacket.MAX_PACKET_SAMPLES ? fallback : n;
    }

    /**
     * Writer of Ogg pages: it collects the lacing segments of the packets and writes a page when asked.
     * <p>
     * Ogg 페이지 작성기: 패킷의 래이싱 세그먼트를 모아 두었다가 요청하면 페이지를 쓴다.
     */
    private static final class PageWriter {

        /**
         * The output.
         * <p>
         * 출력.
         */
        private final ByteArrayOutputStream out;

        /**
         * Serial number of the stream.
         * <p>
         * 스트림의 시리얼 번호.
         */
        private final int serial;

        /**
         * The segment table of the page being built.
         * <p>
         * 만들고 있는 페이지의 세그먼트 표.
         */
        private final byte[] table = new byte[255];

        /**
         * Number of segments of the page being built.
         * <p>
         * 만들고 있는 페이지의 세그먼트 수.
         */
        private int nseg;

        /**
         * Payload of the page being built.
         * <p>
         * 만들고 있는 페이지의 페이로드.
         */
        private final ByteArrayOutputStream payload = new ByteArrayOutputStream();

        /**
         * Sequence number of the next page.
         * <p>
         * 다음 페이지의 순서 번호.
         */
        private int sequence;

        /**
         * True if the next page continues a packet of the previous page.
         * <p>
         * 다음 페이지가 이전 페이지의 패킷을 이어 가면 true.
         */
        private boolean continued;

        /**
         * Creates the writer.
         * <p>
         * 작성기를 만든다.
         *
         * @param out    the output / 출력
         * @param serial serial number / 시리얼 번호
         */
        PageWriter(ByteArrayOutputStream out, int serial) {
            this.out = out;
            this.serial = serial;
        }

        /**
         * Number of segments waiting for the next page.
         * <p>
         * 다음 페이지를 기다리는 세그먼트 수.
         *
         * @return the segments / 세그먼트 수
         */
        int segments() {
            return nseg;
        }

        /**
         * Adds a packet; a page is written when the segment table is full in the middle of the packet.
         * <p>
         * 패킷을 더한다. 패킷 중간에서 세그먼트 표가 가득 차면 페이지를 쓴다.
         *
         * @param d       the bytes / 바이트
         * @param off     start of the packet / 패킷의 시작
         * @param size    size of the packet / 패킷의 크기
         */
        void packet(byte[] d, int off, int size) {
            int pos = off;
            int remaining = size;
            // 255 바이트씩 세그먼트로 나누며 표가 가득 차면 페이지를 쓴다.
            for (;;) {
                int len = Math.min(255, remaining);
                if (nseg == 255) {
                    flush(-1, false, false);
                    continued = true;
                }
                table[nseg++] = (byte) len;
                payload.write(d, pos, len);
                pos += len;
                remaining -= len;
                if (len < 255) {
                    break;
                }
            }
        }

        /**
         * Writes the page that has been collected.
         * <p>
         * 모아 둔 페이지를 쓴다.
         *
         * @param granule granule position of the page / 페이지의 그래뉼 위치
         * @param bos     true for the first page of the stream / 스트림의 첫 페이지이면 true
         * @param eos     true for the last page of the stream / 스트림의 마지막 페이지이면 true
         */
        void flush(long granule, boolean bos, boolean eos) {
            if (nseg == 0) {
                return;
            }
            byte[] body = payload.toByteArray();
            byte[] page = new byte[27 + nseg + body.length];
            page[0] = 'O';
            page[1] = 'g';
            page[2] = 'g';
            page[3] = 'S';
            page[5] = (byte) ((continued ? 1 : 0) | (bos ? 2 : 0) | (eos ? 4 : 0));
            // 그래뉼 위치를 리틀 엔디언 8 바이트로 쓴다.
            for (int i = 0; i < 8; i++) {
                page[6 + i] = (byte) (granule >> (8 * i));
            }
            // i 를 0 부터 4 까지 늘려 가며 항목마다 처리한다.
            for (int i = 0; i < 4; i++) {
                page[14 + i] = (byte) (serial >> (8 * i));
                page[18 + i] = (byte) (sequence >> (8 * i));
            }
            page[26] = (byte) nseg;
            System.arraycopy(table, 0, page, 27, nseg);
            System.arraycopy(body, 0, page, 27 + nseg, body.length);
            int crc = 0;
            // 페이지 전체의 CRC 를 구해 넣는다.
            for (int i = 0; i < page.length; i++) {
                crc = (crc << 8) ^ CRC[((crc >>> 24) ^ page[i]) & 0xFF];
            }
            // i 를 0 부터 4 까지 늘려 가며 항목마다 처리한다.
            for (int i = 0; i < 4; i++) {
                page[22 + i] = (byte) (crc >> (8 * i));
            }
            out.write(page, 0, page.length);
            sequence++;
            nseg = 0;
            payload.reset();
            continued = false;
        }
    }
}
