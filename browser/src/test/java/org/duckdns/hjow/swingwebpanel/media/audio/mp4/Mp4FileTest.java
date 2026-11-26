package org.duckdns.hjow.swingwebpanel.media.audio.mp4;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.FileByteSource;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the MP4 demultiplexer {@link Mp4File} (plan item P18-07): sample tables with several samples per chunk, the esds and dOps
 * configuration, the edit list, fragmented files and damaged structures. The files are written by {@link Mp4TestBuilder} straight from the
 * format specification.
 * <p>
 * MP4 역다중화기 {@link Mp4File} 시험(계획 P18-07): 청크당 샘플이 여럿인 샘플 표, esds 와 dOps 구성, 편집 목록, 조각화된 파일, 손상된 구조.
 * 파일은 {@link Mp4TestBuilder} 가 형식 규격에서 직접 쓴다.
 */
public class Mp4FileTest {

    /**
     * Creates the suite (required by the runner). / 스위트를 만든다(실행기가 필요로 함).
     */
    public Mp4FileTest() {
    }

    /**
     * Makes samples of different sizes with recognizable content.
     * <p>
     * 알아볼 수 있는 내용의 서로 다른 크기의 샘플을 만든다.
     *
     * @param n number of samples / 샘플 수
     * @return samples / 샘플
     */
    private static List<byte[]> samples(int n) {
        List<byte[]> l = new ArrayList<byte[]>();
        for (int i = 0; i < n; i++) { // 샘플마다 크기가 다르고 내용이 번호로 채워진다
            byte[] s = new byte[5 + (i * 7) % 11];
            Arrays.fill(s, (byte) (i + 1));
            l.add(s);
        }
        return l;
    }

    /**
     * Sample offsets, sizes and durations of a plain file, the configuration box and the edit list.
     * <p>
     * 일반 파일의 샘플 위치, 크기, 길이, 구성 박스, 편집 목록.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSampleTable() throws Exception {
        List<byte[]> s = samples(23);
        Mp4TestBuilder b = new Mp4TestBuilder();
        b.config = new byte[] { 0x12, 0x10 };
        b.perChunk = 4;
        b.editStart = 2112;
        b.editDuration = 700;
        byte[] file = b.build(s);
        Check.isTrue(Mp4File.looksLikeMp4(file), "looks like MP4");
        Mp4Track t = Mp4File.parse(file).firstAudioTrack();
        Check.notNull(t, "audio track");
        Check.equal("mp4a", t.getFormat(), "format");
        Check.equal(2, t.getChannels(), "channels");
        Check.equal(44100, t.getSampleRate(), "sampling frequency");
        Check.equal(44100, t.getTimescale(), "time scale");
        Check.equal(0x40, t.getObjectType(), "object type");
        Check.isTrue(Arrays.equals(new byte[] { 0x12, 0x10 }, t.getConfig()), "AudioSpecificConfig");
        Check.equal(23, t.getSampleCount(), "sample count");
        Check.equal(2112L, t.getEditStart(), "edit start");
        Check.equal(700L * 44100 / 1000, t.getEditDurationInMediaUnits(), "edit duration in media units");
        for (int i = 0; i < 23; i++) { // 샘플마다 위치에서 읽은 바이트가 만든 내용과 같아야 한다
            Check.equal(s.get(i).length, t.getSize(i), "size of sample " + i);
            Check.equal(1024, t.getDuration(i), "duration of sample " + i);
            byte[] copy = Arrays.copyOfRange(file, (int) t.getOffset(i), (int) t.getOffset(i) + t.getSize(i));
            Check.isTrue(Arrays.equals(s.get(i), copy), "content of sample " + i);
        }
    }

    /**
     * A fragmented file gives the same samples as the plain file.
     * <p>
     * 조각화된 파일은 일반 파일과 같은 샘플을 준다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFragmented() throws Exception {
        List<byte[]> s = samples(25);
        Mp4TestBuilder b = new Mp4TestBuilder();
        b.fragmented = true;
        b.perFragment = 7;
        byte[] file = b.build(s);
        Mp4Track t = Mp4File.parse(file).firstAudioTrack();
        Check.notNull(t, "audio track");
        Check.equal(25, t.getSampleCount(), "sample count");
        for (int i = 0; i < 25; i++) { // 샘플마다 내용을 확인한다
            byte[] copy = Arrays.copyOfRange(file, (int) t.getOffset(i), (int) t.getOffset(i) + t.getSize(i));
            Check.isTrue(Arrays.equals(s.get(i), copy), "content of sample " + i);
            Check.equal(1024, t.getDuration(i), "duration " + i);
        }
    }

    /**
     * Compares file-backed and array-backed tracks and reads a large opaque media box with a small heap.
     * <p>
     * 파일과 배열의 트랙을 비교하고 큰 미디어 박스를 작은 힙으로 읽는다.
     * @throws Exception on failure / 실패 시
     */
    public void testFileSampleTablesAndLargeMediaBox() throws Exception {
        File file = File.createTempFile("swp-mp4-source-", ".m4a");
        try {
            // 일반 파일은 mdat을 늘리고, 조각 파일은 여러 moof의 샘플을 파일 창을 건너 읽는다.
            for (int mode = 0; mode < 2; mode++) {
                Mp4TestBuilder builder = new Mp4TestBuilder();
                builder.fragmented = mode == 1;
                builder.perFragment = 4;
                builder.config = new byte[] { 0x12, 0x10 };
                byte[] encoded = builder.build(samples(17));
                Mp4Track memory = Mp4File.parse(encoded).firstAudioTrack();
                RandomAccessFile writable = new RandomAccessFile(file, "rw");
                try {
                    writable.setLength(0);
                    writable.write(encoded);
                    if (!builder.fragmented) {
                        long mdat = memory.getOffset(0) - 8;
                        int mdatLength = 72 * 1024 * 1024;
                        writable.setLength(mdat + mdatLength);
                        writable.seek(mdat);
                        writable.writeInt(mdatLength);
                    }
                } finally {
                    writable.close();
                }
                FileByteSource source = new FileByteSource(file, 19);
                try {
                    Check.isTrue(Mp4File.looksLikeMp4Source(source), "recognized MP4 header");
                    Mp4Track disk = Mp4File.parseSource(source).firstAudioTrack();
                    Check.equal(memory.getSampleCount(), disk.getSampleCount(), "sample count");
                    Check.isTrue(Arrays.equals(memory.getConfig(), disk.getConfig()), "codec config");
                    // 파일과 배열의 위치·길이·시간 정보를 샘플마다 비교한다.
                    for (int i = 0; i < disk.getSampleCount(); i++) {
                        Check.equal(memory.getOffset(i), disk.getOffset(i), "sample offset " + i);
                        Check.equal(memory.getSize(i), disk.getSize(i), "sample size " + i);
                        Check.equal(memory.getDuration(i), disk.getDuration(i), "sample duration " + i);
                    }
                    Check.isTrue(source.get(0) >= 0, "the caller still owns the input");
                } finally {
                    source.close();
                }
            }

            RandomAccessFile writable = new RandomAccessFile(file, "rw");
            try {
                writable.setLength(0);
                writable.writeInt(8);
                writable.writeBytes("free");
                writable.writeInt(1);
                writable.writeBytes("mdat");
                writable.writeLong(Long.MAX_VALUE);
            } finally {
                writable.close();
            }
            FileByteSource source = new FileByteSource(file, 13);
            try {
                Mp4File.parseSource(source);
                Check.fail("an overflowing extended box size must be rejected");
            } catch (MediaException expected) {
                Check.equal(MediaException.Kind.CORRUPT, expected.getKind(), "invalid extended box length");
            } finally {
                source.close();
            }
        } finally {
            Check.isTrue(file.delete(), "fixture is released");
        }
    }

