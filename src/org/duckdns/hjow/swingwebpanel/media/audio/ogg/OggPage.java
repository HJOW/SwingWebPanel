package org.duckdns.hjow.swingwebpanel.media.audio.ogg;

import org.duckdns.hjow.swingwebpanel.media.audio.Bytes;

/**
 * One page of an Ogg physical bitstream (RFC 3533): the header fields and the position of the segment table and the payload inside the
 * file data. A page is only a view on the data array; nothing is copied.
 * <p>
 * Ogg 물리 비트스트림(RFC 3533)의 페이지 하나: 헤더 필드와 파일 데이터 안의 세그먼트 표와 페이로드의 위치. 페이지는 데이터 배열에 대한 보기일
 * 뿐이며 아무것도 복사하지 않는다.
 */
public final class OggPage {

    /**
     * Header flag: the first packet of the page continues a packet of the previous page.
     * <p>
     * 헤더 플래그: 페이지의 첫 패킷이 이전 페이지의 패킷을 이어간다.
     */
    public static final int FLAG_CONTINUED = 1;

    /**
     * Header flag: first page of a logical bitstream (beginning of stream).
     * <p>
     * 헤더 플래그: 논리 비트스트림의 첫 페이지(스트림 시작).
     */
    public static final int FLAG_FIRST = 2;

    /**
     * Header flag: last page of a logical bitstream (end of stream).
     * <p>
     * 헤더 플래그: 논리 비트스트림의 마지막 페이지(스트림 끝).
     */
    public static final int FLAG_LAST = 4;

    /**
     * Size of the fixed part of the header.
     * <p>
     * 헤더의 고정 부분 크기.
     */
    public static final int FIXED_HEADER = 27;

    /**
     * Offset of the page in the data.
     * <p>
     * 데이터 안에서의 페이지 위치.
     */
    private final int offset;

    /**
     * Header type flags.
     * <p>
     * 헤더 유형 플래그.
     */
    private final int flags;

    /**
     * Granule position, -1 if no packet ends on the page.
     * <p>
     * 그래뉼 위치. 페이지에서 끝나는 패킷이 없으면 -1.
     */
    private final long granule;

    /**
     * Serial number of the logical bitstream.
     * <p>
     * 논리 비트스트림의 시리얼 번호.
     */
    private final int serial;

    /**
     * Page sequence number.
     * <p>
     * 페이지 순서 번호.
     */
    private final long sequence;

    /**
     * Number of entries of the segment table.
     * <p>
     * 세그먼트 표의 항목 수.
     */
    private final int segments;

    /**
     * Total payload bytes.
     * <p>
     * 페이로드의 전체 바이트 수.
     */
    private final int payloadLength;

    /**
     * Creates a page view.
     * <p>
     * 페이지 보기를 만든다.
     *
     * @param offset        offset in the data / 데이터 안의 위치
     * @param flags         header flags / 헤더 플래그
     * @param granule       granule position / 그래뉼 위치
     * @param serial        serial number / 시리얼 번호
     * @param sequence      sequence number / 순서 번호
     * @param segments      segment count / 세그먼트 수
     * @param payloadLength payload size / 페이로드 크기
     */
    private OggPage(int offset, int flags, long granule, int serial, long sequence, int segments, int payloadLength) {
        this.offset = offset;
        this.flags = flags;
        this.granule = granule;
        this.serial = serial;
        this.sequence = sequence;
        this.segments = segments;
        this.payloadLength = payloadLength;
    }

    /**
     * Parses the header at an offset. The checksum is not verified here (see {@link #isChecksumValid(byte[])}).
     * <p>
     * 위치의 헤더를 해석한다. 체크섬은 여기서 확인하지 않는다({@link #isChecksumValid(byte[])} 참조).
     *
     * @param d   file data / 파일 데이터
     * @param off offset of the capture pattern <code>OggS</code> / 캡처 패턴 <code>OggS</code> 의 위치
     * @return the page, or null if there is no complete valid page header and payload at the offset / 페이지. 완전하고 올바른 헤더와 페이로드가 없으면 null
     */
    public static OggPage parse(byte[] d, int off) {
        if (!Bytes.has(d, off, FIXED_HEADER) || !Bytes.matches(d, off, "OggS") || d[off + 4] != 0 || (d[off + 5] & 0xF8) != 0) {
            return null;
        }
        int nseg = d[off + 26] & 0xFF;
        if (!Bytes.has(d, off + FIXED_HEADER, nseg)) {
            return null;
        }
        int payload = 0;
        // 세그먼트 표의 값을 모두 더해 페이로드 크기를 구한다.
        for (int i = 0; i < nseg; i++) {
            payload += d[off + FIXED_HEADER + i] & 0xFF;
        }
        if (!Bytes.has(d, off + FIXED_HEADER + nseg, payload)) {
            return null;
        }
        return new OggPage(off, d[off + 5] & 0xFF, Bytes.le64(d, off + 6), (int) Bytes.le32(d, off + 14), Bytes.le32(d, off + 18), nseg, payload);
    }

