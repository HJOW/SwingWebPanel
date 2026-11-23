package org.duckdns.hjow.swingwebpanel.media.image;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/**
 * Builds PNG and APNG files for tests from raw sample values: every colour type and bit depth, Adam7 interlacing, all five row filters,
 * palette and transparency chunks, several IDAT chunks and APNG frames. It is an independent encoder, so the decoder is checked against a
 * second implementation of the format.
 * <p>
 * 시험용 PNG/APNG 파일을 원시 샘플 값에서 만든다: 모든 색 유형과 비트 심도, Adam7 인터레이스, 다섯 가지 행 필터, 팔레트/투명도 청크, 여러 IDAT 청크,
 * APNG 프레임. 독립된 인코더이므로 디코더를 형식의 두 번째 구현과 대조하게 된다.
 */
final class PngBuilder {

    /**
     * The description of one PNG image.
     * <p>
     * PNG 이미지 하나의 설명.
     */
    static final class Spec {
        /**
         * Width in pixels.
         * <p>
         * 가로.
         */
        int width;

        /**
         * Height in pixels.
         * <p>
         * 세로.
         */
        int height;

        /**
         * Bit depth.
         * <p>
         * 비트 심도.
         */
        int depth = 8;

        /**
         * Colour type.
         * <p>
         * 색 유형.
         */
        int colorType = 6;

        /**
         * True for Adam7 interlacing.
         * <p>
         * Adam7 인터레이스이면 true.
         */
        boolean interlace;

        /**
         * Sample values, row by row, channel interleaved, each in 0 to 2^depth - 1.
         * <p>
         * 샘플 값. 행 순서, 채널 인터리브, 각각 0~2^depth - 1.
         */
        int[] samples;

        /**
         * Palette RGB bytes or null.
         * <p>
         * 팔레트 RGB 바이트 또는 null.
         */
        byte[] plte;

        /**
         * tRNS data or null.
         * <p>
         * tRNS 데이터 또는 null.
         */
        byte[] trns;

        /**
         * Row filter 0 to 4, or -1 to cycle through all five.
         * <p>
         * 행 필터 0~4. -1 이면 다섯 가지를 돌아가며 쓴다.
         */
        int filter = -1;

        /**
         * Maximum size of one IDAT chunk, 0 for a single chunk.
         * <p>
         * IDAT 청크 하나의 최대 크기. 0 이면 하나의 청크.
         */
        int idatSize;

        /**
         * Extra chunks written between IHDR and the first IDAT (each a complete chunk including CRC).
         * <p>
         * IHDR 과 첫 IDAT 사이에 쓰는 추가 청크(CRC 를 포함한 완전한 청크).
         */
        List<byte[]> extraChunks = new ArrayList<byte[]>();

        /**
         * Creates a description.
         * <p>
         * 설명을 만든다.
         *
         * @param w width / 가로
         * @param h height / 세로
         */
        Spec(int w, int h) {
            this.width = w;
            this.height = h;
        }

        /**
         * Returns the number of channels of the colour type.
         * <p>
         * 색 유형의 채널 수를 반환한다.
         *
         * @return channels / 채널 수
         */
        int channels() {
            switch (colorType) {
            case 0:
                return 1;
            case 2:
                return 3;
            case 3:
                return 1;
            case 4:
                return 2;
            default:
                return 4;
            }
        }
    }

    /**
     * One APNG frame.
     * <p>
     * APNG 프레임 하나.
     */
    static final class FrameSpec {
        /**
         * Frame width.
         * <p>
         * 프레임 가로.
         */
        int w;

        /**
         * Frame height.
         * <p>
         * 프레임 세로.
         */
        int h;

        /**
         * Horizontal offset on the canvas.
         * <p>
         * 가로 위치.
         */
        int x;

        /**
         * Vertical offset on the canvas.
         * <p>
         * 세로 위치.
         */
        int y;

        /**
         * Delay numerator.
         * <p>
         * 지연 분자.
         */
        int delayNum = 1;

        /**
         * Delay denominator.
         * <p>
         * 지연 분모.
         */
        int delayDen = 10;

        /**
         * Dispose operation.
         * <p>
         * 처리 방법.
         */
        int dispose;

