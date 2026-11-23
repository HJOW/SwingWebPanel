package org.duckdns.hjow.swingwebpanel.media.audio.ogg;

/**
 * One packet of a logical Ogg bitstream, reassembled from the segments of one or more pages.
 * <p>
 * 논리 Ogg 비트스트림의 패킷 하나. 페이지 하나 이상의 세그먼트로 다시 조립한 것이다.
 */
public final class OggPacket {

    /**
     * The packet bytes.
     * <p>
     * 패킷 바이트.
     */
    private final byte[] data;

    /**
     * Granule position of the page if this is the last packet that ends on that page, else -1.
     * <p>
     * 이 패킷이 페이지에서 끝나는 마지막 패킷이면 그 페이지의 그래뉼 위치, 아니면 -1.
     */
    private final long granule;

    /**
     * True if this packet ends the logical bitstream.
     * <p>
     * 이 패킷이 논리 비트스트림을 끝내면 true.
     */
    private final boolean last;

    /**
     * Index (within the logical stream) of the page on which the packet ends.
     * <p>
     * 패킷이 끝나는 페이지의 (논리 스트림 안에서의) 번호.
     */
    private final int pageIndex;

    /**
     * Creates a packet.
     * <p>
     * 패킷을 만든다.
     *
     * @param data      packet bytes / 패킷 바이트
     * @param granule   granule position or -1 / 그래뉼 위치 또는 -1
     * @param last      true at the end of the stream / 스트림 끝이면 true
     * @param pageIndex index of the page on which the packet ends / 패킷이 끝나는 페이지 번호
     */
    OggPacket(byte[] data, long granule, boolean last, int pageIndex) {
        this.data = data;
        this.granule = granule;
        this.last = last;
        this.pageIndex = pageIndex;
    }

    /**
     * Returns the packet bytes (not copied).
     * <p>
     * 패킷 바이트를 반환한다(복사하지 않는다).
     *
     * @return bytes / 바이트
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Returns the granule position of the page if this packet is the last one that ends on it, else -1. Its meaning depends on the codec
     * (for Vorbis: the number of PCM frames up to the end of this packet).
     * <p>
     * 이 패킷이 페이지에서 끝나는 마지막 패킷이면 그 페이지의 그래뉼 위치를, 아니면 -1 을 반환한다. 의미는 코덱마다 다르다(Vorbis 는 이
     * 패킷의 끝까지의 PCM 프레임 수).
     *
     * @return granule position or -1 / 그래뉼 위치 또는 -1
     */
    public long getGranule() {
        return granule;
    }

    /**
     * Tells whether this packet is the last one of the logical bitstream (it ends on a page flagged as the end of the stream).
     * <p>
     * 이 패킷이 논리 비트스트림의 마지막 패킷인지(스트림 끝으로 표시된 페이지에서 끝나는지) 알려 준다.
     *
     * @return true for the last packet / 마지막 패킷이면 true
     */
    public boolean isLast() {
        return last;
    }

    /**
     * Returns the index, within its logical stream, of the page on which this packet ends.
     * <p>
     * 이 패킷이 끝나는 페이지의 (논리 스트림 안에서의) 번호를 반환한다.
     *
     * @return page index / 페이지 번호
     */
    public int getPageIndex() {
        return pageIndex;
    }
}
