package org.duckdns.hjow.swingwebpanel.media.audio.ogg;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.FileByteSource;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.OggMux;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the Ogg container reader (plan items P18-07; FEATURE_MATRIX AUD-06, AUD-07, AUD-08): pages, packets over several pages,
 * granule positions, multiplexed and chained streams, garbage between pages, damaged pages and starting in the middle.
 * <p>
 * Ogg 컨테이너 읽기 시험(계획 P18-07, FEATURE_MATRIX AUD-06, AUD-07, AUD-08): 페이지, 여러 페이지에 걸친 패킷, 그래뉼 위치, 다중화/체인 스트림,
 * 페이지 사이의 쓰레기, 손상된 페이지, 중간에서 시작하기.
 */
public class OggReaderTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public OggReaderTest() {
    }

    /**
     * Makes packets of the given sizes with deterministic content and granule positions 100, 200, ...
     * <p>
     * 주어진 크기의 패킷을 결정적인 내용으로 만들고 그래뉼 위치는 100, 200, ... 으로 한다.
     *
     * @param sizes packet sizes / 패킷 크기
     * @param seed  seed / 시드
     * @return the packets / 패킷
     */
    private static byte[][] packets(int[] sizes, long seed) {
        Random r = new Random(seed);
        byte[][] p = new byte[sizes.length][];
        // 크기마다 난수 내용의 패킷을 만든다.
        for (int i = 0; i < sizes.length; i++) {
            p[i] = new byte[sizes[i]];
            r.nextBytes(p[i]);
        }
        return p;
    }

    /**
     * Returns the granule array 100, 200, ... for n packets.
     * <p>
     * n 개 패킷에 대한 그래뉼 배열 100, 200, ... 을 반환한다.
     *
     * @param n count / 개수
     * @return granules / 그래뉼 위치
     */
    private static long[] granules(int n) {
        long[] g = new long[n];
        // 패킷마다 100 씩 늘어난다.
        for (int i = 0; i < n; i++) {
            g[i] = (i + 1) * 100L;
        }
        return g;
    }

    /**
     * Reads all packets of a stream.
     * <p>
     * 스트림의 모든 패킷을 읽는다.
     *
     * @param s      stream / 스트림
     * @param start  start page / 시작 페이지
     * @param packet receives the packets / 패킷을 받는다
     * @throws MediaException on failure / 실패 시
     */
    private static void readAll(OggStream s, int start, List<OggPacket> packet) throws MediaException {
        OggPacketStream in = s.openPackets(start);
        // 끝(null)까지 패킷을 읽는다.
        for (OggPacket p = in.next(); p != null; p = in.next()) {
            packet.add(p);
        }
    }

    /**
     * The bitwise CRC of the test muxer equals the table-driven CRC, and a known all-zero page header has the expected checksum.
     * <p>
     * 시험 멀티플렉서의 비트 단위 CRC 가 표 기반 CRC 와 같다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testChecksum() throws Exception {
        byte[] file = OggMux.mux(1, 0, packets(new int[] { 10, 300 }, 1), granules(2), 255, true, true);
        OggPage p = OggPage.parse(file, 0);
        Check.notNull(p, "the page parses");
        Check.isTrue(p.isChecksumValid(file), "the checksum written by the bitwise implementation is accepted");
        file[40] ^= 1;
        Check.isFalse(p.isChecksumValid(file), "a changed payload byte is detected");
        Check.equal(0L, OggMux.crc(new byte[0]), "empty data");
        Check.equal(0x04C11DB7L, OggMux.crc(new byte[] { 0, 0, 0, 1 }) , "the polynomial itself comes out of one bit shifted through");
    }

    /**
     * Packets of many sizes (empty, 255 multiples, huge) are read back exactly, with continuation over pages and granule positions.
     * <p>
     * 여러 크기(빈 패킷, 255 의 배수, 큰 패킷)의 패킷이 페이지에 걸쳐도 정확히 읽히고 그래뉼 위치가 맞는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPacketsAcrossPages() throws Exception {
        int[] sizes = { 30, 0, 1, 254, 255, 256, 510, 511, 1000, 5, 70000, 12, 255, 0 };
        byte[][] pk = packets(sizes, 2);
        long[] gr = granules(sizes.length);
        int[] perPage = { 255, 7, 3, 1 };
        // 페이지당 세그먼트 수를 바꿔 가며 같은 패킷이 다시 나오는지 확인한다.
        for (int max : perPage) {
            byte[] file = OggMux.mux(0x1234, 5, pk, gr, max, true, true);
            OggReader r = OggReader.scan(file);
            Check.equal(1, r.getStreams().size(), "one stream, " + max + " segments per page");
            OggStream s = r.getStreams().get(0);
            Check.equal(0x1234, s.getSerial(), "serial");
            java.util.ArrayList<OggPacket> got = new java.util.ArrayList<OggPacket>();
            readAll(s, 0, got);
            Check.equal(sizes.length, got.size(), "packet count with " + max + " segments per page");
            // 패킷 내용이 같고 마지막 패킷만 끝으로 표시되는지 확인한다.
            for (int i = 0; i < sizes.length; i++) {
                Check.equal(true, Arrays.equals(pk[i], got.get(i).getData()), "packet " + i + " with " + max + " segments per page");
                Check.equal(i == sizes.length - 1, got.get(i).isLast(), "last flag of packet " + i);
                long g = got.get(i).getGranule();
                Check.isTrue(g == -1 || g == gr[i], "granule of packet " + i + " is its own or unknown: " + g);
            }
            Check.equal(gr[sizes.length - 1], got.get(sizes.length - 1).getGranule(), "the last packet always ends a page");
            Check.equal(gr[sizes.length - 1], s.getLastGranule(), "last granule of the stream");
            Check.isTrue(s.isEnded(), "end of stream flag");
        }
    }

    /**
     * With one packet per page every packet carries its granule position, and pages can be found by granule position.
     * <p>
     * 페이지마다 패킷이 하나면 모든 패킷이 그래뉼 위치를 가지며 그래뉼 위치로 페이지를 찾을 수 있다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testGranulesAndPageSearch() throws Exception {
        byte[][] pk = packets(new int[] { 10, 20, 30, 40, 50 }, 3);
        OggReader r = OggReader.scan(OggMux.mux(7, 0, pk, granules(5), 1, true, true));
        OggStream s = r.getStreams().get(0);
        java.util.ArrayList<OggPacket> got = new java.util.ArrayList<OggPacket>();
        readAll(s, 0, got);
        // 패킷마다 자기 그래뉼 위치와 페이지 번호를 갖는다.
        for (int i = 0; i < 5; i++) {
            Check.equal((i + 1) * 100L, got.get(i).getGranule(), "granule " + i);
            Check.equal(i, got.get(i).getPageIndex(), "page index " + i);
        }
        Check.equal(0, s.findPageAfterGranule(-1), "before everything");
        Check.equal(0, s.findPageAfterGranule(99), "just before the first granule");
        Check.equal(1, s.findPageAfterGranule(100), "the first page ends exactly at 100");
        Check.equal(3, s.findPageAfterGranule(350), "inside the fourth page");
        Check.equal(5, s.findPageAfterGranule(500), "at the end");
        Check.equal(5, s.findPageAfterGranule(10000), "beyond the end");
    }

    /**
     * Reading may start at any page; a packet that continues from before the start page is skipped.
     * <p>
     * 읽기는 어느 페이지에서든 시작할 수 있으며 시작 페이지 앞에서 이어 오는 패킷은 건너뛴다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testStartInTheMiddle() throws Exception {
        int[] sizes = { 600, 600, 600, 600 };
        byte[][] pk = packets(sizes, 4);
        OggStream s = OggReader.scan(OggMux.mux(9, 0, pk, granules(4), 2, true, true)).getStreams().get(0);
        Check.isTrue(s.getPageCount() >= 6, "several pages");
        // 모든 시작 페이지에서 읽은 패킷이 원본 패킷의 뒤쪽 부분과 일치하는지 확인한다.
        for (int start = 0; start < s.getPageCount(); start++) {
            java.util.ArrayList<OggPacket> got = new java.util.ArrayList<OggPacket>();
            readAll(s, start, got);
            // 패킷이 원본과 같은지 하나씩 확인한다.
            for (int k = 0; k < got.size(); k++) {
                int idx = sizes.length - got.size() + k;
                Check.equal(true, Arrays.equals(pk[idx], got.get(k).getData()), "start page " + start + " packet " + k);
            }
            Check.isTrue(got.size() <= sizes.length, "never more packets than exist");
        }
        java.util.ArrayList<OggPacket> none = new java.util.ArrayList<OggPacket>();
        readAll(s, 1000, none);
        Check.equal(0, none.size(), "a start page beyond the end gives no packets");
    }

    /**
     * Multiplexed and chained files: the stream list, the codec recognition and findFirst.
     * <p>
     * 다중화된 파일과 체인 파일: 스트림 목록, 코덱 인식, findFirst.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMultiplexedAndChained() throws Exception {
        byte[] vorbisId = new byte[30];
        vorbisId[0] = 1;
        System.arraycopy("vorbis".getBytes("US-ASCII"), 0, vorbisId, 1, 6);
        byte[] opusId = new byte[19];
        System.arraycopy("OpusHead".getBytes("US-ASCII"), 0, opusId, 0, 8);
        byte[] flacId = new byte[10];
        flacId[0] = 0x7F;
        System.arraycopy("FLAC".getBytes("US-ASCII"), 0, flacId, 1, 4);
        byte[] other = new byte[12];
        byte[] vBos = OggMux.mux(11, 0, new byte[][] { vorbisId }, new long[] { 0 }, 255, true, false);
        byte[] vData = OggMux.mux(11, 1, new byte[][] { new byte[50] }, new long[] { 500 }, 255, false, true);
        byte[] oBos = OggMux.mux(22, 0, new byte[][] { opusId }, new long[] { 0 }, 255, true, false);
        byte[] oData = OggMux.mux(22, 1, new byte[][] { new byte[50] }, new long[] { 900 }, 255, false, true);
        byte[] fFull = OggMux.mux(33, 0, new byte[][] { flacId }, new long[] { 0 }, 255, true, true);
        byte[] uBos = OggMux.mux(44, 0, new byte[][] { other }, new long[] { 0 }, 255, true, false);
        byte[] uData = OggMux.mux(44, 1, new byte[][] { new byte[3] }, new long[] { 5 }, 255, false, true);
        // 처음에 BOS 페이지 네 개가 이어진 다중화 파일.
        byte[] mux = OggMux.concat(vBos, oBos, OggMux.mux(33, 0, new byte[][] { flacId }, new long[] { 0 }, 255, true, false), uBos, vData, oData,
                OggMux.mux(33, 1, new byte[][] { new byte[4] }, new long[] { 9 }, 255, false, true), uData);
        List<String> sniffed = OggReader.sniffCodecs(mux);
        Check.equal(Arrays.asList("vorbis", "opus", "flac", "unknown"), sniffed, "codecs of the start pages");
        OggReader r = OggReader.scan(mux);
        Check.equal(4, r.getStreams().size(), "four streams");
        Check.equal("vorbis", r.findFirst("vorbis").getCodec(), "vorbis stream");
        Check.equal(22, r.findFirst("opus").getSerial(), "opus serial");
        Check.isNull(r.findFirst("speex"), "no speex stream");
        Check.equal(true, Arrays.equals(vorbisId, r.findFirst("vorbis").getFirstPacket()), "first packet");
        Check.equal(500L, r.findFirst("vorbis").getLastGranule(), "granule of the vorbis stream");
        Check.equal(900L, r.findFirst("opus").getLastGranule(), "granule of the opus stream");
        // 체인 파일: 끝난 스트림 뒤에 같은 코덱의 새 스트림이 이어진다.
        byte[] vFull = OggMux.concat(vBos, vData);
        byte[] chained = OggMux.concat(vFull, OggMux.mux(55, 0, new byte[][] { vorbisId, new byte[8] }, new long[] { 0, 77 }, 255, true, true), fFull,
                OggMux.concat(uBos, uData));
        OggReader c = OggReader.scan(chained);
        Check.equal(4, c.getStreams().size(), "a chain of four links");
        Check.equal(11, c.findFirst("vorbis").getSerial(), "findFirst returns the first link of the chain");
        // 같은 시리얼 번호가 끝난 뒤에 다시 시작해도 새 스트림이다.
        OggReader same = OggReader.scan(OggMux.concat(vFull, vFull));
        Check.equal(2, same.getStreams().size(), "a serial number reused after its end starts a new stream");
        Check.isTrue(OggReader.startsWithOgg(mux), "capture pattern");
        Check.equal(0, OggReader.sniffCodecs(new byte[] { 1, 2, 3 }).size(), "no codecs in non-Ogg data");
    }

    /**
     * Garbage between pages, fake capture patterns and a truncated last page do not break the index.
     * <p>
     * 페이지 사이의 쓰레기, 가짜 캡처 패턴, 잘린 마지막 페이지가 색인을 깨지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testGarbageAndTruncation() throws Exception {
        byte[][] pk = packets(new int[] { 100, 200, 300 }, 5);
        byte[] p1 = OggMux.mux(3, 0, new byte[][] { pk[0] }, new long[] { 10 }, 255, true, false);
        byte[] p2 = OggMux.mux(3, 1, new byte[][] { pk[1] }, new long[] { 20 }, 255, false, false);
        byte[] p3 = OggMux.mux(3, 2, new byte[][] { pk[2] }, new long[] { 30 }, 255, false, true);
        byte[] junk = new byte[137];
        new Random(6).nextBytes(junk);
        byte[] fake = new byte[] { 'O', 'g', 'g', 'S', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 200, (byte) 255, (byte) 255, (byte) 255 };
        byte[] file = OggMux.concat(junk, p1, fake, junk, p2, new byte[] { 'O', 'g', 'g', 'S' }, p3);
        OggStream s = OggReader.scan(file).getStreams().get(0);
        java.util.ArrayList<OggPacket> got = new java.util.ArrayList<OggPacket>();
        readAll(s, 0, got);
        Check.equal(3, got.size(), "all packets survive the garbage");
        // 세 패킷이 원본과 같은지 확인한다.
        for (int i = 0; i < 3; i++) {
            Check.equal(true, Arrays.equals(pk[i], got.get(i).getData()), "packet " + i);
        }
        byte[] cut = new byte[p1.length + p2.length + p3.length - 5];
        System.arraycopy(OggMux.concat(p1, p2, p3), 0, cut, 0, cut.length);
        OggStream t = OggReader.scan(cut).getStreams().get(0);
        Check.equal(2, t.getPageCount(), "a truncated last page is ignored");
        try {
            OggReader.scan(new byte[100]);
            Check.fail("data without pages must fail");
        } catch (MediaException e) {
            Check.equal(MediaException.Kind.CORRUPT, e.getKind(), "no pages");
        }
    }

    /**
     * A page with a wrong checksum is skipped: the packets that end on it are lost, the stream goes on at the next packet boundary and
     * the damage is counted.
     * <p>
     * 체크섬이 틀린 페이지는 건너뛴다: 그 페이지에서 끝나는 패킷은 잃고 스트림은 다음 패킷 경계에서 계속되며 손상 수가 센다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDamagedPage() throws Exception {
        byte[][] pk = packets(new int[] { 100, 100, 100, 100 }, 7);
        byte[] file = OggMux.mux(5, 0, pk, granules(4), 1, true, true);
        OggStream s = OggReader.scan(file).getStreams().get(0);
        int off = s.getPageOffset(1) + 27 + 1 + 5;
        file[off] ^= 0x40;
        OggPacketStream in = s.openPackets(0);
        java.util.ArrayList<OggPacket> got = new java.util.ArrayList<OggPacket>();
        // 끝까지 읽으면서 패킷을 모은다.
        for (OggPacket p = in.next(); p != null; p = in.next()) {
            got.add(p);
        }
        Check.equal(3, got.size(), "the packet of the damaged page is lost");
        Check.equal(1, in.getDamagedPages(), "one damaged page");
        Check.equal(true, Arrays.equals(pk[0], got.get(0).getData()), "before the damage");
        Check.equal(true, Arrays.equals(pk[2], got.get(1).getData()), "after the damage");
        Check.equal(true, Arrays.equals(pk[3], got.get(2).getData()), "the last packet");
    }

    /**
     * Reads a split packet, resynchronizes after garbage, and skips a CRC failure using small file windows.
     * <p>
     * 작은 파일 창에서 분할 패킷을 읽고 잡음 뒤에 다시 동기화하여 CRC가 틀린 페이지를 건너뛴다.
     * @throws Exception on I/O or parser failure / 입출력이나 해석에 실패하면
     */
    public void testSeekableFilePageAndPacketReads() throws Exception {
        byte[][] packets = packets(new int[] { 8, 70000, 12, 300 }, 31);
        System.arraycopy("OpusHead".getBytes("US-ASCII"), 0, packets[0], 0, 8);
        byte[] first = OggMux.mux(7, 0, new byte[][] { packets[0], packets[1] }, new long[] { 100, 200 }, 255, true, false);
        OggStream start = OggReader.scan(first).getStreams().get(0);
        int count = start.getPageCount();
        byte[] bad = OggMux.mux(7, count, new byte[][] { packets[2] }, new long[] { 300 }, 255, false, false);
        bad[bad.length - 1] ^= 1;
        byte[] last = OggMux.mux(7, count + 1, new byte[][] { packets[3] }, new long[] { 400 }, 255, false, true);
        byte[] encoded = OggMux.concat(first, new byte[] { 9, 8, 7, 6 }, bad, last);
        File file = File.createTempFile("swp-ogg-source-", ".ogg");
        try {
            FileOutputStream out = new FileOutputStream(file);
            try {
                out.write(encoded);
            } finally {
                out.close();
            }
            FileByteSource input = new FileByteSource(file, 17);
            try {
                Check.equal(Arrays.asList("opus"), OggReader.sniffSourceCodecs(input), "codec from the first file page");
                OggStream disk = OggReader.scanSource(input).findFirst("opus");
                OggStream mem = OggReader.scan(encoded).findFirst("opus");
                Check.equal(count + 2, disk.getPageCount(), "both indexed pages after the garbage");
                // 캡처 위치와 그래뉼 및 분할 상태가 파일과 배열에서 같은지 확인한다.
                for (int i = 0; i < disk.getPageCount(); i++) {
                    Check.equal(mem.getPageOffset(i), disk.getPageOffset(i), "page offset " + i);
                    Check.equal(mem.getPageGranule(i), disk.getPageGranule(i), "page granule " + i);
                    Check.equal(mem.isPageContinued(i), disk.isPageContinued(i), "continuation flag " + i);
                }
                OggPacketStream reader = disk.openPackets(0);
                Check.isTrue(Arrays.equals(packets[0], reader.next().getData()), "first packet");
                Check.isTrue(Arrays.equals(packets[1], reader.next().getData()), "packet spanning multiple pages");
                OggPacket tail = reader.next();
                Check.isTrue(Arrays.equals(packets[3], tail.getData()) && tail.isLast(), "the final packet survives the damage");
                Check.equal(400L, tail.getGranule(), "final granule");
                Check.isNull(reader.next(), "end of stream");
                Check.equal(1, reader.getDamagedPages(), "one wrong CRC");

                OggPacketStream delayedRead = disk.openPackets(count + 1);
                input.close();
                try {
                    delayedRead.next();
                    Check.fail("reading a page of a closed file must fail");
                } catch (MediaException expected) {
                    Check.equal(MediaException.Kind.ACCESS_FAILED, expected.getKind(), "closed file is a storage error");
                }
            } finally {
                input.close();
            }
        } finally {
            Check.isTrue(file.delete(), "the caller can delete the fixture");
        }
    }

    /**
     * A page sequence gap (lost pages) drops a packet that was split over the gap.
     * <p>
     * 페이지 순서의 빈틈(잃은 페이지)이 있으면 그 틈에 걸친 패킷을 버린다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSequenceGap() throws Exception {
        byte[][] pk = packets(new int[] { 700, 50, 50 }, 8);
        byte[] a = OggMux.mux(5, 0, pk, granules(3), 2, true, true);
        OggStream whole = OggReader.scan(a).getStreams().get(0);
        Check.isTrue(whole.getPageCount() >= 3, "the first packet spans pages");
        // 중간 페이지를 통째로 잘라 낸 파일을 만든다.
        int cutFrom = whole.getPageOffset(1);
        int cutTo = whole.getPageOffset(2);
        byte[] holed = new byte[a.length - (cutTo - cutFrom)];
        System.arraycopy(a, 0, holed, 0, cutFrom);
        System.arraycopy(a, cutTo, holed, cutFrom, a.length - cutTo);
        OggStream s = OggReader.scan(holed).getStreams().get(0);
        java.util.ArrayList<OggPacket> got = new java.util.ArrayList<OggPacket>();
        readAll(s, 0, got);
        Check.isTrue(got.size() < 3, "the split packet is lost");
        Check.isTrue(got.size() >= 1, "later packets survive");
        Check.equal(true, Arrays.equals(pk[2], got.get(got.size() - 1).getData()), "the last packet is intact");
    }
}