        /**
         * Blend operation.
         * <p>
         * 합성 방법.
         */
        int blend;

        /**
         * Samples of the frame (same layout as {@link Spec#samples}).
         * <p>
         * 프레임의 샘플({@link Spec#samples} 와 같은 배치).
         */
        int[] samples;

        /**
         * Creates a frame.
         * <p>
         * 프레임을 만든다.
         *
         * @param w       width / 가로
         * @param h       height / 세로
         * @param x       x offset / 가로 위치
         * @param y       y offset / 세로 위치
         * @param samples sample values / 샘플 값
         */
        FrameSpec(int w, int h, int x, int y, int[] samples) {
            this.w = w;
            this.h = h;
            this.x = x;
            this.y = y;
            this.samples = samples;
        }
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private PngBuilder() {
    }

    /**
     * Builds a complete chunk (length, type, data, CRC).
     * <p>
     * 완전한 청크(길이, 유형, 데이터, CRC)를 만든다.
     *
     * @param type four letter type / 네 글자 유형
     * @param data chunk data / 청크 데이터
     * @return the chunk bytes / 청크 바이트
     */
    static byte[] chunk(String type, byte[] data) {
        Bytes b = new Bytes();
        b.be32(data.length);
        b.ascii(type);
        b.put(data);
        CRC32 crc = new CRC32();
        // 각 항목을 차례로 처리한다.
        for (int i = 0; i < 4; i++) {
            crc.update(type.charAt(i));
        }
        crc.update(data, 0, data.length);
        b.be32((int) crc.getValue());
        return b.toBytes();
    }

    /**
     * Encodes a still image.
     * <p>
     * 정지 이미지를 인코딩한다.
     *
     * @param s the description / 설명
     * @return PNG file bytes / PNG 파일 바이트
     */
    static byte[] encode(Spec s) {
        Bytes out = new Bytes();
        out.put(new byte[] { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A });
        out.put(chunk("IHDR", header(s)));
        // 추가 청크와 팔레트, 투명도 순서로 IDAT 앞에 쓴다.
        for (byte[] c : s.extraChunks) {
            out.put(c);
        }
        if (s.plte != null) {
            out.put(chunk("PLTE", s.plte));
        }
        if (s.trns != null) {
            out.put(chunk("tRNS", s.trns));
        }
        byte[] z = deflate(rawStream(s, s.samples, s.width, s.height));
        writeData(out, "IDAT", z, s.idatSize, -1);
        out.put(chunk("IEND", new byte[0]));
        return out.toBytes();
    }

    /**
     * Encodes an animation.
     * <p>
     * 애니메이션을 인코딩한다.
     *
     * @param s                   image description (the canvas and the default image) / 이미지 설명(캔버스와 기본 이미지)
     * @param numPlays            acTL play count / acTL 재생 횟수
     * @param defaultIsFirstFrame true if the default image (IDAT) is the first frame / 기본 이미지(IDAT)가 첫 프레임이면 true
     * @param frames              the animation frames; if the default image is the first frame, the first entry describes it / 애니메이션 프레임. 기본 이미지가 첫 프레임이면 첫 항목이 그것을 설명한다
     * @param badSequence         true to write a wrong sequence number in the second fcTL / true 이면 두 번째 fcTL 에 틀린 순서 번호를 쓴다
     * @return APNG file bytes / APNG 파일 바이트
     */
    static byte[] encodeApng(Spec s, int numPlays, boolean defaultIsFirstFrame, List<FrameSpec> frames, boolean badSequence) {
        Bytes out = new Bytes();
        out.put(new byte[] { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A });
        out.put(chunk("IHDR", header(s)));
        // 각 항목을 차례로 처리한다.
        for (byte[] c : s.extraChunks) {
            out.put(c);
        }
        if (s.plte != null) {
            out.put(chunk("PLTE", s.plte));
        }
        if (s.trns != null) {
            out.put(chunk("tRNS", s.trns));
        }
        out.put(chunk("acTL", new Bytes().be32(frames.size()).be32(numPlays).toBytes()));
        int seq = 0;
        int start = 0;
        if (defaultIsFirstFrame) {
            out.put(chunk("fcTL", frameControl(seq++, frames.get(0))));
            start = 1;
        }
        // 기본 이미지: 첫 프레임이거나 별도 이미지이다.
        int[] defSamples = defaultIsFirstFrame ? frames.get(0).samples : s.samples;
        out.put(chunk("IDAT", deflate(rawStream(s, defSamples, s.width, s.height))));
        // 나머지 프레임은 fcTL 과 fdAT 로 쓴다.
        for (int i = start; i < frames.size(); i++) {
            FrameSpec f = frames.get(i);
            int fseq = seq++;
            if (badSequence && i == start + 1) {
                fseq += 5;
            }
            out.put(chunk("fcTL", frameControl(fseq, f)));
            Spec fs = new Spec(f.w, f.h);
            fs.depth = s.depth;
            fs.colorType = s.colorType;
            fs.interlace = s.interlace;
            fs.filter = s.filter;
            byte[] z = deflate(rawStream(fs, f.samples, f.w, f.h));
            out.put(chunk("fdAT", new Bytes().be32(seq++).put(z).toBytes()));
        }
        out.put(chunk("IEND", new byte[0]));
        return out.toBytes();
    }