    /**
     * An Opus sample entry gives its dOps configuration and format code.
     * <p>
     * Opus 샘플 엔트리는 dOps 구성과 형식 코드를 준다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testOpusEntry() throws Exception {
        Mp4TestBuilder b = new Mp4TestBuilder();
        b.format = "Opus";
        b.config = new byte[] { 0, 2, 1, 56, 0, 0, (byte) 0xBB, (byte) 0x80, 0, 0, 0 };
        b.timescale = 48000;
        b.sampleRate = 48000;
        Mp4Track t = Mp4File.parse(b.build(samples(3))).firstAudioTrack();
        Check.equal("Opus", t.getFormat(), "format");
        Check.isTrue(Arrays.equals(b.config, t.getConfig()), "dOps body");
    }

    /**
     * Damaged structures are reported as CORRUPT; non-MP4 data is not recognized.
     * <p>
     * 손상된 구조는 CORRUPT 로 보고되며 MP4 가 아닌 데이터는 인식하지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDamagedFiles() throws Exception {
        final byte[] good = new Mp4TestBuilder().build(samples(8));
        Check.isFalse(Mp4File.looksLikeMp4(new byte[40]), "zeros are not MP4");
        Check.isFalse(Mp4File.looksLikeMp4("RIFFxxxxWAVEfmt ".getBytes("US-ASCII")), "WAV is not MP4");
        final byte[] cut = Arrays.copyOf(good, good.length / 3);
        Check.throwsException(MediaException.class, new Block() {
            /**
             * Parses a truncated file. / 잘린 파일을 해석한다.
             *
             * @throws Exception always / 항상
             */
            public void run() throws Exception {
                Mp4File.parse(cut);
            }
        }, "truncated file");
        final byte[] bad = good.clone();
        bad[0] = (byte) 0xFF;
        bad[1] = (byte) 0xFF;
        Check.throwsException(MediaException.class, new Block() {
            /**
             * Parses a file whose first box is too large. / 첫 박스가 너무 큰 파일을 해석한다.
             *
             * @throws Exception always / 항상
             */
            public void run() throws Exception {
                Mp4File.parse(bad);
            }
        }, "oversized box");
        // 박스 크기를 바꿔도(여기서는 무작위로 훼손) 예외 말고는 멈추지 않거나 오류를 일으키지 않는다.
        java.util.Random r = new java.util.Random(4);
        for (int round = 0; round < 200; round++) { // 무작위 훼손을 200번 시험한다
            byte[] x = good.clone();
            for (int i = 0; i < 4; i++) {
                x[r.nextInt(x.length)] = (byte) r.nextInt(256);
            }
            try {
                Mp4File.parse(x);
            } catch (MediaException expected) {
                Check.isTrue(true, "damage is reported as an exception");
            }
        }
    }
}
