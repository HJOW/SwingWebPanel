package org.duckdns.hjow.swingwebpanel.media.audio.webm;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.ArrayByteSource;
import org.duckdns.hjow.swingwebpanel.media.audio.ByteSource;
import org.duckdns.hjow.swingwebpanel.media.audio.FileByteSource;
import org.duckdns.hjow.swingwebpanel.media.audio.MediaData;
import org.duckdns.hjow.swingwebpanel.media.audio.MediaSpool;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmSource;
import org.duckdns.hjow.swingwebpanel.media.audio.opus.OpusDecoder;
import org.duckdns.hjow.swingwebpanel.media.audio.reference.ReferenceFixture;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.AudioKit;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.VorbisWriter;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the WebM / Matroska demultiplexer {@link WebmFile} and the decoder {@link WebmDecoder} (plan item P18-07, FEATURE_MATRIX AUD-09):
 * lacing, unknown sizes, block groups, damaged files and Vorbis tracks decoded through the in-memory Ogg bridge and compared with the direct
 * Ogg Vorbis decode. The files come from {@link WebmTestBuilder}.
 * <p>
 * WebM / Matroska 역다중화기 {@link WebmFile} 과 디코더 {@link WebmDecoder} 시험(계획 P18-07, FEATURE_MATRIX AUD-09): 레이싱, 크기 모름, 블록 그룹, 손상된
 * 파일, 메모리 Ogg 다리를 거쳐 해독한 Vorbis 트랙과 Ogg Vorbis 직접 해독의 비교. 파일은 {@link WebmTestBuilder} 가 만든다.
 */
public class WebmDecoderTest {

    /**
     * Creates the suite (required by the runner). / 스위트를 만든다(실행기가 필요로 함).
     */
    public WebmDecoderTest() {
    }

    /**
     * Makes frames of different sizes.
     * <p>
     * 크기가 서로 다른 프레임을 만든다.
     *
     * @param n count / 개수
     * @return frames / 프레임
     */
    private static List<byte[]> frames(int n) {
        List<byte[]> l = new ArrayList<byte[]>();
        for (int i = 0; i < n; i++) { // 프레임마다 크기와 내용이 다르다(255 가 넘는 크기도 만든다)
            byte[] f = new byte[(i % 5 == 4) ? 300 + i : 3 + (i * 13) % 40];
            Arrays.fill(f, (byte) (i + 1));
            l.add(f);
        }
        return l;
    }

    /**
     * Frame times of 20 ms. / 20 ms 간격의 프레임 시각.
     *
     * @param n count / 개수
     * @return times / 시각
     */
    private static long[] times(int n) {
        long[] t = new long[n];
        for (int i = 0; i < n; i++) { // 20 ms 씩 증가한다
            t[i] = 20L * i;
        }
        return t;
    }

