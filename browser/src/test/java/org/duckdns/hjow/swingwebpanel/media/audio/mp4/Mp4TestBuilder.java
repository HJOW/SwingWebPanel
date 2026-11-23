package org.duckdns.hjow.swingwebpanel.media.audio.mp4;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * A test-only builder of ISO base media files (MP4 / M4A): ftyp, moov with one audio track (sample table, optional edit list) and mdat, or a
 * fragmented file (moov with mvex, then moof/mdat pairs). It writes the boxes straight from ISO/IEC 14496-12 and does not use the parser.
 * <p>
 * ISO 기본 미디어 파일(MP4 / M4A)을 만드는 시험 전용 생성기: ftyp, 오디오 트랙 하나(샘플 표, 선택적 편집 목록)가 있는 moov, mdat. 또는 조각화된 파일(mvex 가
 * 있는 moov 다음에 moof/mdat 쌍). 박스를 ISO/IEC 14496-12 에서 직접 쓰며 해석기를 쓰지 않는다.
 */
public final class Mp4TestBuilder {

    /** Sample entry format, for example mp4a. / 샘플 엔트리 형식. 예: mp4a. */
    public String format = "mp4a";
    /** Channel count of the sample entry. / 샘플 엔트리의 채널 수. */
    public int channels = 2;
    /** Sampling frequency of the sample entry. / 샘플 엔트리의 샘플링 주파수. */
    public int sampleRate = 44100;
    /** Media time scale. / 미디어 시간 단위. */
    public int timescale = 44100;
    /** Duration of every sample in time scale units. / 샘플마다 길이(시간 단위). */
    public int sampleDuration = 1024;
    /** AudioSpecificConfig bytes of the elementary stream descriptor (or dOps body for Opus). / 기본 스트림 서술자의 AudioSpecificConfig(Opus 는 dOps 본문). */
    public byte[] config = new byte[0];
    /** Object type indication of the descriptor. / 서술자의 객체 종류 표시. */
    public int objectType = 0x40;
    /** Media time of the edit list (-1 for none). / 편집 목록의 미디어 시간(-1 이면 없음). */
    public long editStart = -1;
    /** Duration of the edit in movie time scale units. / 편집의 길이(영상 시간 단위). */
    public long editDuration;
    /** Write a fragmented file. / 조각화된 파일을 쓴다. */
    public boolean fragmented;
    /** Samples per fragment. / 조각당 샘플 수. */
    public int perFragment = 10;
    /** Samples per chunk in the sample table. / 샘플 표의 청크당 샘플 수. */
    public int perChunk = 4;

    /**
     * Writes a 32-bit big-endian value. / 32비트 빅 엔디언 값을 쓴다.
     *
     * @param o output / 출력
     * @param v value / 값
     */
    private static void u32(ByteArrayOutputStream o, long v) {
        o.write((int) (v >> 24));
        o.write((int) (v >> 16));
        o.write((int) (v >> 8));
        o.write((int) v);
    }

    /**
     * Writes a 16-bit big-endian value. / 16비트 빅 엔디언 값을 쓴다.
     *
     * @param o output / 출력
     * @param v value / 값
     */
    private static void u16(ByteArrayOutputStream o, int v) {
        o.write(v >> 8);
        o.write(v);
    }

    /**
     * Makes a box. / 박스를 만든다.
     *
     * @param type four character code / 4글자 코드
     * @param body body bytes / 본문
     * @return the box / 박스
     */
    static byte[] box(String type, byte[]... body) {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        for (byte[] x : body) { // 본문 조각을 이어 붙인다
            b.write(x, 0, x.length);
        }
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        u32(o, 8 + b.size());
        for (int i = 0; i < 4; i++) { // 형식 코드를 쓴다
            o.write(type.charAt(i));
        }
        byte[] bb = b.toByteArray();
        o.write(bb, 0, bb.length);
        return o.toByteArray();
    }

    /**
     * Makes a full box body prefix (version and flags). / 전체 박스 접두(버전과 플래그)를 만든다.
     *
     * @param version version / 버전
     * @param flags   flags / 플래그
     * @return four bytes / 4바이트
     */
    private static byte[] vf(int version, int flags) {
        return new byte[] { (byte) version, (byte) (flags >> 16), (byte) (flags >> 8), (byte) flags };
    }

