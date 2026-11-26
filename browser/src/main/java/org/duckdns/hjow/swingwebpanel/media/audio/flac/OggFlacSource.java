package org.duckdns.hjow.swingwebpanel.media.audio.flac;

import java.util.LinkedList;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.ByteSource;
import org.duckdns.hjow.swingwebpanel.media.audio.ogg.OggPacket;
import org.duckdns.hjow.swingwebpanel.media.audio.ogg.OggPacketStream;
import org.duckdns.hjow.swingwebpanel.media.audio.ogg.OggStream;

/**
 * Source of FLAC in Ogg (the Ogg FLAC mapping): every audio packet is one FLAC frame, the granule position of a page is the number of
 * samples up to the end of the last packet that ends on it. Seeking reads from a page before the wanted sample and derives the sample
 * number of the first packet from the granule position and the block sizes of the packets of that page; the position is exact.
 * <p>
 * Ogg 안의 FLAC(Ogg FLAC 매핑)의 소스: 오디오 패킷 하나가 FLAC 프레임 하나이며 페이지의 그래뉼 위치는 그 페이지에서 끝나는 마지막 패킷의 끝까지의
 * 샘플 수이다. 탐색은 원하는 샘플 앞의 페이지에서 읽기 시작하고, 그 페이지의 그래뉼 위치와 패킷들의 블록 크기로 첫 패킷의 샘플 번호를 구한다. 위치는
 * 정확하다.
 */
final class OggFlacSource extends FlacPcmSource {

    /**
     * The logical stream.
     * <p>
     * 논리 스트림.
     */
    private final OggStream stream;

    /**
     * Number of the page on which the last header packet ends.
     * <p>
     * 마지막 헤더 패킷이 끝나는 페이지 번호.
     */
    private final int headerPage;

    /**
     * The packet reader.
     * <p>
     * 패킷 읽기 객체.
     */
    private OggPacketStream packets;

    /**
     * Packets that were read ahead while seeking.
     * <p>
     * 탐색하며 미리 읽어 둔 패킷.
     */
    private final LinkedList<OggPacket> queue = new LinkedList<OggPacket>();

    /**
     * Creates the source after the header packets were read.
     * <p>
     * 헤더 패킷을 읽은 뒤에 소스를 만든다.
     *
     * @param data       the encoded input / 인코딩 입력
     * @param stream     the logical stream / 논리 스트림
     * @param info       stream parameters / 스트림 파라미터
     * @param headerPage page on which the last header packet ends / 마지막 헤더 패킷이 끝나는 페이지
     * @param packets    reader positioned at the first audio packet / 첫 오디오 패킷에 위치한 읽기 객체
     * @param first      the first audio packet that was already read, or null / 이미 읽은 첫 오디오 패킷. 없으면 null
     */
    OggFlacSource(ByteSource data, OggStream stream, FlacStreamInfo info, int headerPage, OggPacketStream packets, OggPacket first) {
        super(data, info, info.totalSamples > 0 ? info.totalSamples : stream.getLastGranule());
        this.stream = stream;
        this.headerPage = headerPage;
        this.packets = packets;
        if (first != null) {
            queue.add(first);
        }
    }

    /**
     * Returns the next packet.
     * <p>
     * 다음 패킷을 반환한다.
     *
     * @return the packet or null at the end / 패킷. 끝이면 null
     * @throws MediaException on a reading failure / 읽기 실패 시
     */
    private OggPacket nextPacket() throws MediaException {
        if (!queue.isEmpty()) {
            return queue.removeFirst();
        }
        return packets.next();
    }

    /**
     * Decodes the next audio packet.
     * <p>
     * 다음 오디오 패킷을 디코딩한다.
     *
     * @return the header of the decoded frame or null at the end / 디코딩한 프레임의 헤더. 끝이면 null
     * @throws MediaException CORRUPT for an invalid frame / 프레임이 올바르지 않으면 CORRUPT
     */
    FlacFrameDecoder.Header nextFrame() throws MediaException {
        // 오디오 프레임이 아닌 패킷(뒤늦은 메타데이터)은 건너뛰며 다음 패킷을 찾는다.
        for (;;) {
            OggPacket p = nextPacket();
            if (p == null) {
                return null;
            }
            byte[] d = p.getData();
            if (d.length < 2 || !FlacFrameDecoder.isSync(d[0], d[1])) {
                continue;
            }
            try {
                FlacFrameDecoder.Header h = decoder.parseHeader(d, 0, d.length);
                if (h == null) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "invalid FLAC frame header in an Ogg packet");
                }
                decoder.decodeFrame(d, 0, d.length, h);
                return h;
            } catch (FlacFrameDecoder.Truncated t) {
                throw new MediaException(MediaException.Kind.CORRUPT, "an Ogg packet holds an incomplete FLAC frame");
            }
        }
    }

    /**
     * Returns whether this source can seek.
     * <p>
     * 이 소스가 탐색할 수 있는지 반환한다.
     *
     * @return always true / 항상 true
     */
    protected boolean canSeek() {
        return true;
    }

    /**
     * Positions the reader at a page before the wanted sample.
     * <p>
     * 원하는 샘플 앞의 페이지에 읽기 객체를 둔다.
     *
     * @param frame the wanted sample / 원하는 샘플
     * @return the sample number of the first packet that will be decoded / 디코딩할 첫 패킷의 샘플 번호
     * @throws MediaException on a reading failure / 읽기 실패 시
     */
    protected long seekTo(long frame) throws MediaException {
        queue.clear();
        int k = stream.findPageAfterGranule(frame);
        int start = Math.max(0, Math.min(k, stream.getPageCount() - 1) - 1);
        // 이어지는 페이지에서 시작하면 첫 패킷의 앞부분이 없으므로 이어지지 않는 페이지까지 거슬러 올라간다.
        while (start > headerPage && stream.isPageContinued(start)) {
            start--;
        }
        if (start <= headerPage) {
            rewind();
            return 0;
        }
        OggPacketStream reader = stream.openPackets(start);
        long granule = -1;
        long total = 0;
        // 그래뉼 위치를 아는 패킷이 나올 때까지 오디오 패킷을 모으며 블록 크기를 더한다.
        for (;;) {
            OggPacket p = reader.next();
            if (p == null) {
                break;
            }
            byte[] d = p.getData();
            if (d.length < 2 || !FlacFrameDecoder.isSync(d[0], d[1])) {
                continue;
            }
            FlacFrameDecoder.Header h;
            try {
                h = decoder.parseHeader(d, 0, d.length);
            } catch (FlacFrameDecoder.Truncated t) {
                h = null;
            }
            if (h == null) {
                continue;
            }
            queue.add(p);
            total += h.blockSize;
            if (p.getGranule() >= 0) {
                granule = p.getGranule();
                break;
            }
        }
        packets = reader;
        if (granule < 0) {
            queue.clear();
            rewind();
            return 0;
        }
        return Math.max(0, granule - total);
    }

    /**
     * Restarts at the first audio packet.
     * <p>
     * 첫 오디오 패킷에서 다시 시작한다.
     *
     * @throws MediaException on a reading failure / 읽기 실패 시
     */
    private void rewind() throws MediaException {
        packets = stream.openPackets(0);
        // 헤더 패킷들(오디오 프레임으로 시작하지 않는 패킷)을 건너뛴다.
        for (;;) {
            OggPacket p = packets.next();
            if (p == null) {
                return;
            }
            byte[] d = p.getData();
            if (d.length >= 2 && FlacFrameDecoder.isSync(d[0], d[1])) {
                queue.add(p);
                return;
            }
        }
    }
}
