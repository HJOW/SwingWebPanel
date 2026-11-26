package org.duckdns.hjow.swingwebpanel.media.audio.webm;

import java.io.IOException;
import java.io.OutputStream;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.ByteSource;
import org.duckdns.hjow.swingwebpanel.media.audio.ogg.OggPageWriter;

/**
 * Wraps the Vorbis packets of a WebM / Matroska track into an Ogg stream so that the Vorbis decoder (which reads Ogg) can play
 * them. The three header packets come from the Xiph-laced codec private data; every audio packet gets its own Ogg page whose granule position
 * is computed exactly from the block sizes: the identification header gives the two block sizes and the mode table found at the end of the
 * setup header tells which block size a packet uses (the same backwards scan as other Matroska demultiplexers use, because the setup header
 * has no length field for the mode list).
 * <p>
 * WebM / Matroska 트랙의 Vorbis 패킷을 Ogg 스트림으로 감싸 Ogg 를 읽는 Vorbis 디코더가 재생할 수 있게 한다. 헤더 패킷 3개는 Xiph 레이싱된
 * 코덱 비공개 데이터에서 가져오고 오디오 패킷마다 Ogg 페이지 하나를 만들며 그래뉼 위치는 블록 크기에서 정확히 계산한다: 식별 헤더가 두 블록 크기를 주고
 * 설정 헤더 끝의 모드 표가 패킷이 쓰는 블록 크기를 알려 준다(설정 헤더에는 모드 목록의 길이 필드가 없으므로 다른 Matroska 역다중화기와 같이 뒤에서부터
 * 훑는다).
 */
final class VorbisInOgg {

    /**
     * Private constructor: static helpers. / 비공개 생성자: 정적 도우미.
     */
    private VorbisInOgg() {
    }

    /**
     * Builds the Ogg stream of a Vorbis track.
     * <p>
     * Vorbis 트랙의 Ogg 스트림을 만든다.
     *
     * @param t    the track / 트랙
     * @param data the file data holding the frames / 프레임이 있는 파일 데이터
     * @param out the Ogg stream output / Ogg 스트림 출력
     * @throws MediaException CORRUPT or UNSUPPORTED_FORMAT for damaged headers / 헤더가 손상되었으면 CORRUPT 또는 UNSUPPORTED_FORMAT
     * @throws IOException if writing fails / 쓰기에 실패하면
     */
    static void write(WebmFile.Track t, ByteSource data, OutputStream out) throws MediaException, IOException {
        byte[][] headers = splitHeaders(t.getCodecPrivate());
        if (headers[0].length < 30 || headers[0][0] != 1) {
            throw new MediaException(MediaException.Kind.CORRUPT, "invalid Vorbis identification header");
        }
        int sizes = headers[0][28] & 0xFF;
        int shortBlock = 1 << (sizes & 15);
        int longBlock = 1 << (sizes >> 4);
        boolean[] longFlags = modes(headers[2]);
        int modeBits = 0;
        while ((1 << modeBits) < longFlags.length) { // 모드 번호에 쓰는 비트 수
            modeBits++;
        }
        OggPageWriter writer = new OggPageWriter(out, 1);
        writer.packet(headers[0], 0);
        writer.flush(false);
        writer.packet(headers[1], 0);
        writer.packet(headers[2], 0);
        writer.flush(false);
        long granule = 0;
        int prev = -1;
        int n = t.getFrameCount();
        // 마지막 블록의 DiscardPadding(나노초)은 트랙 끝에서 재생하지 않는 샘플이다(ffmpeg 의 Matroska 먹서가 Vorbis 에도 쓴다). 마지막 쪽의 그래뉼 위치에서 뺀다.
        long rate = (headers[0][12] & 0xFFL) | ((headers[0][13] & 0xFFL) << 8) | ((headers[0][14] & 0xFFL) << 16) | ((headers[0][15] & 0xFFL) << 24);
        long discard = t.getDiscardPadding() > 0 ? (t.getDiscardPadding() * rate + 500000000L) / 1000000000L : 0;
        for (int i = 0; i < n; i++) { // 오디오 패킷마다 블록 크기로 그래뉼 위치를 구해 페이지를 쓴다
            if (Thread.currentThread().isInterrupted()) {
                throw new MediaException(MediaException.Kind.CANCELLED, "Vorbis remux was interrupted");
            }
            int offset = t.getOffset(i);
            int size = t.getSize(i);
            int first = size > 0 ? data.get(offset) : 1;
            int block = prev;
            if ((first & 1) == 0) {
                int mode = first >> 1 & ((1 << modeBits) - 1);
                if (modeBits > 7 && size > 1) {
                    mode = (first >> 1) | (data.get(offset + 1) << 7);
                    mode &= (1 << modeBits) - 1;
                }
                boolean isLong = mode < longFlags.length && longFlags[mode];
                block = isLong ? longBlock : shortBlock;
            }
            if (prev >= 0 && block >= 0) {
                granule += prev / 4 + block / 4;
            }
            if (block >= 0) {
                prev = block;
            }
            boolean last = i == n - 1;
            writer.packet(data, offset, size, last ? Math.max(0L, granule - discard) : granule);
            writer.flush(last);
        }
    }

