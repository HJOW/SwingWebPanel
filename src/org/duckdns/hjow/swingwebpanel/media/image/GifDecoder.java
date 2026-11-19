package org.duckdns.hjow.swingwebpanel.media.image;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * A pure Java decoder for GIF87a and GIF89a including animation (plan items P18-02, P18-03; FEATURE_MATRIX IMG-03).
 * <p>
 * It decodes the LZW data itself and composes every frame onto the logical screen. Supported: global and local colour tables, transparency
 * (graphic control extension), interlaced frames, disposal methods 0 to 3 (methods 4 to 7 are treated like 0), the NETSCAPE2.0 loop
 * extension (a loop count N gives N + 1 plays, no extension gives one play, 0 means forever). Decisions: a frame delay of 10 ms or less
 * (including 0) is replaced by 100 ms like browsers do; "restore to background" clears to transparent, not to the background colour; the
 * canvas is the logical screen, enlarged to the extent of the first frame when that is larger or when the screen size is 0, and later
 * frames are clipped to it; a palette index outside the colour table is transparent; a frame whose data ends early (end code before all
 * pixels) keeps the pixels that were decoded; a file that ends in the middle of a frame drops that frame and keeps the complete ones (CORRUPT
 * if there is none); a missing trailer is tolerated.
 * <p>
 * GIF87a 와 GIF89a(애니메이션 포함)를 위한 순수 Java 디코더(계획 P18-02, P18-03; FEATURE_MATRIX IMG-03).
 * <p>
 * LZW 데이터를 직접 풀고 모든 프레임을 논리 화면에 합성한다. 지원: 전역/지역 색 표, 투명도(그래픽 제어 확장), 인터레이스 프레임, 처리 방법
 * 0~3(4~7 은 0 처럼 다룸), NETSCAPE2.0 반복 확장(반복 횟수 N 은 N + 1 번 재생, 확장이 없으면 한 번, 0 이면 무한). 결정: 프레임 지연이 10 ms 이하
 * (0 포함)이면 브라우저처럼 100 ms 로 바꾼다. "배경으로 복원"은 배경색이 아니라 투명으로 지운다. 캔버스는 논리 화면이며 첫 프레임의 범위가 더 크거나
 * 화면 크기가 0 이면 첫 프레임 범위로 넓히고 이후 프레임은 캔버스에 맞춰 자른다. 색 표 밖의 팔레트 인덱스는 투명이다. 데이터가 일찍 끝난 프레임(모든
 * 픽셀 전에 종료 코드)은 디코딩된 픽셀을 유지한다. 프레임 중간에서 끝난 파일은 그 프레임을 버리고 완전한 프레임만 유지한다(하나도 없으면 CORRUPT).
 * 트레일러가 없는 것은 허용한다.
 */
public final class GifDecoder implements ImageDecoder {

    /**
     * Replacement delay for frames whose delay is 10 ms or less.
     * <p>
     * 지연이 10 ms 이하인 프레임에 쓰는 대체 지연.
     */
    private static final int MIN_DELAY_REPLACEMENT_MS = 100;

    /**
     * Reads the bytes of a chain of data sub-blocks.
     * <p>
     * 데이터 하위 블록 연쇄의 바이트를 읽는다.
     */
    private static final class SubBlocks {
        /**
         * File data.
         * <p>
         * 파일 데이터.
         */
        private final byte[] d;

        /**
         * Position of the next byte in the file.
         * <p>
         * 파일에서 다음 바이트의 위치.
         */
        int pos;

        /**
         * Bytes left in the current sub-block.
         * <p>
         * 현재 하위 블록에 남은 바이트 수.
         */
        private int left;

        /**
         * True once the terminating block was read.
         * <p>
         * 종료 블록을 읽었으면 true.
         */
        boolean terminated;

        /**
         * True if the file ended before the terminating block.
         * <p>
         * 종료 블록 전에 파일이 끝났으면 true.
         */
        boolean truncated;

