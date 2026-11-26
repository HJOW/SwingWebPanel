package org.duckdns.hjow.swingwebpanel.media.audio.ogg;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.ArrayByteSource;
import org.duckdns.hjow.swingwebpanel.media.audio.ByteSource;
import org.duckdns.hjow.swingwebpanel.media.audio.Bytes;

/**
 * Writes one logical Ogg stream to a caller-owned sink. At most one page is buffered, including across split packets.
 * <p>
 * 호출자가 소유하는 출력에 Ogg 논리 스트림 하나를 쓴다. 패킷이 여러 페이지로 나뉘어도 페이지 하나만 버퍼에 둔다.
 */
public final class OggPageWriter {

    /** Payload offset before compacting the lacing table. / 세그먼트 표를 줄이기 전 페이로드의 위치. */
    private static final int PAYLOAD_START = OggPage.FIXED_HEADER + 255;

    /** Output supplied by the caller. / 호출자가 제공한 출력. */
    private final OutputStream out;

    /** Scratch for a full header, lacing table, and payload. / 전체 헤더·세그먼트 표·페이로드의 작업 공간. */
    private final byte[] page = new byte[OggPage.MAX_PAGE_BYTES];

    /** Number of lacing segments on the page. / 현재 페이지의 세그먼트 수. */
    private int segments;

    /** Number of payload bytes on the page. / 현재 페이지의 페이로드 바이트 수. */
    private int payload;

    /** Sequence of the next page to emit. / 다음에 출력할 페이지 순서 번호. */
    private int sequence;

    /** Granule position of the last complete packet on the page. / 페이지의 마지막 완전한 패킷의 그라뉼. */
    private long granule = -1;

    /** True when the first segment continues the previous page. / 첫 세그먼트가 앞 페이지에서 이어지면 true. */
    private boolean continued;

    /**
     * Starts a stream with the given serial number. / 주어진 시리얼 번호로 스트림을 시작한다.
     * @param out the output, closed by the caller / 출력. 호출자가 닫는다
     * @param serial stream serial number / 스트림 시리얼 번호
     */
    public OggPageWriter(OutputStream out, int serial) {
        this.out = out;
        page[0] = 'O';
        page[1] = 'g';
        page[2] = 'g';
        page[3] = 'S';
        putLittleEndian(14, serial, 4);
    }

    /** Returns the buffered segment count. / 버퍼의 세그먼트 수를 반환한다. */
    public int segments() {
        return segments;
    }

    /**
     * Writes an array packet. / 배열 패킷을 쓴다.
     * @param bytes packet bytes / 패킷 바이트
     * @param endGranule its ending granule position / 이 패킷의 마지막 그라뉼
     * @throws IOException if the sink fails / 출력에 실패하면
     * @throws MediaException if the packet exceeds the decoder limit / 디코더의 패킷 제한을 넘으면
     */
    public void packet(byte[] bytes, long endGranule) throws IOException, MediaException {
        packet(new ArrayByteSource(bytes), 0, bytes.length, endGranule);
    }

    /**
     * Copies one packet in lacing segments without allocating a packet-sized buffer. / 패킷 크기의 버퍼를 할당하지 않고 세그먼트별로 패킷을 복사한다.
     * @param data the source / 원천
     * @param offset offset of the packet / 패킷 위치
     * @param length packet length / 패킷 길이
     * @param endGranule its ending granule position / 이 패킷의 마지막 그라뉼
     * @throws IOException if the sink fails / 출력에 실패하면
     * @throws MediaException if the packet range is invalid or exceeds the decoder limit / 패킷 범위가 잘못되었거나 디코더 제한을 넘으면
     */
    public void packet(ByteSource data, int offset, int length, long endGranule) throws IOException, MediaException {
        if (!Bytes.has(data, offset, length)) {
            throw new MediaException(MediaException.Kind.CORRUPT, "Ogg packet range lies outside the source");
        }
        if (length > OggPacketStream.MAX_PACKET_BYTES) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "Ogg packet exceeds the decoding limit");
        }
        int copied = 0;
        // 패킷 끝에 255보다 작은 항목을 둔다. 길이가 255의 배수면 마지막 항목은 0이다.
        for (;;) {
            if (segments == 255) {
                flush(false);
                continued = copied != 0;
            }
            int len = Math.min(255, length - copied);
            page[OggPage.FIXED_HEADER + segments++] = (byte) len;
            Bytes.readFully(data, (long) offset + copied, page, PAYLOAD_START + payload, len);
            payload += len;
            copied += len;
            if (len < 255) {
                granule = endGranule;
                return;
            }
        }
    }

    /**
     * Writes the buffered page and its checksum. / 버퍼의 페이지와 체크섬을 쓴다.
     * @param last true for the last page of the stream / 스트림의 마지막 페이지이면 true
     * @throws IOException if the sink fails / 출력에 실패하면
     */
    public void flush(boolean last) throws IOException {
        if (segments == 0) {
            return;
        }
        page[5] = (byte) ((continued ? OggPage.FLAG_CONTINUED : 0) | (sequence == 0 ? OggPage.FLAG_FIRST : 0)
                | (last ? OggPage.FLAG_LAST : 0));
        putLittleEndian(6, granule, 8);
        putLittleEndian(18, sequence, 4);
        page[26] = (byte) segments;
        int body = OggPage.FIXED_HEADER + segments;
        System.arraycopy(page, PAYLOAD_START, page, body, payload);
        Arrays.fill(page, 22, 26, (byte) 0);
        int length = body + payload;
        putLittleEndian(22, OggCrc.update(0, page, 0, length), 4);
        out.write(page, 0, length);
        sequence++;
        segments = 0;
        payload = 0;
        granule = -1;
        continued = false;
    }

    /**
     * Writes a little-endian header field. / 헤더 필드를 리틀 엔디언으로 쓴다.
     * @param at field offset / 필드 위치
     * @param value field value / 필드 값
     * @param bytes field width / 필드 길이
     */
    private void putLittleEndian(int at, long value, int bytes) {
        // 하위 바이트부터 차례로 보관한다.
        for (int i = 0; i < bytes; i++) {
            page[at + i] = (byte) (value >> (8 * i));
        }
    }
}
