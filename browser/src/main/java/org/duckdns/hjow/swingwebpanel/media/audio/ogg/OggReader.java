package org.duckdns.hjow.swingwebpanel.media.audio.ogg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.Bytes;

/**
 * Index of an Ogg file (RFC 3533): every page is located (garbage between pages is skipped by searching for the next capture pattern)
 * and assigned to its logical bitstream by serial number. The result lists the logical streams in the order in which they begin:
 * several streams at the start are a multiplexed ("grouped") file, streams that follow an ended one are "chained" files. Codec decoders
 * choose the stream they can play with {@link #findFirst(String)} and read its packets with {@link OggStream#openPackets(int)}. Only the
 * page headers are read while indexing; checksums are verified when pages are read.
 * <p>
 * Ogg 파일(RFC 3533)의 색인: 모든 페이지의 위치를 찾고(페이지 사이의 쓰레기는 다음 캡처 패턴을 찾아 건너뛴다) 시리얼 번호로 논리 비트스트림에
 * 배정한다. 결과는 논리 스트림을 시작한 순서대로 나열한다. 처음에 스트림이 여럿이면 다중화된("grouped") 파일이고, 끝난 스트림 뒤에 이어지는 스트림은
 * "chained" 파일이다. 코덱 디코더는 {@link #findFirst(String)} 으로 재생할 수 있는 스트림을 고르고 {@link OggStream#openPackets(int)} 로 패킷을
 * 읽는다. 색인하는 동안은 페이지 헤더만 읽으며 체크섬은 페이지를 읽을 때 확인한다.
 */
public final class OggReader {

    /**
     * The logical streams in order of appearance.
     * <p>
     * 나타난 순서대로의 논리 스트림.
     */
    private final List<OggStream> streams;

    /**
     * Creates the result.
     * <p>
     * 결과를 만든다.
     *
     * @param streams logical streams / 논리 스트림
     */
    private OggReader(List<OggStream> streams) {
        this.streams = Collections.unmodifiableList(streams);
    }

    /**
     * Indexes an Ogg file.
     * <p>
     * Ogg 파일을 색인한다.
     *
     * @param data the whole file / 파일 전체
     * @return the index / 색인
     * @throws MediaException CORRUPT if the data contains no Ogg page; CANCELLED if the thread is interrupted / 데이터에 Ogg 페이지가 없으면 CORRUPT, 스레드가 인터럽트되면 CANCELLED
     */
    public static OggReader scan(byte[] data) throws MediaException {
        List<OggStream> all = new ArrayList<OggStream>();
        Map<Integer, OggStream> current = new HashMap<Integer, OggStream>();
        int pos = 0;
        int pages = 0;
        // 페이지를 차례로 찾아 해당 논리 스트림의 색인에 넣는다.
        while (pos < data.length) {
            if ((pages & 0xFFF) == 0 && Thread.currentThread().isInterrupted()) {
                throw new MediaException(MediaException.Kind.CANCELLED, "Ogg scanning was interrupted");
            }
            OggPage p = OggPage.parse(data, pos);
            // 페이지 바로 뒤에 다음 캡처 패턴이나 파일 끝이 이어지지 않으면 체크섬으로 진짜 페이지인지 확인한다(가짜 헤더가 진짜 페이지를 삼키지 않도록).
            if (p != null && p.getEnd() < data.length && !Bytes.matches(data, p.getEnd(), "OggS") && !p.isChecksumValid(data)) {
                p = null;
            }
            if (p == null) {
                pos = findCapture(data, pos + 1);
                if (pos < 0) {
                    break;
                }
                continue;
            }
            Integer key = Integer.valueOf(p.getSerial());
            OggStream s = current.get(key);
            if (s == null || (p.isFirst() && s.isEnded())) {
                s = new OggStream(data, p.getSerial());
                current.put(key, s);
                all.add(s);
            }
            s.addPage(p);
            pages++;
            pos = p.getEnd();
        }
        if (all.isEmpty()) {
            throw new MediaException(MediaException.Kind.CORRUPT, "no Ogg page was found");
        }
        return new OggReader(all);
    }

