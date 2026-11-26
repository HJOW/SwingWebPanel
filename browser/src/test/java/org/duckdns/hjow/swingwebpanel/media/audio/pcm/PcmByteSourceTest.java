package org.duckdns.hjow.swingwebpanel.media.audio.pcm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.ByteSourceException;
import org.duckdns.hjow.swingwebpanel.media.audio.FileByteSource;
import org.duckdns.hjow.swingwebpanel.media.audio.FloatPcmSource;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmSource;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Compares PCM decoding from arrays and file windows across block boundaries.
 * <p>
 * 블록 경계에 걸친 배열 입력과 파일 창 입력의 PCM 디코딩을 비교한다.
 */
public final class PcmByteSourceTest {

    /** Creates the suite. / 시험 스위트를 만든다. */
    public PcmByteSourceTest() {
    }

    /**
     * Writes a temporary encoded fixture. / 임시 인코딩 시험 파일을 쓴다.
     * @param data encoded data / 인코딩 데이터
     * @return the temporary file / 임시 파일
     * @throws IOException if writing failed / 쓰기에 실패하면
     */
    private static File fixture(byte[] data) throws IOException {
        File file = File.createTempFile("swp-pcm-source-", ".data");
        boolean written = false;
        try {
            FileOutputStream out = new FileOutputStream(file);
            try {
                out.write(data);
            } finally {
                out.close();
            }
            written = true;
            return file;
        } finally {
            if (!written) {
                file.delete();
            }
        }
    }

    /**
     * Compares interleaved PCM, seeking and float channels using uneven file windows and a short last frame.
     * <p>
     * 고르지 않은 파일 창과 마지막 짧은 프레임을 사용해 교차 PCM, 탐색 및 float 채널을 비교한다.
     * @throws Exception if I/O or decoding fails / 입출력 또는 디코딩에 실패하면
     */
    public void testRawBlocksAndWideChannelsMatchTheArray() throws Exception {
        int frames = 4100;
        int start = 7;
        byte[] encoded = new byte[start + frames * 4 + 1];
        // 원문의 프레임마다 내용이 달라야 블록 시작과 탐색의 오프셋 오류가 드러난다.
        for (int i = start; i < encoded.length; i++) {
            encoded[i] = (byte) (i * 79 + 17);
        }
        File file = fixture(encoded);
        try {
            FileByteSource input = new FileByteSource(file, 31);
            PcmSource disk = (PcmSource) PcmSources.raw(false, input, start, encoded.length, SampleFormat.S16LE, 2, 8000, null);
            PcmSource mem = (PcmSource) PcmSources.raw(false, encoded, start, encoded.length, SampleFormat.S16LE, 2, 8000, null);
            try {
                Check.equal(frames, disk.getFrameCount(), "incomplete final frame is ignored");
                comparePcm(mem, disk, frames * 4);
                mem.seekToFrame(2047);
                disk.seekToFrame(2047);
                comparePcm(mem, disk, 68);
            } finally {
                disk.close();
                mem.close();
            }
            try {
                input.get(0);
                Check.fail("closing the decoder must close the owned file source");
            } catch (ByteSourceException expected) {
                Check.isTrue(expected.getMessage().indexOf("closed") >= 0, "the decoder closed its input");
            }

            FloatPcmSource diskWide = (FloatPcmSource) PcmSources.raw(true, new FileByteSource(file, 37), start, encoded.length,
                    SampleFormat.S16LE, 2, 8000, null);
            FloatPcmSource memWide = (FloatPcmSource) PcmSources.raw(true, encoded, start, encoded.length, SampleFormat.S16LE, 2, 8000, null);
            try {
                float[][] actual = new float[2][frames];
                float[][] expected = new float[2][frames];
                Check.equal(frames, memWide.read(expected, 0, frames), "wide array frames");
                Check.equal(frames, diskWide.read(actual, 0, frames), "wide file frames");
                Check.isTrue(Arrays.equals(expected[0], actual[0]) && Arrays.equals(expected[1], actual[1]), "wide channels match");
            } finally {
                diskWide.close();
                memWide.close();
            }
        } finally {
            Check.isTrue(file.delete(), "the fixture can be deleted after all readers close");
        }
    }

    /**
     * Compares a block decoder with a short final IMA block and reports a closed input as an access failure.
     * <p>
     * 마지막 IMA 블록이 짧은 디코더를 비교하고 닫힌 입력을 접근 실패로 보고하는지 확인한다.
     * @throws Exception if I/O or decoding fails / 입출력 또는 디코딩에 실패하면
     */
    public void testShortAdpcmBlockAndInputFailure() throws Exception {
        byte[] encoded = {9, 8, 7, 1, 0, 0, 0, 0x23, 0x45, 0x67, 0x12, 7, 0, 0, 0};
        File file = fixture(encoded);
        FileByteSource input = new FileByteSource(file, 5);
        PcmSource mem = (PcmSource) PcmSources.adpcm(false, encoded, 3, encoded.length, AdpcmSource.IMA, 1, 8000, 8, 9, null, null);
        PcmSource disk = (PcmSource) PcmSources.adpcm(false, input, 3, encoded.length, AdpcmSource.IMA, 1, 8000, 8, 9, null, null);
        try {
            Check.equal(10, disk.getFrameCount(), "one complete block and one predictor sample");
            comparePcm(mem, disk, 20);
            mem.seekToFrame(8);
            disk.seekToFrame(8);
            comparePcm(mem, disk, 4);
            disk.seekToFrame(0);
            input.close();
            try {
                disk.read(new byte[2], 0, 2);
                Check.fail("a closed byte source cannot provide a new block");
            } catch (MediaException expected) {
                Check.equal(MediaException.Kind.ACCESS_FAILED, expected.getKind(), "closed input is an access failure");
            }
        } finally {
            disk.close();
            mem.close();
            Check.isTrue(file.delete(), "the ADPCM fixture can be deleted");
        }
    }

    /**
     * Reads the same number of bytes from both decoders. / 두 디코더에서 같은 바이트 수를 읽는다.
     * @param mem array decoder / 배열 디코더
     * @param disk file decoder / 파일 디코더
     * @param bytes expected byte count / 기대하는 바이트 수
     * @throws MediaException if decoding fails / 디코딩이 실패하면
     */
    private static void comparePcm(PcmSource mem, PcmSource disk, int bytes) throws MediaException {
        byte[] actual = new byte[bytes];
        byte[] expected = new byte[bytes];
        Check.equal(bytes, mem.read(expected, 0, bytes), "array byte count");
        Check.equal(bytes, disk.read(actual, 0, bytes), "file byte count");
        Check.isTrue(Arrays.equals(expected, actual), "PCM matches byte for byte");
    }
}
