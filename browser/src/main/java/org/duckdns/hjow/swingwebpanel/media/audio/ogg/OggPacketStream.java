package org.duckdns.hjow.swingwebpanel.media.audio.ogg;

import java.util.Arrays;
import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * Sequential reader of the packets of one logical Ogg bitstream. Packets are reassembled from the lacing segments of the pages; each page
 * is checked against its CRC-32 when it is read. A page that fails the check, a gap in the page sequence and a continuation without its
 * beginning (after a seek) lose the packet that is affected and the stream goes on with the next packet boundary, so one damaged page does
 * not end the stream (the number of damaged pages is available from {@link #getDamagedPages()}).
 * <p>
 * 논리 Ogg 비트스트림 하나의 패킷을 순서대로 읽는 객체. 패킷은 페이지의 래이싱 세그먼트에서 다시 조립한다. 페이지는 읽을 때마다 CRC-32 를
 * 확인한다. 확인에 실패한 페이지, 페이지 순서의 빈틈, (탐색 뒤) 시작이 없는 이어진 패킷은 해당 패킷을 잃고 다음 패킷 경계부터 계속하므로 손상된
 * 페이지 하나가 스트림을 끝내지 않는다(손상된 페이지 수는 {@link #getDamagedPages()} 로 알 수 있다).
 */
public final class OggPacketStream {

    /**
     * Largest packet that is assembled (a larger one is reported as damaged).
     * <p>
     * 조립하는 패킷의 최대 크기(이보다 크면 손상으로 보고한다).
     */
    public static final int MAX_PACKET_BYTES = 16 * 1024 * 1024;

    /**
     * The file data.
     * <p>
     * 파일 데이터.
     */
    private final byte[] data;

    /**
     * The logical stream.
     * <p>
     * 논리 스트림.
     */
    private final OggStream stream;

    /**
     * Number of the next page to load.
     * <p>
     * 다음에 읽을 페이지 번호.
     */
    private int nextPage;

    /**
     * The page being read, or null.
     * <p>
     * 읽고 있는 페이지. 없으면 null.
     */
    private OggPage page;

    /**
     * Number of the page being read.
     * <p>
     * 읽고 있는 페이지의 번호.
     */
    private int pageIndex;

    /**
     * Next segment of the current page.
     * <p>
     * 현재 페이지의 다음 세그먼트.
     */
    private int segment;

    /**
     * Offset of the next payload byte.
     * <p>
     * 다음 페이로드 바이트의 위치.
     */
    private int payloadPos;

    /**
     * The packet being assembled.
     * <p>
     * 조립 중인 패킷.
     */
    private byte[] partial = new byte[256];

    /**
     * Bytes in {@link #partial}.
     * <p>
     * {@link #partial} 의 바이트 수.
     */
    private int partialLength;

    /**
     * True while the bytes up to the next packet boundary must be dropped.
     * <p>
     * 다음 패킷 경계까지의 바이트를 버려야 하는 동안 true.
     */
    private boolean discarding;

    /**
     * Sequence number of the previous page, or -1 before the first page.
     * <p>
     * 이전 페이지의 순서 번호. 첫 페이지 전에는 -1.
     */
    private long lastSequence = -1;

    /**
     * Pages rejected because of a bad checksum.
     * <p>
     * 체크섬이 틀려 거부한 페이지 수.
     */
    private int damaged;

    /**
     * Creates the reader.
     * <p>
     * 읽기 객체를 만든다.
     *
     * @param data      file data / 파일 데이터
     * @param stream    the logical stream / 논리 스트림
     * @param startPage first page / 첫 페이지
     */
    OggPacketStream(byte[] data, OggStream stream, int startPage) {
        this.data = data;
        this.stream = stream;
        this.nextPage = Math.max(0, startPage);
    }

    /**
     * Returns the number of pages that were rejected because their CRC-32 did not match or that could not be parsed.
     * <p>
     * CRC-32 가 맞지 않거나 해석할 수 없어 거부한 페이지 수를 반환한다.
     *
     * @return damaged pages / 손상된 페이지 수
     */
    public int getDamagedPages() {
        return damaged;
    }

    /**
     * Returns the next packet, or null at the end of the stream.
     * <p>
     * 다음 패킷을 반환한다. 스트림 끝이면 null.
     *
     * @return the packet or null / 패킷 또는 null
     * @throws MediaException CANCELLED if the thread is interrupted / 스레드가 인터럽트되면 CANCELLED
     */
    public OggPacket next() throws MediaException {
        // 패킷이 완성될 때까지 페이지와 세그먼트를 차례로 읽는다.
        for (;;) {
            if (page == null && !loadPage()) {
                return null;
            }
            // 현재 페이지의 남은 세그먼트를 처리한다.
            while (segment < page.getSegmentCount()) {
                int len = data[page.getSegmentTableOffset() + segment] & 0xFF;
                segment++;
                if (!discarding) {
                    append(payloadPos, len);
                }
                payloadPos += len;
                if (len == 255) {
                    continue;
                }
                if (discarding) {
                    discarding = false;
                    partialLength = 0;
                    continue;
                }
                return finishPacket();
            }
            page = null;
        }
    }

    /**
     * Builds the packet that ended in the segment just read.
     * <p>
     * 방금 읽은 세그먼트에서 끝난 패킷을 만든다.
     *
     * @return the packet / 패킷
     */
    private OggPacket finishPacket() {
        byte[] bytes = Arrays.copyOf(partial, partialLength);
        partialLength = 0;
        boolean lastOnPage = true;
        // 이 뒤에도 패킷을 끝내는 세그먼트(255 미만)가 있으면 이 패킷은 페이지의 마지막이 아니다.
        for (int i = segment; i < page.getSegmentCount(); i++) {
            if ((data[page.getSegmentTableOffset() + i] & 0xFF) < 255) {
                lastOnPage = false;
                break;
            }
        }
        long granule = lastOnPage ? page.getGranule() : -1;
        boolean last = page.isLast() && segment == page.getSegmentCount();
        return new OggPacket(bytes, granule, last, pageIndex);
    }

    /**
     * Appends payload bytes to the packet being assembled.
     * <p>
     * 조립 중인 패킷에 페이로드 바이트를 덧붙인다.
     *
     * @param from offset of the bytes / 바이트의 위치
     * @param len  number of bytes / 바이트 수
     */
    private void append(int from, int len) {
        if (partialLength + len > MAX_PACKET_BYTES) {
            // 너무 큰 패킷은 손상으로 보고 다음 경계까지 버린다.
            partialLength = 0;
            discarding = true;
            damaged++;
            return;
        }
        if (partialLength + len > partial.length) {
            partial = Arrays.copyOf(partial, Math.max(partial.length * 2, partialLength + len));
        }
        System.arraycopy(data, from, partial, partialLength, len);
        partialLength += len;
    }

    /**
     * Loads the next valid page of the stream and prepares the continuation state.
     * <p>
     * 스트림의 다음 올바른 페이지를 읽고 이어짐 상태를 준비한다.
     *
     * @return false at the end of the stream / 스트림 끝이면 false
     * @throws MediaException CANCELLED if the thread is interrupted / 스레드가 인터럽트되면 CANCELLED
     */
    private boolean loadPage() throws MediaException {
        // 올바른 페이지를 찾을 때까지 페이지 번호를 올린다.
        while (nextPage < stream.getPageCount()) {
            if (Thread.currentThread().isInterrupted()) {
                throw new MediaException(MediaException.Kind.CANCELLED, "Ogg reading was interrupted");
            }
            int idx = nextPage++;
            OggPage p = OggPage.parse(data, stream.getPageOffset(idx));
            if (p == null || !p.isChecksumValid(data)) {
                damaged++;
                partialLength = 0;
                discarding = true;
                lastSequence = -1;
                continue;
            }
            if (lastSequence >= 0 && p.getSequence() != ((lastSequence + 1) & 0xFFFFFFFFL)) {
                partialLength = 0;
                discarding = false;
            }
            lastSequence = p.getSequence();
            if (p.isContinued()) {
                if (partialLength == 0 && !discarding) {
                    discarding = true;
                }
            } else {
                partialLength = 0;
                discarding = false;
            }
            page = p;
            pageIndex = idx;
            segment = 0;
            payloadPos = p.getPayloadOffset();
            return true;
        }
        return false;
    }
}