        /**
         * Creates the reader.
         * <p>
         * 읽기 객체를 만든다.
         *
         * @param d   file data / 파일 데이터
         * @param pos position of the first sub-block length byte / 첫 하위 블록 길이 바이트의 위치
         */
        SubBlocks(byte[] d, int pos) {
            this.d = d;
            this.pos = pos;
        }

        /**
         * Returns the next data byte, or -1 at the end of the chain.
         * <p>
         * 다음 데이터 바이트를 반환하며 연쇄의 끝이면 -1 이다.
         *
         * @return byte value or -1 / 바이트 값 또는 -1
         */
        int next() {
            if (terminated || truncated) {
                return -1;
            }
            // 현재 하위 블록을 다 읽었으면 다음 블록의 길이를 읽는다.
            if (left == 0) {
                if (pos >= d.length) {
                    truncated = true;
                    return -1;
                }
                left = d[pos++] & 0xFF;
                if (left == 0) {
                    terminated = true;
                    return -1;
                }
            }
            if (pos >= d.length) {
                truncated = true;
                return -1;
            }
            left--;
            return d[pos++] & 0xFF;
        }

        /**
         * Skips to the end of the chain.
         * <p>
         * 연쇄의 끝까지 건너뛴다.
         */
        void skipAll() {
            // 종료 블록이나 파일 끝을 만날 때까지 한 블록씩 건너뛴다.
            while (!terminated && !truncated) {
                if (left > 0) {
                    int n = Math.min(left, d.length - pos);
                    pos += n;
                    left -= n;
                    if (left > 0) {
                        truncated = true;
                    }
                } else if (pos >= d.length) {
                    truncated = true;
                } else {
                    left = d[pos++] & 0xFF;
                    if (left == 0) {
                        terminated = true;
                    }
                }
            }
        }
    }

    /**
     * Creates a decoder.
     * <p>
     * 디코더를 만든다.
     */
    public GifDecoder() {
    }

    /**
     * Returns the format name.
     * <p>
     * 형식 이름을 반환한다.
     *
     * @return <code>gif</code>
     */
    public String getFormatName() {
        return "gif";
    }

    /**
     * Returns the MIME types.
     * <p>
     * MIME 타입을 반환한다.
     *
     * @return MIME types / MIME 타입
     */
    public String[] getMimeTypes() {
        return new String[] { "image/gif" };
    }

    /**
     * Checks the GIF87a/GIF89a signature.
     * <p>
     * GIF87a/GIF89a 시그니처를 확인한다.
     *
     * @param head first bytes / 첫 바이트
     * @return true for a GIF signature / GIF 시그니처이면 true
     */
    public boolean accepts(byte[] head) {
        return head != null && head.length >= 6 && head[0] == 'G' && head[1] == 'I' && head[2] == 'F' && head[3] == '8'
                && (head[4] == '7' || head[4] == '9') && head[5] == 'a';
    }

