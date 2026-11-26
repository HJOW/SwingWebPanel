package org.duckdns.hjow.swingwebpanel.media.audio.opus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.ArrayByteSource;
import org.duckdns.hjow.swingwebpanel.media.audio.ByteSource;
import org.duckdns.hjow.swingwebpanel.media.audio.ogg.OggPageWriter;
import org.duckdns.hjow.swingwebpanel.media.audio.webm.WebmFile;

/**
 * Wraps the Opus track of a WebM/Matroska file into an Ogg Opus stream so that the Ogg Opus decoder can play it: the
 * CodecPrivate of the track is the identification header (<code>OpusHead</code>, RFC 7845 section 5.1; it is rebuilt from the track
 * channels and the CodecDelay if it is missing or damaged), every block is one audio packet, and the granule positions of the pages
 * count the decoded samples, with the DiscardPadding of the last block trimmed from the end. The pre-skip comes from the header
 * (the Matroska CodecDelay is its equivalent in nanoseconds). The file bytes are copied once into the Ogg data.
 * <p>
 * WebM/Matroska 파일의 Opus 트랙을 Ogg Opus 스트림으로 감싸서 Ogg Opus 디코더가 재생하게 한다: 트랙의 CodecPrivate 는 식별 헤더
 * (<code>OpusHead</code>, RFC 7845 5.1. 없거나 손상되었으면 트랙의 채널 수와 CodecDelay 로 다시 만든다)이고, 블록 하나가 오디오 패킷 하나이며,
 * 페이지의 그래뉼 위치는 디코딩한 샘플 수를 세고 마지막 블록의 DiscardPadding 은 끝에서 자른다. 사전 건너뛰기는 헤더에서 오며(Matroska 의
 * CodecDelay 는 그 나노초 표현이다) 파일 바이트는 Ogg 데이터로 한 번 복사한다.
 */
public final class OpusInOgg {


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
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            write(t, new ArrayByteSource(data), out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new MediaException(MediaException.Kind.ACCESS_FAILED, "could not write derived Ogg data", e);
        }
    }

    /**
     * Writes an Opus track into an Ogg stream without retaining the encoded track.
     * <p>
     * 인코딩 트랙 전체를 보관하지 않고 Opus 트랙을 Ogg 스트림으로 쓴다.
     * @param t audio track / 오디오 트랙
     * @param data source of its WebM packets / WebM 패킷의 원천
     * @param out output supplied by the caller / 호출자가 제공한 출력
     * @throws MediaException if there are no packets or a packet is invalid / 패킷이 없거나 잘못되었으면
     * @throws IOException if writing fails / 쓰기에 실패하면
     */
    public static void write(WebmFile.Track t, ByteSource data, OutputStream out) throws MediaException, IOException {
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
        OggPageWriter w = new OggPageWriter(out, 0x4F505553);
        w.packet(head, 0);
        w.flush(false);
        byte[] tags = { 'O', 'p', 'u', 's', 'T', 'a', 'g', 's', 0, 0, 0, 0, 0, 0, 0, 0 };
        w.packet(tags, 0);
        w.flush(false);
        long decoded = 0;
        int dur = 960;
        // 블록마다 패킷으로 쓰고 길이를 더해 그래뉼 위치를 쌓는다.
        for (int i = 0; i < count; i++) {
            if (Thread.currentThread().isInterrupted()) {
                throw new MediaException(MediaException.Kind.CANCELLED, "Opus remux was interrupted");
            }
            int off = t.getOffset(i);
            int size = t.getSize(i);
            dur = duration(data, off, size, dur);
            decoded += dur;
            boolean last = i == count - 1;
            long granule = decoded;
            if (last && t.getDiscardPadding() > 0) {
                granule = Math.max(0, decoded - Math.round(t.getDiscardPadding() * 48000.0 / 1e9));
            }
            w.packet(data, off, size, granule);
            // 페이지가 80 개 세그먼트 이상이거나 마지막 패킷이면 페이지를 내보낸다.
            if (w.segments() >= 80 || last) {
                w.flush(last);
            }
        }
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
    private static int duration(ByteSource d, int off, int size, int fallback) {
        if (size < 1) {
            return fallback;
        }
        int toc = d.get(off);
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
            count = d.get(off + 1) & 0x3F;
        }
        int n = per * count;
        return count < 1 || n > OpusPacket.MAX_PACKET_SAMPLES ? fallback : n;
    }

}