    /**
     * Builds the IHDR data.
     * <p>
     * IHDR 데이터를 만든다.
     *
     * @param s the description / 설명
     * @return header data / 헤더 데이터
     */
    private static byte[] header(Spec s) {
        return new Bytes().be32(s.width).be32(s.height).u8(s.depth).u8(s.colorType).u8(0).u8(0).u8(s.interlace ? 1 : 0).toBytes();
    }

    /**
     * Builds the fcTL data of a frame.
     * <p>
     * 프레임의 fcTL 데이터를 만든다.
     *
     * @param seq sequence number / 순서 번호
     * @param f   the frame / 프레임
     * @return chunk data / 청크 데이터
     */
    private static byte[] frameControl(int seq, FrameSpec f) {
        return new Bytes().be32(seq).be32(f.w).be32(f.h).be32(f.x).be32(f.y).be16(f.delayNum).be16(f.delayDen).u8(f.dispose).u8(f.blend)
                .toBytes();
    }

    /**
     * Writes compressed data as one or several chunks.
     * <p>
     * 압축 데이터를 하나 또는 여러 청크로 쓴다.
     *
     * @param out  output / 출력
     * @param type chunk type / 청크 유형
     * @param z    compressed data / 압축 데이터
     * @param size maximum chunk size, 0 for one chunk / 청크의 최대 크기. 0 이면 하나
     * @param seq  unused (-1) / 쓰지 않음(-1)
     */
    private static void writeData(Bytes out, String type, byte[] z, int size, int seq) {
        if (size <= 0 || z.length <= size) {
            out.put(chunk(type, z));
            return;
        }
        // 지정한 크기로 잘라 여러 청크로 쓴다.
        for (int p = 0; p < z.length; p += size) {
            int n = Math.min(size, z.length - p);
            byte[] part = new byte[n];
            System.arraycopy(z, p, part, 0, n);
            out.put(chunk(type, part));
        }
    }

    /**
     * Compresses data with zlib.
     * <p>
     * 데이터를 zlib 으로 압축한다.
     *
     * @param raw data / 데이터
     * @return compressed data / 압축 데이터
     */
    static byte[] deflate(byte[] raw) {
        Deflater d = new Deflater(6);
        d.setInput(raw);
        d.finish();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        // 압축이 끝날 때까지 출력을 모은다.
        while (!d.finished()) {
            int n = d.deflate(buf);
            bos.write(buf, 0, n);
        }
        d.end();
        return bos.toByteArray();
    }

    /**
     * Builds the filtered scanline stream (all Adam7 passes when interlaced).
     * <p>
     * 필터를 적용한 스캔라인 스트림을 만든다(인터레이스이면 Adam7 의 모든 단계).
     *
     * @param s       the description (depth, colour type, interlace, filter) / 설명(심도, 색 유형, 인터레이스, 필터)
     * @param samples samples of the image / 이미지의 샘플
     * @param w       width / 가로
     * @param h       height / 세로
     * @return raw stream before compression / 압축 전 원시 스트림
     */
    static byte[] rawStream(Spec s, int[] samples, int w, int h) {
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        if (!s.interlace) {
            pass(raw, s, samples, w, h, 0, 0, 1, 1);
        } else {
            int[] x0 = { 0, 4, 0, 2, 0, 1, 0 };
            int[] y0 = { 0, 0, 4, 0, 2, 0, 1 };
            int[] dx = { 8, 8, 4, 4, 2, 2, 1 };
            int[] dy = { 8, 8, 8, 4, 4, 2, 2 };
            // 일곱 단계마다 해당 픽셀만 모아 행으로 만든다.
            for (int p = 0; p < 7; p++) {
                pass(raw, s, samples, w, h, x0[p], y0[p], dx[p], dy[p]);
            }
        }
        return raw.toByteArray();
    }