    /**
     * Frames come out intact for every lacing method, with known and unknown sizes and with block groups.
     * <p>
     * 모든 레이싱 방식, 크기를 아는 경우와 모르는 경우, 블록 그룹에서 프레임이 온전히 나온다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testLacingAndStructure() throws Exception {
        for (int lacing = 0; lacing <= 3; lacing++) { // 레이싱 방식마다
            List<byte[]> src = frames(23);
            if (lacing == 2) { // 고정 크기 레이싱은 한 블록의 프레임 크기가 모두 같아야 한다
                for (int i = 0; i < src.size(); i++) {
                    src.set(i, new byte[] { (byte) (i + 1), (byte) (i + 1), (byte) (i + 1), (byte) (i + 1), (byte) (i + 1) });
                }
            }
            for (int variant = 0; variant < 4; variant++) { // 크기 모름과 블록 그룹 조합마다
                WebmTestBuilder b = new WebmTestBuilder();
                b.lacing = lacing;
                b.unknownSizes = (variant & 1) != 0;
                b.blockGroups = (variant & 2) != 0;
                b.codecId = "A_OPUS";
                byte[] file = b.build(src, times(23));
                WebmFile f = WebmFile.parse(file);
                WebmFile.Track t = f.firstAudioTrack();
                String what = "lacing " + lacing + " variant " + variant;
                Check.notNull(t, what + ": audio track");
                Check.equal("webm", f.getDocType(), what + ": doc type");
                Check.equal("A_OPUS", t.getCodecId(), what + ": codec");
                Check.equal(2, t.getChannels(), what + ": channels");
                Check.near(44100.0, t.getSampleRate(), 0.001, what + ": rate");
                Check.equal(23, t.getFrameCount(), what + ": frame count");
                for (int i = 0; i < 23; i++) { // 프레임마다 내용과 시각을 확인한다
                    byte[] got = Arrays.copyOfRange(file, t.getOffset(i), t.getOffset(i) + t.getSize(i));
                    Check.isTrue(Arrays.equals(src.get(i), got), what + ": frame " + i);
                    if (lacing == 0) {
                        Check.equal(20000000L * i, t.getTime(i), what + ": time " + i);
                    }
                }
            }
        }
    }

    /**
     * Indexes laced blocks from disk across file-window boundaries and ignores incomplete trailing data.
     * <p>
     * 파일 창 경계를 가로지르는 레이싱 블록을 디스크에서 색인하고 끝의 불완전 데이터를 건너뛴다.
     * @throws Exception if writing or parsing fails / 파일 쓰기나 해석에 실패하면
     */
    public void testFileIndexWithLacingAndTruncation() throws Exception {
        File file = File.createTempFile("swp-webm-source-", ".webm");
        try {
            // 일반·Xiph·고정·EBML 레이싱에서 완전한 파일과 끝이 잘린 파일을 확인한다.
            for (int mode = 0; mode < 4; mode++) {
                WebmTestBuilder builder = new WebmTestBuilder();
                builder.codecId = "A_OPUS";
                builder.codecPrivate = new byte[] { 'O', 'p', 'u', 's' };
                builder.blockGroups = true;
                builder.unknownSizes = true;
                builder.lacing = mode;
                List<byte[]> packets = frames(9);
                if (mode == 2) {
                    // 고정 레이싱은 블록 내 길이를 같게 쓴다.
                    for (int i = 0; i < packets.size(); i++) {
                        packets.set(i, new byte[] { 3, (byte) i, 7 });
                    }
                }
                byte[] encoded = builder.build(packets, times(packets.size()));
                RandomAccessFile writable = new RandomAccessFile(file, "rw");
                try {
                    writable.setLength(0);
                    writable.write(encoded);
                } finally {
                    writable.close();
                }
                compareWebmIndex(file, encoded, packets.size());
                int cut = encoded.length - 2;
                writable = new RandomAccessFile(file, "rw");
                try {
                    writable.setLength(cut);
                } finally {
                    writable.close();
                }
                compareWebmIndex(file, Arrays.copyOf(encoded, cut), packets.size() - 1);
            }
        } finally {
            Check.isTrue(file.delete(), "caller can release the disk source");
        }
    }

    /**
     * Compares metadata and bounded packet positions for one fixture. / 시험 파일 하나의 메타데이터와 패킷 범위를 비교한다.
     * @param file encoded file / 인코딩 파일
     * @param encoded encoded array / 인코딩 배열
     * @param maxPackets maximum expected number of complete packets / 완전한 패킷의 최대 기대 개수
     * @throws Exception if the source cannot be opened or parsed / 원천을 열거나 해석할 수 없으면
     */
    private static void compareWebmIndex(File file, byte[] encoded, int maxPackets) throws Exception {
        WebmFile.Track mem = WebmFile.parse(encoded).firstAudioTrack();
        FileByteSource input = new FileByteSource(file, 13);
        try {
            Check.isTrue(WebmFile.looksLikeEbmlSource(input), "file signature is recognized");
            WebmFile.Track disk = WebmFile.parseSource(input).firstAudioTrack();
            Check.notNull(disk, "audio track in file");
            Check.equal(mem.getCodecId(), disk.getCodecId(), "codec");
            Check.isTrue(Arrays.equals(mem.getCodecPrivate(), disk.getCodecPrivate()), "codec private");
            Check.equal(mem.getFrameCount(), disk.getFrameCount(), "packet count");
            Check.isTrue(disk.getFrameCount() <= maxPackets, "truncated final block is not indexed");
            // 파일에서 읽은 범위가 배열의 색인과 일치하며 끝을 넘지 않아야 한다.
            for (int i = 0; i < disk.getFrameCount(); i++) {
                Check.equal(mem.getOffset(i), disk.getOffset(i), "packet offset");
                Check.equal(mem.getSize(i), disk.getSize(i), "packet size");
                Check.equal(mem.getTime(i), disk.getTime(i), "packet time");
                Check.isTrue(disk.getOffset(i) + disk.getSize(i) <= input.length(), "complete packet bytes");
            }
        } finally {
            input.close();
        }
    }

