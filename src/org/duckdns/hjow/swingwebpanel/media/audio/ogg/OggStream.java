package org.duckdns.hjow.swingwebpanel.media.audio.ogg;

import java.util.Arrays;
import org.duckdns.hjow.swingwebpanel.media.audio.Bytes;

/**
 * One logical bitstream of an Ogg file: its serial number, the codec recognized from its first packet and an index of its pages (offset,
 * granule position, flags) that makes seeking possible without reading the whole file again. Use {@link #openPackets(int)} to read its
 * packets.
 * <p>
 * Ogg 파일의 논리 비트스트림 하나: 시리얼 번호, 첫 패킷으로 알아낸 코덱, 그리고 파일 전체를 다시 읽지 않고 탐색할 수 있게 하는 페이지 색인(위치,
 * 그래뉼 위치, 플래그). 패킷은 {@link #openPackets(int)} 로 읽는다.
 */
public final class OggStream {

    /**
     * The codec name for streams that are not recognized.
     * <p>
     * 알아보지 못한 스트림의 코덱 이름.
     */
    public static final String UNKNOWN = "unknown";

    /**
     * The file data.
     * <p>
     * 파일 데이터.
     */
    private final byte[] data;

    /**
     * Serial number.
     * <p>
     * 시리얼 번호.
     */
    private final int serial;

    /**
     * Codec name.
     * <p>
     * 코덱 이름.
     */
    private String codec = UNKNOWN;

    /**
     * Bytes of the first packet, or null if it does not fit the first page.
     * <p>
     * 첫 패킷의 바이트. 첫 페이지에 다 들어가지 않으면 null.
     */
    private byte[] firstPacket;

    /**
     * Offsets of the pages in the data.
     * <p>
     * 데이터 안에서 페이지의 위치.
     */
    private int[] offsets = new int[32];

    /**
     * Granule positions of the pages.
     * <p>
     * 페이지의 그래뉼 위치.
     */
    private long[] granules = new long[32];

    /**
     * Number of pages.
     * <p>
     * 페이지 수.
     */
    private int count;

    /**
     * True once a page flagged as the end of the stream was added.
     * <p>
     * 스트림 끝으로 표시된 페이지를 추가한 뒤에는 true.
     */
    private boolean ended;

    /**
     * Creates an empty index.
     * <p>
     * 빈 색인을 만든다.
     *
     * @param data   file data / 파일 데이터
     * @param serial serial number / 시리얼 번호
     */
    OggStream(byte[] data, int serial) {
        this.data = data;
        this.serial = serial;
    }

    /**
     * Adds the next page of this stream to the index.
     * <p>
     * 이 스트림의 다음 페이지를 색인에 추가한다.
     *
     * @param p the page / 페이지
     */
    void addPage(OggPage p) {
        if (count == offsets.length) {
            offsets = Arrays.copyOf(offsets, count * 2);
            granules = Arrays.copyOf(granules, count * 2);
        }
        if (count == 0 && p.isFirst()) {
            readFirstPacket(p);
        }
        offsets[count] = p.getOffset();
        granules[count] = p.getGranule();
        count++;
        if (p.isLast()) {
            ended = true;
        }
    }

    /**
     * Reads the first packet of the beginning-of-stream page and recognizes the codec.
     * <p>
     * 스트림 시작 페이지의 첫 패킷을 읽고 코덱을 알아본다.
     *
     * @param p the first page / 첫 페이지
     */
    private void readFirstPacket(OggPage p) {
        int len = 0;
        boolean complete = false;
        // 첫 패킷을 이루는 세그먼트의 길이를 더한다(255 미만인 세그먼트에서 끝난다).
        for (int i = 0; i < p.getSegmentCount(); i++) {
            int s = data[p.getSegmentTableOffset() + i] & 0xFF;
            len += s;
            if (s < 255) {
                complete = true;
                break;
            }
        }
        if (complete) {
            firstPacket = Arrays.copyOfRange(data, p.getPayloadOffset(), p.getPayloadOffset() + len);
            codec = detectCodec(firstPacket, 0, len);
        }
    }

    /**
     * Recognizes a codec from the first packet of a logical stream.
     * <p>
     * 논리 스트림의 첫 패킷으로 코덱을 알아본다.
     *
     * @param d   data / 데이터
     * @param off offset of the packet / 패킷의 위치
     * @param len length of the packet / 패킷의 길이
     * @return "vorbis", "opus", "flac", "speex", "theora", "skeleton" or {@link #UNKNOWN} / "vorbis", "opus", "flac", "speex", "theora", "skeleton" 또는 {@link #UNKNOWN}
     */
    public static String detectCodec(byte[] d, int off, int len) {
        if (len >= 7 && d[off] == 1 && Bytes.matches(d, off + 1, "vorbis")) {
            return "vorbis";
        }
        if (len >= 8 && Bytes.matches(d, off, "OpusHead")) {
            return "opus";
        }
        if (len >= 5 && (d[off] & 0xFF) == 0x7F && Bytes.matches(d, off + 1, "FLAC")) {
            return "flac";
        }
        if (len >= 8 && Bytes.matches(d, off, "Speex   ")) {
            return "speex";
        }
        if (len >= 7 && (d[off] & 0xFF) == 0x80 && Bytes.matches(d, off + 1, "theora")) {
            return "theora";
        }
        if (len >= 8 && Bytes.matches(d, off, "fishead\0")) {
            return "skeleton";
        }
        return UNKNOWN;
    }