    /**
     * Verifies the CRC-32 of the page (polynomial 0x04C11DB7, computed with the checksum field zeroed).
     * <p>
     * 페이지의 CRC-32 를 확인한다(다항식 0x04C11DB7, 체크섬 필드를 0 으로 놓고 계산).
     *
     * @param d file data / 파일 데이터
     * @return true if the stored checksum matches / 저장된 체크섬이 맞으면 true
     */
    public boolean isChecksumValid(byte[] d) {
        int crc = OggCrc.update(0, d, offset, 22);
        crc = OggCrc.update(crc, ZERO4, 0, 4);
        crc = OggCrc.update(crc, d, offset + 26, getEnd() - offset - 26);
        return (crc & 0xFFFFFFFFL) == Bytes.le32(d, offset + 22);
    }

    /**
     * Four zero bytes that stand in for the checksum field.
     * <p>
     * 체크섬 필드 대신 쓰는 0 바이트 네 개.
     */
    private static final byte[] ZERO4 = new byte[4];

    /**
     * Returns the offset of the page.
     * <p>
     * 페이지의 위치를 반환한다.
     *
     * @return offset / 위치
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Returns the offset just after the page.
     * <p>
     * 페이지 바로 다음의 위치를 반환한다.
     *
     * @return end offset / 끝 위치
     */
    public int getEnd() {
        return offset + FIXED_HEADER + segments + payloadLength;
    }

    /**
     * Returns the header flags.
     * <p>
     * 헤더 플래그를 반환한다.
     *
     * @return flags / 플래그
     */
    public int getFlags() {
        return flags;
    }

    /**
     * Returns the granule position (codec specific; -1 if no packet ends on this page).
     * <p>
     * 그래뉼 위치를 반환한다(코덱마다 의미가 다르다. 이 페이지에서 끝나는 패킷이 없으면 -1).
     *
     * @return granule position / 그래뉼 위치
     */
    public long getGranule() {
        return granule;
    }

    /**
     * Returns the serial number of the logical bitstream.
     * <p>
     * 논리 비트스트림의 시리얼 번호를 반환한다.
     *
     * @return serial number / 시리얼 번호
     */
    public int getSerial() {
        return serial;
    }

    /**
     * Returns the page sequence number.
     * <p>
     * 페이지 순서 번호를 반환한다.
     *
     * @return sequence number / 순서 번호
     */
    public long getSequence() {
        return sequence;
    }

    /**
     * Returns the number of segments.
     * <p>
     * 세그먼트 수를 반환한다.
     *
     * @return segment count / 세그먼트 수
     */
    public int getSegmentCount() {
        return segments;
    }

    /**
     * Returns the offset of the segment table in the data.
     * <p>
     * 데이터 안에서 세그먼트 표의 위치를 반환한다.
     *
     * @return offset / 위치
     */
    public int getSegmentTableOffset() {
        return offset + FIXED_HEADER;
    }

    /**
     * Returns the offset of the payload in the data.
     * <p>
     * 데이터 안에서 페이로드의 위치를 반환한다.
     *
     * @return offset / 위치
     */
    public int getPayloadOffset() {
        return offset + FIXED_HEADER + segments;
    }

    /**
     * Returns the payload size.
     * <p>
     * 페이로드 크기를 반환한다.
     *
     * @return bytes / 바이트 수
     */
    public int getPayloadLength() {
        return payloadLength;
    }

    /**
     * Tells whether the page continues a packet of the previous page.
     * <p>
     * 페이지가 이전 페이지의 패킷을 이어가는지 알려 준다.
     *
     * @return true if continued / 이어가면 true
     */
    public boolean isContinued() {
        return (flags & FLAG_CONTINUED) != 0;
    }

    /**
     * Tells whether this is the first page of a logical bitstream.
     * <p>
     * 논리 비트스트림의 첫 페이지인지 알려 준다.
     *
     * @return true for the beginning of a stream / 스트림 시작이면 true
     */
    public boolean isFirst() {
        return (flags & FLAG_FIRST) != 0;
    }

    /**
     * Tells whether this is the last page of a logical bitstream.
     * <p>
     * 논리 비트스트림의 마지막 페이지인지 알려 준다.
     *
     * @return true for the end of a stream / 스트림 끝이면 true
     */
    public boolean isLast() {
        return (flags & FLAG_LAST) != 0;
    }
}