    /**
     * Codec private data, codec delay and non-audio tracks.
     * <p>
     * 코덱 비공개 데이터, 코덱 지연, 오디오가 아닌 트랙.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTrackProperties() throws Exception {
        WebmTestBuilder b = new WebmTestBuilder();
        b.codecId = "A_OPUS";
        b.codecPrivate = new byte[] { 'O', 'p', 'u', 's', 'H', 'e', 'a', 'd', 1, 2, 56, 1 };
        b.codecDelay = 6500000L;
        b.channels = 2;
        b.rate = 48000;
        WebmFile.Track t = WebmFile.parse(b.build(frames(5), times(5))).firstAudioTrack();
        Check.isTrue(Arrays.equals(b.codecPrivate, t.getCodecPrivate()), "codec private data");
        Check.equal(6500000L, t.getCodecDelay(), "codec delay");
        Check.near(48000.0, t.getSampleRate(), 0.001, "sampling frequency");
    }

    /**
     * Remuxes a track beyond the memory threshold and verifies that closing its PCM source deletes the Ogg file.
     * <p>
     * 메모리 문턱보다 큰 트랙을 재포장하고 PCM을 닫으면 Ogg 임시 파일이 삭제되는지 확인한다.
     * @throws Exception if I/O or decoding fails / 입출력 또는 해독에 실패하면
     */
    public void testDerivedOggSpillsAndClosesWithPcm() throws Exception {
        byte[] original = null;
        WebmFile.Track originalTrack = null;
        // 참조 WebM에서 실제 Opus 패킷과 초기화 기록을 가져온다.
        for (ReferenceFixture fixture : ReferenceFixture.ofGroup("webm")) {
            if (fixture.name.equals("opus_audio_64k_48000_webm_native")) {
                original = fixture.readFile();
                originalTrack = WebmFile.parse(original).firstAudioTrack();
            }
        }
        Check.notNull(originalTrack, "Opus fixture track");
        int offset = originalTrack.getOffset(0);
        byte[] frame = Arrays.copyOfRange(original, offset, offset + originalTrack.getSize(0));
        int repeats = (int) (MediaSpool.MEMORY_LIMIT / frame.length + 2);
        WebmTestBuilder builder = new WebmTestBuilder();
        builder.codecId = "A_OPUS";
        builder.codecPrivate = originalTrack.getCodecPrivate();
        builder.rate = 48000;
        byte[] encoded = builder.build(Collections.nCopies(repeats, frame), times(repeats));
        ByteSource input = new ArrayByteSource(encoded);
        WebmFile.Track longTrack = WebmFile.parseSource(input).firstAudioTrack();
        MediaData derived = WebmDecoder.remux(longTrack, input, true);
        try {
            File stored = derived.getFile();
            Check.notNull(stored, "derived audio spills to disk");
            Check.isTrue(stored.isFile() && derived.length() > MediaSpool.MEMORY_LIMIT, "derived Ogg exceeds the memory threshold");
            PcmSource pcm = new OpusDecoder().openSource(derived.openOwned(), DecodeLimits.DEFAULT);
            try {
                byte[] bytes = new byte[64];
                Check.equal(bytes.length, pcm.read(bytes, 0, bytes.length), "first frames from the derived file");
                pcm.seekToFrame(pcm.getFrameCount() - 32);
                Check.equal(bytes.length, pcm.read(bytes, 0, bytes.length), "frames near the end of the derived file");
            } finally {
                pcm.close();
            }
            Check.isTrue(derived.isReleased() && !stored.exists(), "closing PCM releases the derived file");
        } finally {
            derived.release();
            input.close();
        }
    }