    /**
     * Writes the rows of one pass.
     * <p>
     * 한 단계의 행을 쓴다.
     *
     * @param raw     output / 출력
     * @param s       the description / 설명
     * @param samples samples of the image / 이미지의 샘플
     * @param w       image width / 이미지 가로
     * @param h       image height / 이미지 세로
     * @param x0      first column / 첫 열
     * @param y0      first row / 첫 행
     * @param dx      column step / 열 간격
     * @param dy      row step / 행 간격
     */
    private static void pass(ByteArrayOutputStream raw, Spec s, int[] samples, int w, int h, int x0, int y0, int dx, int dy) {
        if (x0 >= w || y0 >= h) {
            return;
        }
        int pw = (w - x0 + dx - 1) / dx;
        int ph = (h - y0 + dy - 1) / dy;
        int ch = s.channels();
        int bits = ch * s.depth;
        int rowBytes = (pw * bits + 7) / 8;
        int bpp = Math.max(1, bits / 8);
        byte[] prev = new byte[rowBytes];
        // 단계의 행마다 샘플을 비트로 채워 넣고 필터를 적용한다.
        for (int y = 0; y < ph; y++) {
            byte[] row = new byte[rowBytes];
            // 각 항목을 차례로 처리한다.
            for (int i = 0; i < pw; i++) {
                int px = x0 + i * dx;
                int py = y0 + y * dy;
                // 각 항목을 차례로 처리한다.
                for (int c = 0; c < ch; c++) {
                    int v = samples[(py * w + px) * ch + c];
                    int bitPos = (i * ch + c) * s.depth;
                    if (s.depth == 16) {
                        row[bitPos / 8] = (byte) (v >> 8);
                        row[bitPos / 8 + 1] = (byte) v;
                    } else if (s.depth == 8) {
                        row[bitPos / 8] = (byte) v;
                    } else {
                        row[bitPos / 8] |= (byte) (v << (8 - s.depth - (bitPos & 7)));
                    }
                }
            }
            int type = s.filter >= 0 ? s.filter : y % 5;
            raw.write(type);
            byte[] f = filter(type, row, prev, bpp);
            raw.write(f, 0, f.length);
            prev = row;
        }
    }

    /**
     * Applies a row filter.
     * <p>
     * 행 필터를 적용한다.
     *
     * @param type filter type / 필터 유형
     * @param row  raw row / 원본 행
     * @param prev previous raw row / 이전 원본 행
     * @param bpp  bytes per pixel / 픽셀당 바이트 수
     * @return filtered row / 필터가 적용된 행
     */
    private static byte[] filter(int type, byte[] row, byte[] prev, int bpp) {
        byte[] out = new byte[row.length];
        // 바이트마다 유형에 맞는 예측값을 뺀다.
        for (int i = 0; i < row.length; i++) {
            int a = i >= bpp ? row[i - bpp] & 0xFF : 0;
            int b = prev[i] & 0xFF;
            int c = i >= bpp ? prev[i - bpp] & 0xFF : 0;
            int pred;
            switch (type) {
            case 1:
                pred = a;
                break;
            case 2:
                pred = b;
                break;
            case 3:
                pred = (a + b) >> 1;
                break;
            case 4:
                int p = a + b - c;
                int pa = Math.abs(p - a);
                int pb = Math.abs(p - b);
                int pc = Math.abs(p - c);
                pred = (pa <= pb && pa <= pc) ? a : (pb <= pc ? b : c);
                break;
            default:
                pred = 0;
                break;
            }
            out[i] = (byte) ((row[i] & 0xFF) - pred);
        }
        return out;
    }
}