    /**
     * Returns the serial number.
     * <p>
     * 시리얼 번호를 반환한다.
     *
     * @return serial number / 시리얼 번호
     */
    public int getSerial() {
        return serial;
    }

    /**
     * Returns the codec name recognized from the first packet.
     * <p>
     * 첫 패킷으로 알아낸 코덱 이름을 반환한다.
     *
     * @return codec name / 코덱 이름
     */
    public String getCodec() {
        return codec;
    }

    /**
     * Returns a copy of the first packet (the identification header of most codecs), or null.
     * <p>
     * 첫 패킷(대부분의 코덱에서 식별 헤더)의 복사본을 반환한다. 없으면 null.
     *
     * @return first packet or null / 첫 패킷 또는 null
     */
    public byte[] getFirstPacket() {
        return firstPacket == null ? null : firstPacket.clone();
    }

    /**
     * Returns the number of pages of this stream found in the file.
     * <p>
     * 파일에서 찾은 이 스트림의 페이지 수를 반환한다.
     *
     * @return page count / 페이지 수
     */
    public int getPageCount() {
        return count;
    }

    /**
     * Returns the offset of a page in the file data.
     * <p>
     * 파일 데이터 안에서 페이지의 위치를 반환한다.
     *
     * @param index page number within this stream / 이 스트림 안에서의 페이지 번호
     * @return offset / 위치
     */
    public int getPageOffset(int index) {
        return offsets[index];
    }

    /**
     * Tells whether the first packet of a page continues a packet of the previous page.
     * <p>
     * 페이지의 첫 패킷이 이전 페이지의 패킷을 이어가는지 알려 준다.
     *
     * @param index page number within this stream / 이 스트림 안에서의 페이지 번호
     * @return true if the page is a continuation / 이어지는 페이지이면 true
     */
    public boolean isPageContinued(int index) {
        return (data[offsets[index] + 5] & OggPage.FLAG_CONTINUED) != 0;
    }

    /**
     * Returns the granule position of a page (-1 if no packet ends on it).
     * <p>
     * 페이지의 그래뉼 위치를 반환한다(끝나는 패킷이 없으면 -1).
     *
     * @param index page number within this stream / 이 스트림 안에서의 페이지 번호
     * @return granule position / 그래뉼 위치
     */
    public long getPageGranule(int index) {
        return granules[index];
    }

    /**
     * Returns the granule position of the last page that has one, or -1.
     * <p>
     * 그래뉼 위치가 있는 마지막 페이지의 그래뉼 위치를 반환한다. 없으면 -1.
     *
     * @return granule position / 그래뉼 위치
     */
    public long getLastGranule() {
        // 뒤에서부터 그래뉼 위치가 있는 페이지를 찾는다.
        for (int i = count - 1; i >= 0; i--) {
            if (granules[i] >= 0) {
                return granules[i];
            }
        }
        return -1;
    }

    /**
     * Tells whether a page flagged as the end of the stream was found.
     * <p>
     * 스트림 끝으로 표시된 페이지를 찾았는지 알려 준다.
     *
     * @return true if the stream is properly ended / 스트림이 제대로 끝나 있으면 true
     */
    public boolean isEnded() {
        return ended;
    }

    /**
     * Returns the number of the first page whose granule position is greater than the given one, or the page count if there is none.
     * Pages without a granule position are skipped.
     * <p>
     * 그래뉼 위치가 주어진 값보다 큰 첫 페이지의 번호를 반환한다. 없으면 페이지 수를 반환한다. 그래뉼 위치가 없는 페이지는 건너뛴다.
     *
     * @param granule granule position / 그래뉼 위치
     * @return page number / 페이지 번호
     */
    public int findPageAfterGranule(long granule) {
        // 그래뉼 위치가 있는 페이지 중 처음으로 값보다 커지는 페이지를 앞에서부터 찾는다.
        for (int i = 0; i < count; i++) {
            if (granules[i] > granule) {
                return i;
            }
        }
        return count;
    }

    /**
     * Opens a sequential reader of the packets of this stream, starting at a page. A packet that continues from the page before the start
     * page is skipped.
     * <p>
     * 이 스트림의 패킷을 순서대로 읽는 읽기 객체를 연다. 지정한 페이지에서 시작하며 시작 페이지 앞에서 이어 오는 패킷은 건너뛴다.
     *
     * @param startPage page number within this stream (0 for the beginning) / 이 스트림 안에서의 페이지 번호(처음이면 0)
     * @return the packet reader / 패킷 읽기 객체
     */
    public OggPacketStream openPackets(int startPage) {
        return new OggPacketStream(data, this, startPage);
    }

    /**
     * Returns the file data (shared, do not modify).
     * <p>
     * 파일 데이터를 반환한다(공유되므로 수정하지 않는다).
     *
     * @return data / 데이터
     */
    byte[] getData() {
        return data;
    }
}
