package org.duckdns.hjow.swingwebpanel.media.audio.testkit;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.AudioKit.Out;

/**
 * Test-only Ogg multiplexer written from RFC 3533 with a bitwise CRC (independent of the table-driven decoder): packets are laced into
 * segments and distributed over pages with a chosen number of segments per page, so packets that continue over several pages are produced.
 * <p>
 * RFC 3533 에서 직접 작성한 시험 전용 Ogg 멀티플렉서. 디코더의 표 기반 구현과 무관한 비트 단위 CRC 를 쓴다. 패킷을 세그먼트로 나누어 페이지당
 * 지정한 세그먼트 수로 나누어 담으므로 여러 페이지에 걸친 패킷도 만들어진다.
 */
public final class OggMux {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private OggMux() {
    }

    /**
     * Bitwise CRC-32 of Ogg (polynomial 0x04C11DB7, no reflection, initial value 0).
     * <p>
     * Ogg 의 비트 단위 CRC-32(다항식 0x04C11DB7, 반사 없음, 초기값 0).
     *
     * @param d data / 데이터
     * @return the checksum / 체크섬
     */
    public static long crc(byte[] d) {
        long c = 0;
        // 바이트의 비트를 앞에서부터 한 비트씩 밀어 넣는다.
        for (int i = 0; i < d.length; i++) {
            for (int bit = 7; bit >= 0; bit--) {
                long in = (d[i] >> bit) & 1;
                long top = (c >> 31) & 1;
                c = (c << 1) & 0xFFFFFFFFL;
                if ((top ^ in) != 0) {
                    c ^= 0x04C11DB7L;
                }
            }
        }
        return c;
    }

    /**
     * One lacing segment waiting to be paged.
     * <p>
     * 쪽으로 나누기를 기다리는 래이싱 세그먼트 하나.
     */
    private static final class Seg {
        /**
         * Segment bytes.
         * <p>
         * 세그먼트 바이트.
         */
        byte[] data;

        /**
         * Index of the packet this segment belongs to.
         * <p>
         * 이 세그먼트가 속한 패킷의 번호.
         */
        int packet;

        /**
         * True if this is the first segment of its packet.
         * <p>
         * 패킷의 첫 세그먼트이면 true.
         */
        boolean first;

        /**
         * True if this segment ends its packet (length below 255).
         * <p>
         * 패킷을 끝내는 세그먼트(길이가 255 미만)이면 true.
         */
        boolean ends;
    }

    /**
     * Builds the pages of one logical stream.
     * <p>
     * 논리 스트림 하나의 페이지들을 만든다.
     *
     * @param serial       serial number / 시리얼 번호
     * @param firstSeq     sequence number of the first page / 첫 페이지의 순서 번호
     * @param packets      the packets / 패킷
     * @param granules     granule position after each packet (used for the last packet that ends on a page) / 패킷마다의 그래뉼 위치(페이지에서 끝나는 마지막 패킷의 값을 쓴다)
     * @param maxSegments  segments per page, at most 255 / 페이지당 세그먼트 수(255 이하)
     * @param bos          mark the first page as the beginning of the stream / 첫 페이지를 스트림 시작으로 표시
     * @param eos          mark the last page as the end of the stream / 마지막 페이지를 스트림 끝으로 표시
     * @return the pages as one byte array / 모든 페이지를 합친 바이트 배열
     */
    public static byte[] mux(int serial, int firstSeq, byte[][] packets, long[] granules, int maxSegments, boolean bos, boolean eos) {
        List<Seg> segs = new ArrayList<Seg>();
        // 패킷마다 255 바이트씩 잘라 래이싱 세그먼트로 만든다(길이가 255 의 배수면 마지막에 0 길이 세그먼트가 붙는다).
        for (int p = 0; p < packets.length; p++) {
            byte[] d = packets[p];
            int off = 0;
            boolean first = true;
            // 세그먼트를 255 바이트씩 잘라 만든다.
            for (;;) {
                int len = Math.min(255, d.length - off);
                Seg s = new Seg();
                s.data = new byte[len];
                System.arraycopy(d, off, s.data, 0, len);
                s.packet = p;
                s.first = first;
                s.ends = len < 255;
                segs.add(s);
                off += len;
                first = false;
                if (len < 255) {
                    break;
                }
            }
        }
        Out out = new Out();
        int seq = firstSeq;
        int i = 0;
        // 페이지마다 세그먼트를 최대 개수까지 담는다.
        while (i < segs.size()) {
            int n = Math.min(maxSegments, segs.size() - i);
            Out payload = new Out();
            long granule = -1;
            boolean continued = !segs.get(i).first;
            // 세그먼트마다 길이를 표에 쓴다.
            for (int k = 0; k < n; k++) {
                Seg s = segs.get(i + k);
                payload.bytes(s.data);
                if (s.ends) {
                    granule = granules[s.packet];
                }
            }
            boolean lastPage = i + n >= segs.size();
            int flags = (continued ? 1 : 0) | (bos && seq == firstSeq ? 2 : 0) | (eos && lastPage ? 4 : 0);
            Out header = new Out();
            header.ascii("OggS").u8(0).u8(flags).le32(granule & 0xFFFFFFFFL).le32((granule >> 32) & 0xFFFFFFFFL).le32(serial & 0xFFFFFFFFL).le32(seq);
            header.le32(0).u8(n);
            // 세그먼트 표를 쓴다.
            for (int k = 0; k < n; k++) {
                header.u8(segs.get(i + k).data.length);
            }
            byte[] page = new Out().bytes(header.toByteArray()).bytes(payload.toByteArray()).toByteArray();
            long crc = crc(page);
            page[22] = (byte) crc;
            page[23] = (byte) (crc >> 8);
            page[24] = (byte) (crc >> 16);
            page[25] = (byte) (crc >> 24);
            out.bytes(page);
            seq++;
            i += n;
        }
        return out.toByteArray();
    }

    /**
     * Counts the pages in muxed data.
     * <p>
     * 멀티플렉싱한 데이터의 페이지 수를 센다.
     *
     * @param d pages / 페이지들
     * @return number of pages / 페이지 수
     */
    public static int pageCount(byte[] d) {
        int pos = 0;
        int n = 0;
        // 페이지 헤더의 세그먼트 표로 다음 페이지의 위치를 구한다.
        while (pos + 27 <= d.length) {
            int nseg = d[pos + 26] & 0xFF;
            int payload = 0;
            // 세그먼트 표의 값을 더해 페이로드 크기를 구한다.
            for (int i = 0; i < nseg; i++) {
                payload += d[pos + 27 + i] & 0xFF;
            }
            pos += 27 + nseg + payload;
            n++;
        }
        return n;
    }

    /**
     * Concatenates byte arrays.
     * <p>
     * 바이트 배열을 이어 붙인다.
     *
     * @param parts the arrays / 배열들
     * @return the concatenation / 이어 붙인 결과
     */
    public static byte[] concat(byte[]... parts) {
        Out o = new Out();
        // 배열을 차례로 쓴다.
        for (byte[] p : parts) {
            o.bytes(p);
        }
        return o.toByteArray();
    }
}