    /**
     * Vorbis tracks of a WebM file decode exactly like the Ogg Vorbis file they came from.
     * <p>
     * WebM 파일의 Vorbis 트랙은 원본 Ogg Vorbis 파일과 똑같이 해독된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testVorbisTrack() throws Exception {
        for (int ch = 1; ch <= 2; ch++) { // 모노와 스테레오
            VorbisWriter.Config cfg = new VorbisWriter.Config();
            cfg.channels = ch;
            cfg.rate = 22050;
            cfg.blockShort = 256;
            cfg.blockLong = 1024;
            cfg.pattern = "LSLLSL";
            float[][] x = new float[ch][9000];
            for (int c = 0; c < ch; c++) { // 채널마다 다른 사인파
                for (int i = 0; i < x[c].length; i++) {
                    x[c][i] = (float) (0.4 * Math.sin(2 * Math.PI * (300 + 200 * c) * i / 22050.0));
                }
            }
            VorbisWriter.Result r = VorbisWriter.encode(x, cfg);
            // 헤더 3개를 Xiph 레이싱으로 코덱 비공개 데이터에 넣고 오디오 패킷을 프레임으로 넣는다.
            ByteArrayOutputStream cp = new ByteArrayOutputStream();
            cp.write(2);
            for (int i = 0; i < 2; i++) { // 처음 두 헤더의 크기
                int s = r.packets[i].length;
                while (s >= 255) {
                    cp.write(255);
                    s -= 255;
                }
                cp.write(s);
            }
            for (int i = 0; i < 3; i++) { // 헤더 본문
                cp.write(r.packets[i], 0, r.packets[i].length);
            }
            List<byte[]> audio = new ArrayList<byte[]>();
            long[] ms = new long[r.packets.length - 3];
            for (int i = 3; i < r.packets.length; i++) { // 오디오 패킷과 시각
                audio.add(r.packets[i]);
                ms[i - 3] = r.granules[i - 1] * 1000L / 22050;
            }
            WebmTestBuilder b = new WebmTestBuilder();
            b.codecId = "A_VORBIS";
            b.codecPrivate = cp.toByteArray();
            b.channels = ch;
            b.rate = 22050;
            byte[] file = b.build(audio, ms);
            short[] viaWebm = decodeWebm(file);
            short[] direct = AudioKit.toShorts(AudioKit.readAll(new org.duckdns.hjow.swingwebpanel.media.audio.vorbis.VorbisDecoder().open(r.file, DecodeLimits.DEFAULT)));
            Check.isTrue(viaWebm.length > 5000 * ch, "decoded length " + viaWebm.length);
            // WebM 에는 Ogg 의 마지막 그래뉼 자르기에 해당하는 정보가 없으므로 끝은 블록 경계까지 더 길 수 있다(앞부분은 같아야 한다).
            Check.isTrue(viaWebm.length >= direct.length, "not shorter than the Ogg file (" + ch + " channels)");
            Check.isTrue(viaWebm.length - direct.length < 2048 * ch, "at most one block longer (" + ch + " channels)");
            Check.isTrue(Arrays.equals(direct, Arrays.copyOf(viaWebm, direct.length)), "same samples as the Ogg file (" + ch + " channels)");
        }
    }

    /**
     * Decodes a WebM file completely.
     * <p>
     * WebM 파일을 끝까지 해독한다.
     *
     * @param file data / 데이터
     * @return samples / 샘플
     * @throws MediaException on failure / 실패 시
     */
    private static short[] decodeWebm(byte[] file) throws MediaException {
        PcmSource s = new WebmDecoder().open(file, DecodeLimits.DEFAULT);
        try {
            return AudioKit.toShorts(AudioKit.readAll(s));
        } finally {
            s.close();
        }
    }

    /**
     * Unsupported codecs, damaged files and non-EBML data.
     * <p>
     * 지원하지 않는 코덱, 손상된 파일, EBML 이 아닌 데이터.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testUnsupportedAndDamaged() throws Exception {
        WebmTestBuilder b = new WebmTestBuilder();
        b.codecId = "A_AAC";
        final byte[] aac = b.build(frames(6), times(6));
        MediaException e = (MediaException) Check.throwsException(MediaException.class, new Block() {
            /**
             * Opens a track with an unsupported codec. / 지원하지 않는 코덱의 트랙을 연다.
             *
             * @throws Exception always / 항상
             */
            public void run() throws Exception {
                new WebmDecoder().open(aac, DecodeLimits.DEFAULT);
            }
        }, "unsupported codec");
        Check.equal(MediaException.Kind.UNSUPPORTED_FORMAT, e.getKind(), "kind");
        Check.isFalse(new WebmDecoder().accepts(new byte[200]), "zeros are not WebM");
        Check.isFalse(new WebmDecoder().accepts("OggS-not-ebml".getBytes("US-ASCII")), "Ogg is not WebM");
        Check.isTrue(new WebmDecoder().accepts(aac), "an EBML file with an audio track is accepted");
        Random r = new Random(8);
        byte[] good = aac;
        for (int round = 0; round < 300; round++) { // 무작위 훼손과 잘라 내기를 견디는지 본다
            byte[] x = Arrays.copyOf(good, round % 3 == 0 ? Math.max(8, r.nextInt(good.length)) : good.length);
            for (int i = 0; i < 3; i++) {
                x[r.nextInt(x.length)] = (byte) r.nextInt(256);
            }
            try {
                WebmFile.parse(x);
            } catch (MediaException expected) {
                Check.isTrue(true, "damage is reported as an exception");
            }
        }
    }

    /**
     * Format name and MIME types.
     * <p>
     * 형식 이름과 MIME 타입.
     */
    public void testIdentity() {
        WebmDecoder d = new WebmDecoder();
        Check.equal("webm", d.getFormatName(), "format name");
        Check.isTrue(Arrays.asList(d.getMimeTypes()).contains("audio/webm"), "audio/webm");
    }
}