    /**
     * Splits the Xiph-laced codec private data into the three Vorbis headers.
     * <p>
     * Xiph 레이싱된 코덱 비공개 데이터를 Vorbis 헤더 3개로 나눈다.
     *
     * @param cp codec private data / 코덱 비공개 데이터
     * @return identification, comment and setup header / 식별, 주석, 설정 헤더
     * @throws MediaException CORRUPT if the lacing is invalid / 레이싱이 올바르지 않으면 CORRUPT
     */
    static byte[][] splitHeaders(byte[] cp) throws MediaException {
        if (cp.length < 3 || cp[0] != 2) {
            throw new MediaException(MediaException.Kind.CORRUPT, "invalid Vorbis codec private data");
        }
        int pos = 1;
        int[] len = new int[3];
        for (int i = 0; i < 2; i++) { // 처음 두 패킷의 크기를 읽는다
            int s = 0;
            int b;
            do { // 크기 필드가 255 이면 다음 바이트를 더 읽는다
                if (pos >= cp.length) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "truncated Vorbis codec private data");
                }
                b = cp[pos++] & 0xFF;
                s += b;
            } while (b == 255);
            len[i] = s;
        }
        len[2] = cp.length - pos - len[0] - len[1];
        if (len[2] <= 0 || len[0] <= 0 || len[1] < 0) {
            throw new MediaException(MediaException.Kind.CORRUPT, "invalid Vorbis header sizes");
        }
        byte[][] h = new byte[3][];
        for (int i = 0; i < 3; i++) { // 헤더를 잘라 낸다
            h[i] = java.util.Arrays.copyOfRange(cp, pos, pos + len[i]);
            pos += len[i];
        }
        return h;
    }

    /**
     * Finds the block flags of the modes by scanning the end of the setup header backwards.
     * <p>
     * 설정 헤더의 끝을 뒤에서부터 훑어 모드의 블록 플래그를 찾는다.
     *
     * @param setup setup header / 설정 헤더
     * @return block flag (true = long) per mode / 모드별 블록 플래그(true = 긴 블록)
     * @throws MediaException UNSUPPORTED_FORMAT if the modes cannot be found / 모드를 찾을 수 없으면 UNSUPPORTED_FORMAT
     */
    static boolean[] modes(byte[] setup) throws MediaException {
        int last = -1;
        for (int i = setup.length * 8 - 1; i >= 0; i--) { // 마지막으로 1 인 비트(framing)를 찾는다
            if (bit(setup, i) == 1) {
                last = i;
                break;
            }
        }
        if (last < 0) {
            throw new MediaException(MediaException.Kind.CORRUPT, "empty Vorbis setup header");
        }
        for (int k = 64; k >= 1; k--) { // 가능한 모드 수를 큰 값부터 시험한다
            int firstBit = last - 41 * k;
            if (firstBit - 6 < 0) {
                continue;
            }
            if (bits(setup, firstBit - 6, 6) != k - 1) {
                continue;
            }
            boolean ok = true;
            boolean[] flags = new boolean[k];
            for (int m = 0; m < k && ok; m++) { // 모드마다 창/변환 종류가 0 인지 확인한다
                int b = firstBit + 41 * m;
                flags[m] = bit(setup, b) == 1;
                ok = bits(setup, b + 1, 16) == 0 && bits(setup, b + 17, 16) == 0;
            }
            if (ok) {
                return flags;
            }
        }
        throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "cannot find the Vorbis mode table");
    }

    /**
     * Reads a bit, least significant bit of each byte first.
     * <p>
     * 비트 하나를 읽는다(각 바이트의 최하위 비트가 먼저).
     *
     * @param d data / 데이터
     * @param i bit index / 비트 번호
     * @return 0 or 1 / 0 또는 1
     */
    private static int bit(byte[] d, int i) {
        return (d[i >> 3] >> (i & 7)) & 1;
    }

    /**
     * Reads up to 24 bits, least significant bit first.
     * <p>
     * 최대 24비트를 읽는다(최하위 비트가 먼저).
     *
     * @param d data / 데이터
     * @param i first bit / 첫 비트
     * @param n number of bits / 비트 수
     * @return the value / 값
     */
    private static int bits(byte[] d, int i, int n) {
        int v = 0;
        for (int k = 0; k < n; k++) { // 비트를 하나씩 모은다
            v |= bit(d, i + k) << k;
        }
        return v;
    }

}