    /**
     * Concatenates arrays. / 배열을 이어 붙인다.
     *
     * @param parts arrays / 배열
     * @return the concatenation / 이어 붙인 결과
     */
    static byte[] cat(byte[]... parts) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        for (byte[] p : parts) { // 배열을 차례로 쓴다
            o.write(p, 0, p.length);
        }
        return o.toByteArray();
    }

    /**
     * Builds the sample entry with its configuration box.
     * <p>
     * 구성 박스가 있는 샘플 엔트리를 만든다.
     *
     * @return the stsd box / stsd 박스
     */
    private byte[] stsd() {
        ByteArrayOutputStream e = new ByteArrayOutputStream();
        for (int i = 0; i < 6; i++) { // 예약 6바이트
            e.write(0);
        }
        u16(e, 1);
        u32(e, 0);
        u32(e, 0);
        u16(e, channels);
        u16(e, 16);
        u32(e, 0);
        u32(e, (long) sampleRate << 16);
        byte[] cfg;
        if ("mp4a".equals(format)) {
            byte[] dsi = cat(new byte[] { 5, (byte) config.length }, config);
            byte[] dcd = cat(new byte[] { 4, (byte) (13 + dsi.length), (byte) objectType, 0x15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, dsi);
            byte[] sl = new byte[] { 6, 1, 2 };
            byte[] es = cat(new byte[] { 3, (byte) (3 + dcd.length + sl.length), 0, 0, 0 }, dcd, sl);
            cfg = box("esds", vf(0, 0), es);
        } else {
            cfg = box("dOps", config);
        }
        byte[] eb = e.toByteArray();
        byte[] entry = box(format, eb, cfg);
        ByteArrayOutputStream s = new ByteArrayOutputStream();
        s.write(vf(0, 0), 0, 4);
        u32(s, 1);
        s.write(entry, 0, entry.length);
        return box("stsd", s.toByteArray());
    }

    /**
     * Builds the file.
     * <p>
     * 파일을 만든다.
     *
     * @param samples the samples / 샘플
     * @return the file bytes / 파일 바이트
     */
    public byte[] build(List<byte[]> samples) {
        byte[] ftyp = box("ftyp", new byte[] { 'M', '4', 'A', ' ', 0, 0, 0, 0, 'M', '4', 'A', ' ', 'i', 's', 'o', 'm' });
        ByteArrayOutputStream mvhd = new ByteArrayOutputStream();
        mvhd.write(vf(0, 0), 0, 4);
        u32(mvhd, 0);
        u32(mvhd, 0);
        u32(mvhd, 1000);
        u32(mvhd, 0);
        for (int i = 0; i < 80; i++) { // 나머지 필드는 0
            mvhd.write(0);
        }
        ByteArrayOutputStream tkhd = new ByteArrayOutputStream();
        tkhd.write(vf(0, 7), 0, 4);
        u32(tkhd, 0);
        u32(tkhd, 0);
        u32(tkhd, 1);
        for (int i = 0; i < 72; i++) { // 나머지 필드는 0
            tkhd.write(0);
        }
        ByteArrayOutputStream mdhd = new ByteArrayOutputStream();
        mdhd.write(vf(0, 0), 0, 4);
        u32(mdhd, 0);
        u32(mdhd, 0);
        u32(mdhd, timescale);
        u32(mdhd, fragmented ? 0 : (long) samples.size() * sampleDuration);
        u32(mdhd, 0);
        ByteArrayOutputStream hdlr = new ByteArrayOutputStream();
        hdlr.write(vf(0, 0), 0, 4);
        u32(hdlr, 0);
        hdlr.write('s');
        hdlr.write('o');
        hdlr.write('u');
        hdlr.write('n');
        for (int i = 0; i < 13; i++) { // 예약과 이름
            hdlr.write(0);
        }
        byte[] stbl;
        byte[] mdat;
        ByteArrayOutputStream mdatBody = new ByteArrayOutputStream();
        if (fragmented) {
            stbl = box("stbl", stsd(), box("stts", vf(0, 0), new byte[4]), box("stsc", vf(0, 0), new byte[4]), box("stsz", vf(0, 0), new byte[8]),
                    box("stco", vf(0, 0), new byte[4]));
            mdat = new byte[0];
        } else {
            for (byte[] s : samples) { // 모든 샘플을 mdat 에 이어 쓴다
                mdatBody.write(s, 0, s.length);
            }
            mdat = box("mdat", mdatBody.toByteArray());
            ByteArrayOutputStream stts = new ByteArrayOutputStream();
            stts.write(vf(0, 0), 0, 4);
            u32(stts, 1);
            u32(stts, samples.size());
            u32(stts, sampleDuration);
            ByteArrayOutputStream stsc = new ByteArrayOutputStream();
            stsc.write(vf(0, 0), 0, 4);
            u32(stsc, 1);
            u32(stsc, 1);
            u32(stsc, perChunk);
            u32(stsc, 1);
            ByteArrayOutputStream stsz = new ByteArrayOutputStream();
            stsz.write(vf(0, 0), 0, 4);
            u32(stsz, 0);
            u32(stsz, samples.size());
            for (byte[] s : samples) { // 샘플 크기
                u32(stsz, s.length);
            }
            int chunks = (samples.size() + perChunk - 1) / perChunk;
            // moov 의 크기를 알아야 청크 위치를 정할 수 있으므로 두 번 만든다.
            byte[] moovTry = moov(mvhd, tkhd, mdhd, hdlr, stts, stsc, stsz, chunks, 0, samples);
            long dataStart = ftyp.length + moovTry.length + 8;
            return cat(ftyp, moov(mvhd, tkhd, mdhd, hdlr, stts, stsc, stsz, chunks, dataStart, samples), mdat);
        }
        ByteArrayOutputStream trex = new ByteArrayOutputStream();
        trex.write(vf(0, 0), 0, 4);
        u32(trex, 1);
        u32(trex, 1);
        u32(trex, sampleDuration);
        u32(trex, 0);
        u32(trex, 0);
        byte[] moov = box("moov", box("mvhd", mvhd.toByteArray()), box("trak", box("tkhd", tkhd.toByteArray()),
                box("mdia", box("mdhd", mdhd.toByteArray()), box("hdlr", hdlr.toByteArray()), box("minf", stbl))), box("mvex", box("trex", trex.toByteArray())));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(ftyp, 0, ftyp.length);
        out.write(moov, 0, moov.length);
        for (int i = 0; i < samples.size(); i += perFragment) { // 조각마다 moof 와 mdat 를 쓴다
            int n = Math.min(perFragment, samples.size() - i);
            ByteArrayOutputStream trun = new ByteArrayOutputStream();
            trun.write(vf(0, 0x201), 0, 4);
            u32(trun, n);
            u32(trun, 0);
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            for (int j = 0; j < n; j++) { // 샘플 크기와 본문
                u32(trun, samples.get(i + j).length);
                body.write(samples.get(i + j), 0, samples.get(i + j).length);
            }
            byte[] tfhd = box("tfhd", vf(0, 0x20000), new byte[] { 0, 0, 0, 1 });
            byte[] mfhd = box("mfhd", vf(0, 0), new byte[] { 0, 0, 0, (byte) (i / perFragment + 1) });
            byte[] trunBox = box("trun", trun.toByteArray());
            byte[] moofNoOffset = box("moof", mfhd, box("traf", tfhd, trunBox));
            // trun 의 data_offset 은 moof 시작에서 mdat 본문까지의 거리이다.
            byte[] trunFixed = trun.toByteArray();
            long off = moofNoOffset.length + 8;
            trunFixed[8] = (byte) (off >> 24);
            trunFixed[9] = (byte) (off >> 16);
            trunFixed[10] = (byte) (off >> 8);
            trunFixed[11] = (byte) off;
            byte[] moof = box("moof", mfhd, box("traf", tfhd, box("trun", trunFixed)));
            byte[] md = box("mdat", body.toByteArray());
            out.write(moof, 0, moof.length);
            out.write(md, 0, md.length);
        }
        return out.toByteArray();
    }

    /**
     * Builds the moov box of a non-fragmented file.
     * <p>
     * 조각화되지 않은 파일의 moov 박스를 만든다.
     *
     * @param mvhd      movie header body / 영상 헤더 본문
     * @param tkhd      track header body / 트랙 헤더 본문
     * @param mdhd      media header body / 미디어 헤더 본문
     * @param hdlr      handler body / 핸들러 본문
     * @param stts      stts body / stts 본문
     * @param stsc      stsc body / stsc 본문
     * @param stsz      stsz body / stsz 본문
     * @param chunks    number of chunks / 청크 수
     * @param dataStart offset of the first sample / 첫 샘플의 위치
     * @param samples   samples / 샘플
     * @return the moov box / moov 박스
     */
    private byte[] moov(ByteArrayOutputStream mvhd, ByteArrayOutputStream tkhd, ByteArrayOutputStream mdhd, ByteArrayOutputStream hdlr,
            ByteArrayOutputStream stts, ByteArrayOutputStream stsc, ByteArrayOutputStream stsz, int chunks, long dataStart, List<byte[]> samples) {
        ByteArrayOutputStream stco = new ByteArrayOutputStream();
        stco.write(vf(0, 0), 0, 4);
        u32(stco, chunks);
        long pos = dataStart;
        int idx = 0;
        for (int c = 0; c < chunks; c++) { // 청크마다 시작 위치를 쓴다
            u32(stco, pos);
            for (int i = 0; i < perChunk && idx < samples.size(); i++) {
                pos += samples.get(idx++).length;
            }
        }
        byte[] stbl = box("stbl", stsd(), box("stts", stts.toByteArray()), box("stsc", stsc.toByteArray()), box("stsz", stsz.toByteArray()),
                box("stco", stco.toByteArray()));
        byte[] trak;
        byte[] mdia = box("mdia", box("mdhd", mdhd.toByteArray()), box("hdlr", hdlr.toByteArray()), box("minf", stbl));
        if (editStart >= 0) {
            ByteArrayOutputStream el = new ByteArrayOutputStream();
            el.write(vf(0, 0), 0, 4);
            u32(el, 1);
            u32(el, editDuration);
            u32(el, editStart);
            u32(el, 0x10000);
            trak = box("trak", box("tkhd", tkhd.toByteArray()), box("edts", box("elst", el.toByteArray())), mdia);
        } else {
            trak = box("trak", box("tkhd", tkhd.toByteArray()), mdia);
        }
        return box("moov", box("mvhd", mvhd.toByteArray()), trak);
    }
}