    /**
     * Finds the next capture pattern <code>OggS</code>.
     * <p>
     * 다음 캡처 패턴 <code>OggS</code> 를 찾는다.
     *
     * @param d    data / 데이터
     * @param from first offset to examine / 처음 검사할 위치
     * @return offset, or -1 / 위치. 없으면 -1
     */
    private static int findCapture(byte[] d, int from) {
        // 바이트를 하나씩 보며 캡처 패턴을 찾는다.
        for (int i = from; i + 4 <= d.length; i++) {
            if (d[i] == 'O' && d[i + 1] == 'g' && d[i + 2] == 'g' && d[i + 3] == 'S') {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the logical streams in order of appearance.
     * <p>
     * 나타난 순서대로 논리 스트림을 반환한다.
     *
     * @return streams (unmodifiable) / 스트림(수정 불가)
     */
    public List<OggStream> getStreams() {
        return streams;
    }

    /**
     * Returns the first logical stream of a codec, or null. A chained file continues after the first stream: only that first stream is
     * returned, so a player plays the first link of a chain.
     * <p>
     * 코덱의 첫 논리 스트림을 반환한다. 없으면 null. chained 파일은 첫 스트림 뒤에 이어지는데 그 첫 스트림만 반환하므로 재생기는 체인의 첫 고리를
     * 재생한다.
     *
     * @param codec codec name as returned by {@link OggStream#getCodec()} / {@link OggStream#getCodec()} 가 반환하는 코덱 이름
     * @return the stream or null / 스트림 또는 null
     */
    public OggStream findFirst(String codec) {
        // 스트림을 처음부터 보며 코덱이 같은 첫 스트림을 찾는다.
        for (OggStream s : streams) {
            if (codec.equals(s.getCodec())) {
                return s;
            }
        }
        return null;
    }

    /**
     * Lists the codecs of the logical streams that begin at the start of the file (the beginning-of-stream pages), without indexing the
     * whole file. Used by decoders to recognize their data cheaply.
     * <p>
     * 파일 첫머리에서 시작하는 논리 스트림(스트림 시작 페이지)들의 코덱을 파일 전체를 색인하지 않고 나열한다. 디코더가 자기 데이터를 싸게 알아보는 데
     * 쓴다.
     *
     * @param data the file data / 파일 데이터
     * @return codec names in order, empty if the data does not start with an Ogg page / 순서대로의 코덱 이름. 데이터가 Ogg 페이지로 시작하지 않으면 빈 목록
     */
    public static List<String> sniffCodecs(byte[] data) {
        List<String> codecs = new ArrayList<String>();
        int pos = 0;
        // 스트림 시작 페이지가 이어지는 동안 첫 패킷으로 코덱을 알아본다.
        for (;;) {
            OggPage p = OggPage.parse(data, pos);
            if (p == null || !p.isFirst()) {
                break;
            }
            int len = 0;
            // 첫 패킷의 길이를 세그먼트에서 구한다.
            for (int i = 0; i < p.getSegmentCount(); i++) {
                int s = data[p.getSegmentTableOffset() + i] & 0xFF;
                len += s;
                if (s < 255) {
                    break;
                }
            }
            codecs.add(OggStream.detectCodec(data, p.getPayloadOffset(), Math.min(len, p.getPayloadLength())));
            pos = p.getEnd();
        }
        return codecs;
    }

    /**
     * Tells whether the data starts with the Ogg capture pattern.
     * <p>
     * 데이터가 Ogg 캡처 패턴으로 시작하는지 알려 준다.
     *
     * @param data the data / 데이터
     * @return true if it starts with <code>OggS</code> / <code>OggS</code> 로 시작하면 true
     */
    public static boolean startsWithOgg(byte[] data) {
        return Bytes.matches(data, 0, "OggS");
    }
}