    /**
     * Decodes a GIF file.
     * <p>
     * GIF 파일을 디코딩한다.
     *
     * @param data   file data / 파일 데이터
     * @param limits limits / 제한
     * @return decoded image / 디코딩된 이미지
     * @throws MediaException on damaged or too large data / 손상되었거나 과대한 데이터일 때
     */
    public DecodedImage decode(byte[] data, DecodeLimits limits) throws MediaException {
        try {
            return decodeImpl(data, limits);
        } catch (RuntimeException e) {
            throw new MediaException(MediaException.Kind.CORRUPT, "damaged GIF data: " + e, e);
        } catch (OutOfMemoryError e) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "not enough memory to decode the GIF image");
        }
    }

    /**
     * The actual decoding.
     * <p>
     * 실제 디코딩.
     *
     * @param d      file data / 파일 데이터
     * @param limits limits / 제한
     * @return decoded image / 디코딩된 이미지
     * @throws MediaException on failure / 실패 시
     */
    private DecodedImage decodeImpl(byte[] d, DecodeLimits limits) throws MediaException {
        if (!accepts(d)) {
            throw new MediaException(MediaException.Kind.CORRUPT, "not a GIF file");
        }
        if (d.length > limits.getMaxBytes()) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "GIF data is too large");
        }
        int screenW = PixelOps.u16le(d, 6);
        int screenH = PixelOps.u16le(d, 8);
        int packed = d[10] & 0xFF;
        int pos = 13;
        int[] globalPalette = null;
        if ((packed & 0x80) != 0) {
            int count = 2 << (packed & 7);
            PixelOps.need(d, pos, 3L * count);
            globalPalette = readPalette(d, pos, count);
            pos += 3 * count;
        }
        List<DecodedFrame> frames = new ArrayList<DecodedFrame>();
        int[] canvas = null;
        int cw = 0;
        int ch = 0;
        int[] saved = null;
        int prevDispose = 0;
        int prevX = 0;
        int prevY = 0;
        int prevW = 0;
        int prevH = 0;
        int gceDispose = 0;
        int gceDelay = 0;
        int gceTransparent = -1;
        boolean netscape = false;
        int loops = 0;
        // 블록을 하나씩 읽는다: 확장(0x21), 이미지(0x2C), 트레일러(0x3B).
        while (pos < d.length) {
            PixelOps.checkCancelled();
            int block = d[pos++] & 0xFF;
            if (block == 0x3B) {
                break;
            } else if (block == 0x21) {
                if (pos >= d.length) {
                    break;
                }
                int label = d[pos++] & 0xFF;
                SubBlocks sb = new SubBlocks(d, pos);
                if (label == 0xF9) {
                    // 그래픽 제어 확장: 처리 방법, 투명 색 여부, 지연, 투명 색 인덱스.
                    int size = sb.pos < d.length ? d[sb.pos] & 0xFF : 0;
                    if (size >= 4 && sb.pos + 5 < d.length) {
                        int flags = d[sb.pos + 1] & 0xFF;
                        gceDispose = (flags >> 2) & 7;
                        gceDelay = (d[sb.pos + 2] & 0xFF) | ((d[sb.pos + 3] & 0xFF) << 8);
                        gceTransparent = (flags & 1) != 0 ? (d[sb.pos + 4] & 0xFF) : -1;
                    }
                    sb.skipAll();
                } else if (label == 0xFF) {
                    // 응용 확장: NETSCAPE2.0 이면 반복 횟수를 읽는다.
                    int size = sb.pos < d.length ? d[sb.pos] & 0xFF : 0;
                    boolean isNetscape = size == 11 && sb.pos + 12 <= d.length && matches(d, sb.pos + 1, "NETSCAPE2.0");
                    sb.skipAll();
                    if (isNetscape) {
                        int sub = sb.pos;
                        int p2 = findNetscapeLoop(d, pos + 12);
                        if (p2 >= 0) {
                            netscape = true;
                            loops = (d[p2] & 0xFF) | ((d[p2 + 1] & 0xFF) << 8);
                        }
                        sb.pos = sub;
                    }
                } else {
                    sb.skipAll();
                }
                pos = sb.pos;
                if (sb.truncated) {
                    break;
                }
            } else if (block == 0x2C) {
                PixelOps.need(d, pos, 9);
                int left = PixelOps.u16le(d, pos);
                int top = PixelOps.u16le(d, pos + 2);
                int fw = PixelOps.u16le(d, pos + 4);
                int fh = PixelOps.u16le(d, pos + 6);
                int flags = d[pos + 8] & 0xFF;
                pos += 9;
                int[] palette = globalPalette;
                if ((flags & 0x80) != 0) {
                    int count = 2 << (flags & 7);
                    if (pos + 3L * count > d.length) {
                        break;
                    }
                    palette = readPalette(d, pos, count);
                    pos += 3 * count;
                }
                if (pos >= d.length) {
                    break;
                }
                int minCode = d[pos++] & 0xFF;
                if (canvas == null) {
                    cw = Math.max(screenW, left + fw);
                    ch = Math.max(screenH, top + fh);
                    limits.checkImageSize(cw, ch);
                    canvas = new int[cw * ch];
                }
                if (palette == null) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "GIF frame without a colour table");
                }
                if (minCode < 2 || minCode > 8) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "invalid LZW code size " + minCode);
                }
                SubBlocks sb = new SubBlocks(d, pos);
                if (fw == 0 || fh == 0) {
                    sb.skipAll();
                    pos = sb.pos;
                    gceDispose = 0;
                    gceDelay = 0;
                    gceTransparent = -1;
                    if (sb.truncated) {
                        break;
                    }
                    continue;
                }
                limits.checkImageSize(fw, fh);
                byte[] index = new byte[fw * fh];
                int decoded = lzw(sb, minCode, index);
                sb.skipAll();
                pos = sb.pos;
                if (sb.truncated && decoded < index.length) {
                    break;
                }
                if (frames.size() >= limits.getMaxFrames() || (long) (frames.size() + 1) * cw * ch > limits.getMaxTotalPixels()) {
                    throw new MediaException(MediaException.Kind.TOO_LARGE, "the animation has too many frames or pixels");
                }
                // 직전 프레임의 처리 방법을 적용한다.
                if (!frames.isEmpty()) {
                    if (prevDispose == 2) {
                        clear(canvas, cw, ch, prevX, prevY, prevW, prevH);
                    } else if (prevDispose == 3 && saved != null) {
                        System.arraycopy(saved, 0, canvas, 0, canvas.length);
                    }
                }
                saved = gceDispose == 3 ? canvas.clone() : null;
                draw(canvas, cw, ch, index, decoded, fw, fh, left, top, (flags & 0x40) != 0, palette, gceTransparent);
                int delay = gceDelay * 10;
                frames.add(new DecodedFrame(canvas.clone(), delay <= 10 ? MIN_DELAY_REPLACEMENT_MS : delay));
                prevDispose = gceDispose;
                prevX = left;
                prevY = top;
                prevW = fw;
                prevH = fh;
                gceDispose = 0;
                gceDelay = 0;
                gceTransparent = -1;
                if (sb.truncated) {
                    break;
                }
            } else {
                // 알 수 없는 블록을 만나면 더 읽지 않는다.
                break;
            }
        }
        if (frames.isEmpty()) {
            throw new MediaException(MediaException.Kind.CORRUPT, "the GIF file contains no complete image");
        }
        int plays = netscape ? (loops == 0 ? 0 : loops + 1) : 1;
        return new DecodedImage(cw, ch, frames, plays);
    }

    /**
     * Finds the loop count bytes of a NETSCAPE2.0 extension.
     * <p>
     * NETSCAPE2.0 확장의 반복 횟수 바이트 위치를 찾는다.
     *
     * @param d     file data / 파일 데이터
     * @param start position of the first data sub-block after the application identifier / 응용 식별자 뒤 첫 데이터 하위 블록의 위치
     * @return position of the two loop count bytes, or -1 / 반복 횟수 두 바이트의 위치. 없으면 -1
     */
    private static int findNetscapeLoop(byte[] d, int start) {
        int p = start;
        // 하위 블록을 따라가며 길이 3, 번호 1 인 블록을 찾는다.
        while (p < d.length) {
            int n = d[p] & 0xFF;
            if (n == 0) {
                return -1;
            }
            if (n >= 3 && p + 3 < d.length && (d[p + 1] & 0xFF) == 1) {
                return p + 2;
            }
            p += n + 1;
        }
        return -1;
    }

    /**
     * Tells whether an ASCII text appears at a position.
     * <p>
     * 위치에 ASCII 문자열이 있는지 알려 준다.
     *
     * @param d    data / 데이터
     * @param pos  position / 위치
     * @param text ASCII text / ASCII 문자열
     * @return true if equal / 같으면 true
     */
    private static boolean matches(byte[] d, int pos, String text) {
        // 문자를 하나씩 비교한다.
        for (int i = 0; i < text.length(); i++) {
            if (pos + i >= d.length || d[pos + i] != (byte) text.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Reads a colour table as opaque ARGB values.
     * <p>
     * 색 표를 불투명 ARGB 값으로 읽는다.
     *
     * @param d     data / 데이터
     * @param pos   position of the first RGB triple / 첫 RGB 세 바이트의 위치
     * @param count number of entries / 항목 수
     * @return colours / 색
     */
    private static int[] readPalette(byte[] d, int pos, int count) {
        int[] pal = new int[count];
        // 항목마다 RGB 세 바이트를 읽는다.
        for (int i = 0; i < count; i++) {
            pal[i] = 0xFF000000 | ((d[pos + 3 * i] & 0xFF) << 16) | ((d[pos + 3 * i + 1] & 0xFF) << 8) | (d[pos + 3 * i + 2] & 0xFF);
        }
        return pal;
    }

    /**
     * Clears a rectangle of the canvas to transparent (clipped to the canvas).
     * <p>
     * 캔버스의 사각형을 투명으로 지운다(캔버스에 맞춰 자름).
     *
     * @param canvas canvas pixels / 캔버스 픽셀
     * @param cw     canvas width / 캔버스 가로
     * @param ch     canvas height / 캔버스 세로
     * @param x      left / 왼쪽
     * @param y      top / 위쪽
     * @param w      width / 가로
     * @param h      height / 세로
     */
    private static void clear(int[] canvas, int cw, int ch, int x, int y, int w, int h) {
        int x1 = Math.min(cw, x + w);
        int y1 = Math.min(ch, y + h);
        // 영역의 행마다 0 으로 채운다.
        for (int yy = y; yy < y1; yy++) {
            if (x < x1) {
                java.util.Arrays.fill(canvas, yy * cw + x, yy * cw + x1, 0);
            }
        }
    }

    /**
     * Draws the pixels of a frame onto the canvas, skipping transparent pixels and clipping to the canvas.
     * <p>
     * 프레임의 픽셀을 캔버스에 그린다. 투명 픽셀은 건너뛰고 캔버스에 맞춰 자른다.
     *
     * @param canvas      canvas pixels / 캔버스 픽셀
     * @param cw          canvas width / 캔버스 가로
     * @param ch          canvas height / 캔버스 세로
     * @param index       palette indexes in stored row order / 저장된 행 순서의 팔레트 인덱스
     * @param decoded     number of valid entries in index / index 의 유효한 항목 수
     * @param fw          frame width / 프레임 가로
     * @param fh          frame height / 프레임 세로
     * @param left        frame left / 프레임 왼쪽
     * @param top         frame top / 프레임 위쪽
     * @param interlaced  true if the rows are interlaced / 행이 인터레이스이면 true
     * @param palette     colours / 색
     * @param transparent transparent palette index or -1 / 투명 팔레트 인덱스 또는 -1
     */
    private static void draw(int[] canvas, int cw, int ch, byte[] index, int decoded, int fw, int fh, int left, int top, boolean interlaced,
            int[] palette, int transparent) {
        // 저장된 행마다 화면상의 행을 찾아 픽셀을 그린다.
        for (int row = 0; row < fh; row++) {
            int destRow = interlaced ? interlacedRow(row, fh) : row;
            int y = top + destRow;
            if (y >= ch) {
                continue;
            }
            // 각 항목을 차례로 처리한다.
            for (int x = 0; x < fw; x++) {
                int k = row * fw + x;
                int cx = left + x;
                if (k >= decoded || cx >= cw) {
                    break;
                }
                int ci = index[k] & 0xFF;
                if (ci != transparent && ci < palette.length) {
                    canvas[y * cw + cx] = palette[ci];
                }
            }
        }
    }

    /**
     * Maps the n-th stored row of an interlaced frame to its row on the screen.
     * <p>
     * 인터레이스 프레임에서 n 번째로 저장된 행이 화면의 어느 행인지 계산한다.
     *
     * @param n stored row number / 저장된 행 번호
     * @param h frame height / 프레임 세로
     * @return screen row / 화면의 행
     */
    private static int interlacedRow(int n, int h) {
        int pass1 = (h + 7) / 8;
        int pass2 = (h + 3) / 8;
        int pass3 = (h + 1) / 4;
        if (n < pass1) {
            return n * 8;
        }
        n -= pass1;
        if (n < pass2) {
            return 4 + n * 8;
        }
        n -= pass2;
        if (n < pass3) {
            return 2 + n * 4;
        }
        n -= pass3;
        return 1 + n * 2;
    }

    /**
     * Decodes LZW data into palette indexes.
     * <p>
     * LZW 데이터를 팔레트 인덱스로 푼다.
     *
     * @param sb      the data sub-blocks / 데이터 하위 블록
     * @param minCode the minimum code size / 최소 코드 크기
     * @param out     output indexes / 인덱스 출력
     * @return number of indexes written / 쓴 인덱스 수
     * @throws MediaException CORRUPT for an invalid code / 올바르지 않은 코드이면 CORRUPT
     */
    private static int lzw(SubBlocks sb, int minCode, byte[] out) throws MediaException {
        int clear = 1 << minCode;
        int eoi = clear + 1;
        int avail = clear + 2;
        int codeSize = minCode + 1;
        int codeMask = (1 << codeSize) - 1;
        short[] prefix = new short[4096];
        byte[] suffix = new byte[4096];
        byte[] stack = new byte[4097];
        int old = -1;
        int first = 0;
        int datum = 0;
        int bits = 0;
        int total = out.length;
        int outPos = 0;
        // 코드를 하나씩 읽어 문자열로 펼친다.
        while (outPos < total) {
            if ((outPos & 0xFFFF) == 0) {
                PixelOps.checkCancelled();
            }
            // 코드 하나를 만들 만큼 비트가 모일 때까지 바이트를 읽는다.
            while (bits < codeSize) {
                int b = sb.next();
                if (b < 0) {
                    return outPos;
                }
                datum |= b << bits;
                bits += 8;
            }
            int code = datum & codeMask;
            datum >>>= codeSize;
            bits -= codeSize;
            if (code == clear) {
                codeSize = minCode + 1;
                codeMask = (1 << codeSize) - 1;
                avail = clear + 2;
                old = -1;
                continue;
            }
            if (code == eoi) {
                return outPos;
            }
            if (old == -1) {
                if (code >= clear) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "invalid first LZW code");
                }
                out[outPos++] = (byte) code;
                old = code;
                first = code;
                continue;
            }
            int in = code;
            int sp = 0;
            if (code > avail) {
                throw new MediaException(MediaException.Kind.CORRUPT, "invalid LZW code");
            }
            if (code == avail) {
                stack[sp++] = (byte) first;
                code = old;
            }
            // 접두 연쇄를 따라 올라가며 문자를 스택에 쌓는다.
            while (code >= clear) {
                if (sp >= 4096) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "invalid LZW table");
                }
                stack[sp++] = suffix[code];
                code = prefix[code];
            }
            first = code & 0xFF;
            stack[sp++] = (byte) first;
            // 새 항목을 표에 추가하고 필요하면 코드 크기를 키운다.
            if (avail < 4096) {
                prefix[avail] = (short) old;
                suffix[avail] = (byte) first;
                avail++;
                if ((avail & codeMask) == 0 && avail < 4096) {
                    codeSize++;
                    codeMask = (1 << codeSize) - 1;
                }
            }
            old = in;
            // 스택의 문자를 거꾸로 꺼내 출력한다.
            while (sp > 0 && outPos < total) {
                out[outPos++] = stack[--sp];
            }
        }
        return outPos;
    }
}
