package org.duckdns.hjow.swingwebpanel.media.image.webp;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * The decoder of the WebP lossless bitstream (VP8L; plan item P18-03; "WebP Lossless Bitstream Specification"). A stream consists of an
 * optional chain of transforms (predictor, cross-colour, subtract-green, colour indexing) followed by an entropy-coded image of ARGB pixels
 * that uses prefix codes, LZ77 backward references with a 2-D distance map and an optional colour cache. The image data may use several sets
 * of prefix codes that are chosen per block by a "meta" image; the transform data and the meta image are themselves entropy-coded images. The
 * decoder reads the transforms, decodes the pixels and applies the transforms in the reverse order. The same code also decodes the "headerless"
 * stream of the alpha channel of a lossy image.
 * <p>
 * WebP 무손실 비트스트림(VP8L)의 디코더(계획 P18-03; "WebP Lossless Bitstream Specification"). 스트림은 선택적인 변환 사슬(예측, 색 교차, 녹색
 * 빼기, 색 인덱싱)과 그 뒤의 엔트로피 부호화된 ARGB 픽셀 영상으로 이루어지며 영상은 접두 부호, 2차원 거리표를 쓰는 LZ77 역참조, 선택적 색 캐시를
 * 쓴다. 영상 데이터는 "메타" 영상이 블록마다 고르는 여러 접두 부호 집합을 쓸 수 있으며 변환 데이터와 메타 영상도 엔트로피 부호화된 영상이다.
 * 디코더는 변환을 읽고 픽셀을 디코딩한 뒤 변환을 역순으로 적용한다. 같은 코드가 손실 영상의 알파 채널용 "헤더 없는" 스트림도 디코딩한다.
 */
final class Vp8lDecoder {

    /**
     * Signature byte at the start of a VP8L chunk.
     * <p>
     * VP8L 청크 시작의 시그니처 바이트.
     */
    private static final int SIGNATURE = 0x2f;

    /**
     * Number of literal symbols of a colour channel and the first length code.
     * <p>
     * 색 채널의 리터럴 기호 수이자 첫 길이 부호.
     */
    private static final int LITERALS = 256;

    /**
     * Number of length prefix codes.
     * <p>
     * 길이 접두 부호의 수.
     */
    private static final int LENGTH_CODES = 24;

    /**
     * Number of distance prefix codes.
     * <p>
     * 거리 접두 부호의 수.
     */
    private static final int DISTANCE_CODES = 40;

    /**
     * The largest number of colour cache bits.
     * <p>
     * 색 캐시 비트 수의 최대값.
     */
    private static final int MAX_CACHE_BITS = 11;

    /**
     * Upper bound of the memory (in ints) that all prefix code tables of an image may use: 8 Mi ints, 32 MiB.
     * <p>
     * 한 영상의 모든 접두 부호 표가 쓸 수 있는 메모리(int 단위)의 상한: 8 Mi int, 32 MiB.
     */
    private static final int MAX_TABLE_INTS = 8 * 1024 * 1024;

    /**
     * The order in which the code lengths of the code length code are stored (LZ77-style "code length code order").
     * <p>
     * 부호 길이 부호의 길이가 저장되는 순서.
     */
    private static final int[] CODE_LENGTH_ORDER = { 17, 18, 0, 1, 2, 3, 4, 5, 16, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };

    /**
     * Extra bits of the code length symbols 16, 17 and 18.
     * <p>
     * 부호 길이 기호 16, 17, 18 의 추가 비트 수.
     */
    private static final int[] REPEAT_EXTRA_BITS = { 2, 3, 7 };

    /**
     * Base repeat counts of the code length symbols 16, 17 and 18.
     * <p>
     * 부호 길이 기호 16, 17, 18 의 기본 반복 횟수.
     */
    private static final int[] REPEAT_BASE = { 3, 3, 11 };

    /**
     * Maps the distance codes 1..120 to a pixel offset: the high nibble is the number of rows up, 8 minus the low nibble is the column
     * offset.
     * <p>
     * 거리 부호 1..120 을 픽셀 오프셋으로 바꾼다. 위 4비트는 위로 올라가는 행 수, 8 에서 아래 4비트를 뺀 값은 열 오프셋이다.
     */
    private static final int[] DISTANCE_MAP = { 0x18, 0x07, 0x17, 0x19, 0x28, 0x06, 0x27, 0x29, 0x16, 0x1a, 0x26, 0x2a, 0x38, 0x05, 0x37,
        0x39, 0x15, 0x1b, 0x36, 0x3a, 0x25, 0x2b, 0x48, 0x04, 0x47, 0x49, 0x14, 0x1c, 0x35, 0x3b, 0x46, 0x4a, 0x24, 0x2c, 0x58, 0x45,
        0x4b, 0x34, 0x3c, 0x03, 0x57, 0x59, 0x13, 0x1d, 0x56, 0x5a, 0x23, 0x2d, 0x44, 0x4c, 0x55, 0x5b, 0x33, 0x3d, 0x68, 0x02, 0x67,
        0x69, 0x12, 0x1e, 0x66, 0x6a, 0x22, 0x2e, 0x54, 0x5c, 0x43, 0x4d, 0x65, 0x6b, 0x32, 0x3e, 0x78, 0x01, 0x77, 0x79, 0x53, 0x5d,
        0x11, 0x1f, 0x64, 0x6c, 0x42, 0x4e, 0x76, 0x7a, 0x21, 0x2f, 0x75, 0x7b, 0x31, 0x3f, 0x63, 0x6d, 0x52, 0x5e, 0x00, 0x74, 0x7c,
        0x41, 0x4f, 0x10, 0x20, 0x62, 0x6e, 0x30, 0x73, 0x7d, 0x51, 0x5f, 0x40, 0x72, 0x7e, 0x61, 0x6f, 0x50, 0x71, 0x7f, 0x60, 0x70 };

    /**
     * The bit reader.
     * <p>
     * 비트 리더.
     */
    private final Vp8lBits br;

    /**
     * The transforms read from the stream, in stream order.
     * <p>
     * 스트림에서 읽은 변환(스트림 순서).
     */
    private final List<Transform> transforms = new ArrayList<Transform>();

    /**
     * Width of the pixel image after the transforms that shrink it (colour indexing with pixel bundling).
     * <p>
     * 이미지를 줄이는 변환(픽셀 묶음이 있는 색 인덱싱)을 거친 뒤의 가로.
     */
    private int codedWidth;

    /**
     * Remaining budget of prefix code table memory.
     * <p>
     * 접두 부호 표 메모리의 남은 한도.
     */
    private int tableBudget = MAX_TABLE_INTS;

    /**
     * Creates a decoder; use the static methods.
     * <p>
     * 디코더를 만든다. 정적 메서드를 쓴다.
     *
     * @param data   data / 데이터
     * @param offset start of the bitstream / 비트스트림 시작
     * @param length length of the bitstream / 비트스트림 길이
     */
    private Vp8lDecoder(byte[] data, int offset, int length) {
        this.br = new Vp8lBits(data, offset, length);
    }

    /**
     * Reads the size of the image from the header of a VP8L chunk without decoding it.
     * <p>
     * VP8L 청크의 헤더에서 디코딩 없이 이미지 크기를 읽는다.
     *
     * @param data data / 데이터
     * @param off  start of the chunk data / 청크 데이터 시작
     * @param len  length of the chunk data / 청크 데이터 길이
     * @return {width, height, alpha used (0 or 1)} / {가로, 세로, 알파 사용 여부(0 또는 1)}
     * @throws MediaException CORRUPT if the header is not valid / 헤더가 올바르지 않으면 CORRUPT
     */
    static int[] readSize(byte[] data, int off, int len) throws MediaException {
        if (len < 5) {
            throw new MediaException(MediaException.Kind.CORRUPT, "VP8L header is too short");
        }
        if ((data[off] & 0xff) != SIGNATURE) {
            throw new MediaException(MediaException.Kind.CORRUPT, "VP8L signature is missing");
        }
        int bits = (data[off + 1] & 0xff) | ((data[off + 2] & 0xff) << 8) | ((data[off + 3] & 0xff) << 16) | ((data[off + 4] & 0xff) << 24);
        int w = (bits & 0x3fff) + 1;
        int h = ((bits >>> 14) & 0x3fff) + 1;
        int alpha = (bits >>> 28) & 1;
        int version = bits >>> 29;
        if (version != 0) {
            throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "unknown VP8L version " + version);
        }
        return new int[] { w, h, alpha };
    }

    /**
     * Decodes a VP8L chunk.
     * <p>
     * VP8L 청크를 디코딩한다.
     *
     * @param data   data / 데이터
     * @param off    start of the chunk data / 청크 데이터 시작
     * @param len    length of the chunk data / 청크 데이터 길이
     * @param limits decoding limits / 디코딩 제한
     * @return the image / 이미지
     * @throws MediaException CORRUPT for damaged data, TOO_LARGE over a limit, CANCELLED if interrupted / 손상은 CORRUPT, 제한 초과는 TOO_LARGE, 인터럽트는 CANCELLED
     */
    static WebpRaster decode(byte[] data, int off, int len, DecodeLimits limits) throws MediaException {
        int[] size = readSize(data, off, len);
        limits.checkImageSize(size[0], size[1]);
        Vp8lDecoder d = new Vp8lDecoder(data, off + 5, len - 5);
        int[] argb = d.run(size[0], size[1]);
        return new WebpRaster(size[0], size[1], argb, size[2] != 0);
    }

    /**
     * Decodes the headerless lossless stream of an alpha channel (the image size is known from the lossy frame).
     * <p>
     * 알파 채널의 헤더 없는 무손실 스트림을 디코딩한다(이미지 크기는 손실 프레임에서 안다).
     *
     * @param data   data / 데이터
     * @param off    start of the stream / 스트림 시작
     * @param len    length of the stream / 스트림 길이
     * @param width  width / 가로
     * @param height height / 세로
     * @return the pixels; the alpha values are in the green channel / 픽셀. 알파 값은 녹색 채널에 있다
     * @throws MediaException CORRUPT for damaged data, CANCELLED if interrupted / 손상은 CORRUPT, 인터럽트는 CANCELLED
     */
    static int[] decodeHeaderless(byte[] data, int off, int len, int width, int height) throws MediaException {
        return new Vp8lDecoder(data, off, len).run(width, height);
    }

    /**
     * Decodes the main image and converts the failures to media exceptions.
     * <p>
     * 주 영상을 디코딩하고 실패를 미디어 예외로 바꾼다.
     *
     * @param width  width / 가로
     * @param height height / 세로
     * @return the pixels / 픽셀
     * @throws MediaException on failure / 실패 시
     */
    private int[] run(int width, int height) throws MediaException {
        try {
            int[] px = decodeImageStream(width, height, true);
            // 변환은 읽은 순서의 반대로 되돌린다.
            for (int i = transforms.size() - 1; i >= 0; i--) {
                px = transforms.get(i).inverse(px, height);
                checkInterrupt();
            }
            return px;
        } catch (Vp8lBits.LosslessException e) {
            throw new MediaException(MediaException.Kind.CORRUPT, e.getMessage(), e);
        }
    }

    /**
     * Fails with CANCELLED if the current thread was interrupted.
     * <p>
     * 현재 스레드가 인터럽트되었으면 CANCELLED 로 실패한다.
     *
     * @throws MediaException CANCELLED if interrupted / 인터럽트되었으면 CANCELLED
     */
    private static void checkInterrupt() throws MediaException {
        if (Thread.currentThread().isInterrupted()) {
            throw new MediaException(MediaException.Kind.CANCELLED, "decoding was cancelled");
        }
    }

    /**
     * Returns the number of blocks needed to cover a length (division rounded up).
     * <p>
     * 길이를 덮는 데 필요한 블록 수(올림 나눗셈)를 반환한다.
     *
     * @param size length / 길이
     * @param bits block size as a power of two / 블록 크기의 2 의 거듭제곱 지수
     * @return number of blocks / 블록 수
     */
    private static int subSample(int size, int bits) {
        return (size + (1 << bits) - 1) >> bits;
    }

    /**
     * Decodes an entropy-coded image; the main image (level 0) first reads the transforms and may use several prefix code groups.
     * <p>
     * 엔트로피 부호화된 영상을 디코딩한다. 주 영상(레벨 0)은 먼저 변환을 읽고 접두 부호 집합을 여럿 쓸 수 있다.
     *
     * @param xsize  width / 가로
     * @param ysize  height / 세로
     * @param level0 true for the main image / 주 영상이면 true
     * @return the pixels (in the coded domain: after the transforms) / 픽셀(부호화 영역, 곧 변환을 거친 뒤)
     * @throws MediaException on cancellation / 취소 시
     */
    private int[] decodeImageStream(int xsize, int ysize, boolean level0) throws MediaException {
        int w = xsize;
        if (level0) {
            codedWidth = xsize;
            // 변환이 있는 동안 하나씩 읽는다(각 종류는 한 번만 올 수 있다).
            while (br.read(1) == 1) {
                readTransform(ysize);
            }
            w = codedWidth;
        }
        int cacheBits = 0;
        if (br.read(1) == 1) {
            cacheBits = br.read(4);
            if (cacheBits < 1 || cacheBits > MAX_CACHE_BITS) {
                throw new Vp8lBits.LosslessException("invalid colour cache size");
            }
        }
        return decodePixels(w, ysize, cacheBits, level0);
    }

    /**
     * Reads one transform from the stream and remembers it.
     * <p>
     * 스트림에서 변환 하나를 읽어 기억한다.
     *
     * @param ysize height of the image / 이미지 세로
     * @throws MediaException on cancellation / 취소 시
     */
    private void readTransform(int ysize) throws MediaException {
        int type = br.read(2);
        // 같은 종류의 변환이 두 번 나오면 잘못된 스트림이다.
        for (int i = 0; i < transforms.size(); i++) {
            if (transforms.get(i).type == type) {
                throw new Vp8lBits.LosslessException("a transform appears twice");
            }
        }
        Transform t = new Transform(type, codedWidth);
        if (type == Transform.PREDICTOR || type == Transform.CROSS_COLOR) {
            t.bits = br.read(3) + 2;
            t.data = decodeImageStream(subSample(codedWidth, t.bits), subSample(ysize, t.bits), false);
        } else if (type == Transform.COLOR_INDEXING) {
            int colors = br.read(8) + 1;
            t.bits = colors > 16 ? 0 : (colors > 4 ? 1 : (colors > 2 ? 2 : 3));
            int[] table = decodeImageStream(colors, 1, false);
            int[] palette = new int[1 << (8 >> t.bits)];
            palette[0] = table[0];
            // 팔레트는 이전 항목과의 성분별 차이로 저장되어 있으므로 누적해 되돌린다.
            for (int i = 1; i < colors; i++) {
                palette[i] = addPixels(table[i], palette[i - 1]);
            }
            t.data = palette;
            codedWidth = subSample(codedWidth, t.bits);
        }
        transforms.add(t);
    }

    /**
     * Reads the prefix codes and decodes the pixels of an image.
     * <p>
     * 접두 부호를 읽고 영상의 픽셀을 디코딩한다.
     *
     * @param width     width of the coded image / 부호화된 이미지의 가로
     * @param height    height of the coded image / 부호화된 이미지의 세로
     * @param cacheBits colour cache bits, 0 for none / 색 캐시 비트 수. 없으면 0
     * @param level0    true for the main image, which may have a meta image / 메타 영상이 있을 수 있는 주 영상이면 true
     * @return the pixels / 픽셀
     * @throws MediaException on cancellation / 취소 시
     */
    private int[] decodePixels(int width, int height, int cacheBits, boolean level0) throws MediaException {
        int metaBits = 0;
        int metaWidth = 0;
        int[] meta = null;
        int groupCount = 1;
        if (level0 && br.read(1) == 1) {
            metaBits = br.read(3) + 2;
            metaWidth = subSample(width, metaBits);
            int[] img = decodeImageStream(metaWidth, subSample(height, metaBits), false);
            meta = new int[img.length];
            // 메타 영상에서 접두 부호 집합 번호(빨강과 녹색 채널)를 꺼내고 최댓값을 찾는다.
            for (int i = 0; i < img.length; i++) {
                meta[i] = (img[i] >> 8) & 0xffff;
                if (meta[i] >= groupCount) {
                    groupCount = meta[i] + 1;
                }
            }
        }
        boolean[] used = new boolean[groupCount];
        if (meta == null) {
            used[0] = true;
        } else {
            // 실제로 쓰이는 집합만 표를 보관한다.
            for (int i = 0; i < meta.length; i++) {
                used[meta[i]] = true;
            }
        }
        Vp8lHuffman[][] groups = new Vp8lHuffman[groupCount][];
        // 집합마다 다섯 개의 접두 부호(녹색+길이+캐시, 빨강, 파랑, 알파, 거리)를 읽는다.
        for (int g = 0; g < groupCount; g++) {
            Vp8lHuffman[] set = new Vp8lHuffman[5];
            // 다섯 부호의 알파벳 크기를 정해 하나씩 읽는다.
            for (int j = 0; j < 5; j++) {
                int alphabet = j == 0 ? LITERALS + LENGTH_CODES + (cacheBits > 0 ? 1 << cacheBits : 0) : (j == 4 ? DISTANCE_CODES : LITERALS);
                set[j] = readPrefixCode(alphabet, used[g]);
            }
            groups[g] = used[g] ? set : null;
        }
        return readPixels(width, height, cacheBits, groups, meta, metaBits, metaWidth);
    }

    /**
     * Reads one prefix code (simple or normal form) from the stream.
     * <p>
     * 스트림에서 접두 부호 하나(단순형 또는 일반형)를 읽는다.
     *
     * @param alphabet number of symbols / 기호 수
     * @param keep     whether the tables are needed / 표가 필요한지 여부
     * @return the code / 부호
     */
    private Vp8lHuffman readPrefixCode(int alphabet, boolean keep) {
        // 단순형의 기호 값은 8비트이므로 알파벳이 작아도 배열은 256 이상으로 잡는다(알파벳 밖의 기호는 무시된다).
        int[] lengths = new int[Math.max(alphabet, LITERALS)];
        if (br.read(1) == 1) {
            // 단순형: 기호가 한두 개이고 길이는 모두 1 로 본다.
            int symbols = br.read(1) + 1;
            int firstIs8 = br.read(1);
            int s = br.read(firstIs8 == 0 ? 1 : 8);
            lengths[s] = 1;
            if (symbols == 2) {
                lengths[br.read(8)] = 1;
            }
        } else {
            readCodeLengths(lengths, alphabet);
        }
        Vp8lHuffman h = Vp8lHuffman.build(lengths, alphabet, keep);
        if (h == null) {
            throw new Vp8lBits.LosslessException("invalid prefix code lengths");
        }
        if (keep) {
            tableBudget -= Vp8lHuffman.cost(alphabet);
            if (tableBudget < 0) {
                throw new Vp8lBits.LosslessException("the prefix codes need too much memory");
            }
        }
        return h;
    }

    /**
     * Reads the code lengths of a normal prefix code with the code length code.
     * <p>
     * 부호 길이 부호를 써서 일반형 접두 부호의 부호 길이를 읽는다.
     *
     * @param lengths receives the code lengths / 부호 길이를 받는다
     * @param alphabet number of symbols / 기호 수
     */
    private void readCodeLengths(int[] lengths, int alphabet) {
        int[] clLengths = new int[CODE_LENGTH_ORDER.length];
        int num = br.read(4) + 4;
        // 부호 길이 부호의 길이를 정해진 순서대로 읽는다(3비트씩).
        for (int i = 0; i < num; i++) {
            clLengths[CODE_LENGTH_ORDER[i]] = br.read(3);
        }
        Vp8lHuffman cl = Vp8lHuffman.build(clLengths, clLengths.length, true);
        if (cl == null) {
            throw new Vp8lBits.LosslessException("invalid code length code");
        }
        int maxSymbol = alphabet;
        if (br.read(1) == 1) {
            // 읽을 기호 수가 따로 지정된 경우이다.
            int lengthBits = 2 + 2 * br.read(3);
            maxSymbol = 2 + br.read(lengthBits);
            if (maxSymbol > alphabet) {
                throw new Vp8lBits.LosslessException("too many code length symbols");
            }
        }
        int symbol = 0;
        int prev = 8;
        // 기호마다 길이를 읽는다. 16, 17, 18 은 이전 길이 또는 0 의 반복이다.
        while (symbol < alphabet && maxSymbol-- > 0) {
            int code = cl.read(br);
            if (code < 16) {
                lengths[symbol++] = code;
                if (code != 0) {
                    prev = code;
                }
            } else {
                int slot = code - 16;
                int repeat = br.read(REPEAT_EXTRA_BITS[slot]) + REPEAT_BASE[slot];
                if (symbol + repeat > alphabet) {
                    throw new Vp8lBits.LosslessException("code length repeat is too long");
                }
                int value = code == 16 ? prev : 0;
                // 같은 길이를 반복 횟수만큼 채운다.
                for (int i = 0; i < repeat; i++) {
                    lengths[symbol++] = value;
                }
            }
        }
    }

    /**
     * Reads the pixels with the prefix codes, backward references and the colour cache.
     * <p>
     * 접두 부호, 역참조, 색 캐시로 픽셀을 읽는다.
     *
     * @param width     width / 가로
     * @param height    height / 세로
     * @param cacheBits colour cache bits / 색 캐시 비트 수
     * @param groups    prefix code sets (null entries for unused sets) / 접두 부호 집합(쓰지 않는 것은 null)
     * @param meta      set number per block, or null for one set / 블록별 집합 번호. 하나뿐이면 null
     * @param metaBits  block size of the meta image as a power of two / 메타 영상 블록 크기의 지수
     * @param metaWidth width of the meta image / 메타 영상의 가로
     * @return the pixels / 픽셀
     * @throws MediaException on cancellation / 취소 시
     */
    private int[] readPixels(int width, int height, int cacheBits, Vp8lHuffman[][] groups, int[] meta, int metaBits, int metaWidth)
            throws MediaException {
        int total = width * height;
        int[] px = new int[total];
        int[] cache = cacheBits > 0 ? new int[1 << cacheBits] : null;
        int mask = meta == null ? -1 : (1 << metaBits) - 1;
        Vp8lHuffman[] cur = groups[0];
        int src = 0;
        int x = 0;
        int y = 0;
        int cached = 0;
        int ticks = 0;
        // 모든 픽셀이 채워질 때까지 기호를 읽는다.
        while (src < total) {
            if ((x & mask) == 0 && meta != null) {
                cur = groups[meta[(y >> metaBits) * metaWidth + (x >> metaBits)]];
            }
            // 일정한 횟수마다 취소되었는지 확인한다.
            if ((++ticks & 0x3fff) == 0) {
                checkInterrupt();
            }
            int s = cur[0].read(br);
            if (s < LITERALS) {
                // 리터럴 픽셀: 녹색은 이미 읽었고 빨강, 파랑, 알파 순으로 읽는다.
                int red = cur[1].read(br);
                int blue = cur[2].read(br);
                int alpha = cur[3].read(br);
                px[src++] = (alpha << 24) | (red << 16) | (s << 8) | blue;
                x++;
                if (x >= width) {
                    x = 0;
                    y++;
                }
            } else if (s < LITERALS + LENGTH_CODES) {
                // 역참조: 길이와 거리를 읽어 이전 픽셀을 복사한다(겹쳐도 되므로 한 픽셀씩).
                int length = lz77Value(s - LITERALS);
                int distance = distanceOf(lz77Value(cur[4].read(br)), width);
                if (src < distance || total - src < length) {
                    throw new Vp8lBits.LosslessException("invalid backward reference");
                }
                // 복사 구간이 겹칠 수 있으므로 한 픽셀씩 이전 픽셀을 복사한다.
                for (int i = 0; i < length; i++) {
                    px[src] = px[src - distance];
                    src++;
                }
                x += length;
                // 복사로 여러 행을 넘어갔을 수 있다.
                while (x >= width) {
                    x -= width;
                    y++;
                }
                if (src < total && (x & mask) != 0 && meta != null) {
                    cur = groups[meta[(y >> metaBits) * metaWidth + (x >> metaBits)]];
                }
            } else {
                // 색 캐시 참조: 지금까지의 픽셀을 캐시에 반영한 뒤 읽는다.
                if (cache == null) {
                    throw new Vp8lBits.LosslessException("colour cache symbol without a colour cache");
                }
                // 아직 캐시에 넣지 않은 픽셀을 모두 캐시에 반영한다.
                while (cached < src) {
                    int c = px[cached++];
                    cache[(c * 0x1e35a7bd) >>> (32 - cacheBits)] = c;
                }
                px[src++] = cache[s - LITERALS - LENGTH_CODES];
                x++;
                if (x >= width) {
                    x = 0;
                    y++;
                }
            }
        }
        return px;
    }

    /**
     * Decodes a length or distance prefix code into its value.
     * <p>
     * 길이 또는 거리 접두 부호를 값으로 바꾼다.
     *
     * @param prefix the prefix symbol / 접두 기호
     * @return the value (at least 1) / 값(1 이상)
     */
    private int lz77Value(int prefix) {
        if (prefix < 4) {
            return prefix + 1;
        }
        int extra = (prefix - 2) >> 1;
        int offset = (2 + (prefix & 1)) << extra;
        return offset + br.read(extra) + 1;
    }

    /**
     * Converts a distance code to a distance in pixels with the 2-D neighbourhood map.
     * <p>
     * 2차원 이웃 표로 거리 부호를 픽셀 거리로 바꾼다.
     *
     * @param code  distance code, at least 1 / 거리 부호(1 이상)
     * @param width width of the image / 이미지 가로
     * @return distance in pixels, at least 1 / 픽셀 거리(1 이상)
     */
    private static int distanceOf(int code, int width) {
        if (code > DISTANCE_MAP.length) {
            return code - DISTANCE_MAP.length;
        }
        int v = DISTANCE_MAP[code - 1];
        int d = (v >> 4) * width + (8 - (v & 0xf));
        return d >= 1 ? d : 1;
    }

    /**
     * Adds two ARGB pixels channel by channel modulo 256.
     * <p>
     * ARGB 픽셀 둘을 채널별로 256 로 나눈 나머지로 더한다.
     *
     * @param a first pixel / 첫 픽셀
     * @param b second pixel / 둘째 픽셀
     * @return the sum / 합
     */
    static int addPixels(int a, int b) {
        int ag = (a & 0xff00ff00) + (b & 0xff00ff00);
        int rb = (a & 0x00ff00ff) + (b & 0x00ff00ff);
        return (ag & 0xff00ff00) | (rb & 0x00ff00ff);
    }

    /**
     * Averages two ARGB pixels channel by channel (rounded down).
     * <p>
     * ARGB 픽셀 둘의 채널별 평균(내림)을 구한다.
     *
     * @param a first pixel / 첫 픽셀
     * @param b second pixel / 둘째 픽셀
     * @return the average / 평균
     */
    private static int average2(int a, int b) {
        return (((a ^ b) & 0xfefefefe) >>> 1) + (a & b);
    }

    /**
     * Clamps a value to 0..255.
     * <p>
     * 값을 0..255 로 자른다.
     *
     * @param v value / 값
     * @return clamped value / 자른 값
     */
    private static int clamp(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    /**
     * Returns the absolute difference of two bytes of a pixel summed over the four channels.
     * <p>
     * 픽셀 둘의 네 채널에 걸친 차이의 절댓값 합을 구한다.
     *
     * @param a first pixel / 첫 픽셀
     * @param b second pixel / 둘째 픽셀
     * @return the sum of absolute differences / 절대 차이의 합
     */
    private static int manhattan(int a, int b) {
        return Math.abs((a >>> 24) - (b >>> 24)) + Math.abs(((a >> 16) & 0xff) - ((b >> 16) & 0xff)) + Math.abs(((a >> 8) & 0xff) - ((b >> 8) & 0xff))
                + Math.abs((a & 0xff) - (b & 0xff));
    }

    /**
     * Computes the prediction of the predictor transform.
     * <p>
     * 예측 변환의 예측값을 계산한다.
     *
     * @param mode predictor mode 0..13 (14 and 15 act like 0) / 예측 모드 0..13(14, 15 는 0 과 같다)
     * @param l    left pixel / 왼쪽 픽셀
     * @param t    top pixel / 위 픽셀
     * @param tr   top-right pixel / 오른쪽 위 픽셀
     * @param tl   top-left pixel / 왼쪽 위 픽셀
     * @return the predicted pixel / 예측 픽셀
     */
    private static int predict(int mode, int l, int t, int tr, int tl) {
        switch (mode) {
        case 1:
            return l;
        case 2:
            return t;
        case 3:
            return tr;
        case 4:
            return tl;
        case 5:
            return average2(average2(l, tr), t);
        case 6:
            return average2(l, tl);
        case 7:
            return average2(l, t);
        case 8:
            return average2(tl, t);
        case 9:
            return average2(t, tr);
        case 10:
            return average2(average2(l, tl), average2(t, tr));
        case 11:
            // 선택 예측: 왼쪽과 위 중 기울기 예측에 더 가까운 쪽을 고른다.
            return manhattan(t, tl) < manhattan(l, tl) ? l : t;
        case 12:
            return clampAddSubtractFull(l, t, tl);
        case 13:
            return clampAddSubtractHalf(average2(l, t), tl);
        default:
            return 0xff000000;
        }
    }

    /**
     * Predicts <code>a + b - c</code> per channel, clamped.
     * <p>
     * 채널별로 <code>a + b - c</code> 를 예측하고 자른다.
     *
     * @param a first pixel / 첫 픽셀
     * @param b second pixel / 둘째 픽셀
     * @param c third pixel / 셋째 픽셀
     * @return the prediction / 예측값
     */
    private static int clampAddSubtractFull(int a, int b, int c) {
        int out = 0;
        // 네 채널을 하나씩 계산한다.
        for (int shift = 0; shift < 32; shift += 8) {
            int v = clamp(((a >>> shift) & 0xff) + ((b >>> shift) & 0xff) - ((c >>> shift) & 0xff));
            out |= v << shift;
        }
        return out;
    }

    /**
     * Predicts <code>a + (a - b) / 2</code> per channel, clamped (the division truncates toward zero).
     * <p>
     * 채널별로 <code>a + (a - b) / 2</code> 를 예측하고 자른다(나눗셈은 0 방향으로 버린다).
     *
     * @param a first pixel / 첫 픽셀
     * @param b second pixel / 둘째 픽셀
     * @return the prediction / 예측값
     */
    private static int clampAddSubtractHalf(int a, int b) {
        int out = 0;
        // 네 채널을 하나씩 계산한다.
        for (int shift = 0; shift < 32; shift += 8) {
            int ca = (a >>> shift) & 0xff;
            int cb = (b >>> shift) & 0xff;
            out |= clamp(ca + (ca - cb) / 2) << shift;
        }
        return out;
    }

    /**
     * One transform of the lossless format together with the code that undoes it.
     * <p>
     * 무손실 형식의 변환 하나와 그것을 되돌리는 코드.
     */
    private static final class Transform {

        /**
         * Type of the predictor transform.
         * <p>
         * 예측 변환의 종류.
         */
        static final int PREDICTOR = 0;

        /**
         * Type of the cross-colour transform.
         * <p>
         * 색 교차 변환의 종류.
         */
        static final int CROSS_COLOR = 1;

        /**
         * Type of the subtract-green transform.
         * <p>
         * 녹색 빼기 변환의 종류.
         */
        static final int SUBTRACT_GREEN = 2;

        /**
         * Type of the colour indexing transform.
         * <p>
         * 색 인덱싱 변환의 종류.
         */
        static final int COLOR_INDEXING = 3;

        /**
         * Type of this transform.
         * <p>
         * 이 변환의 종류.
         */
        final int type;

        /**
         * Width of the image when the transform was read (before this transform shrinks it).
         * <p>
         * 변환을 읽은 시점의 이미지 가로(이 변환이 줄이기 전).
         */
        final int width;

        /**
         * Block size (predictor, cross-colour) as a power of two, or pixel bundling (colour indexing) as a power of two.
         * <p>
         * 블록 크기(예측, 색 교차)의 지수, 또는 픽셀 묶음(색 인덱싱)의 지수.
         */
        int bits;

        /**
         * Transform data: the sub-image of modes or multipliers, or the palette.
         * <p>
         * 변환 데이터: 모드나 곱셈 계수의 하위 영상, 또는 팔레트.
         */
        int[] data;

        /**
         * Creates a transform.
         * <p>
         * 변환을 만든다.
         *
         * @param type  transform type / 변환 종류
         * @param width image width at this point / 이 시점의 이미지 가로
         */
        Transform(int type, int width) {
            this.type = type;
            this.width = width;
        }

        /**
         * Undoes the transform.
         * <p>
         * 변환을 되돌린다.
         *
         * @param px     pixels in the domain after the transform / 변환 뒤 영역의 픽셀
         * @param height image height / 이미지 세로
         * @return pixels in the domain before the transform (may be the same array) / 변환 전 영역의 픽셀(같은 배열일 수 있다)
         */
        int[] inverse(int[] px, int height) {
            switch (type) {
            case PREDICTOR:
                inversePredictor(px, height);
                return px;
            case CROSS_COLOR:
                inverseCrossColor(px, height);
                return px;
            case SUBTRACT_GREEN:
                // 녹색을 빨강과 파랑에 다시 더한다.
                for (int i = 0; i < px.length; i++) {
                    int p = px[i];
                    int g = (p >> 8) & 0xff;
                    int rb = ((p & 0x00ff00ff) + ((g << 16) | g)) & 0x00ff00ff;
                    px[i] = (p & 0xff00ff00) | rb;
                }
                return px;
            default:
                return inverseColorIndexing(px, height);
            }
        }

        /**
         * Undoes the predictor transform in place.
         * <p>
         * 예측 변환을 제자리에서 되돌린다.
         *
         * @param px     pixels / 픽셀
         * @param height image height / 이미지 세로
         */
        private void inversePredictor(int[] px, int height) {
            int w = width;
            int blockW = subSample(w, bits);
            px[0] = addPixels(px[0], 0xff000000);
            // 첫 행은 모두 왼쪽 픽셀로 예측한다.
            for (int x = 1; x < w; x++) {
                px[x] = addPixels(px[x], px[x - 1]);
            }
            // 둘째 행부터는 첫 픽셀을 위 픽셀로, 나머지는 블록의 모드로 예측한다.
            for (int y = 1; y < height; y++) {
                int row = y * w;
                px[row] = addPixels(px[row], px[row - w]);
                int x = 1;
                // 블록 경계마다 모드를 다시 읽으며 행을 채운다.
                while (x < w) {
                    int mode = (data[(y >> bits) * blockW + (x >> bits)] >> 8) & 0xf;
                    int xEnd = Math.min(w, ((x >> bits) + 1) << bits);
                    // 같은 블록 안의 픽셀은 같은 모드로 예측한다.
                    for (; x < xEnd; x++) {
                        int i = row + x;
                        px[i] = addPixels(px[i], predict(mode, px[i - 1], px[i - w], px[i - w + 1], px[i - w - 1]));
                    }
                }
            }
        }

        /**
         * Undoes the cross-colour transform in place.
         * <p>
         * 색 교차 변환을 제자리에서 되돌린다.
         *
         * @param px     pixels / 픽셀
         * @param height image height / 이미지 세로
         */
        private void inverseCrossColor(int[] px, int height) {
            int w = width;
            int blockW = subSample(w, bits);
            // 행마다 블록의 곱셈 계수를 읽어 빨강과 파랑을 되돌린다.
            for (int y = 0; y < height; y++) {
                int row = y * w;
                // 한 행의 픽셀을 처리한다.
                for (int x = 0; x < w; x++) {
                    int m = data[(y >> bits) * blockW + (x >> bits)];
                    int greenToRed = (byte) m;
                    int greenToBlue = (byte) (m >> 8);
                    int redToBlue = (byte) (m >> 16);
                    int p = px[row + x];
                    int green = (byte) (p >> 8);
                    int red = (p >> 16) & 0xff;
                    int blue = p & 0xff;
                    red = (red + ((greenToRed * green) >> 5)) & 0xff;
                    blue = (blue + ((greenToBlue * green) >> 5) + ((redToBlue * (byte) red) >> 5)) & 0xff;
                    px[row + x] = (p & 0xff00ff00) | (red << 16) | blue;
                }
            }
        }

        /**
         * Undoes the colour indexing transform, expanding bundled pixels.
         * <p>
         * 색 인덱싱 변환을 되돌리며 묶인 픽셀을 푼다.
         *
         * @param packed pixels whose green channel holds the palette indexes / 녹색 채널에 팔레트 색인이 있는 픽셀
         * @param height image height / 이미지 세로
         * @return the expanded pixels / 푼 픽셀
         */
        private int[] inverseColorIndexing(int[] packed, int height) {
            int w = width;
            int[] out = new int[w * height];
            if (bits == 0) {
                // 묶음이 없으면 한 픽셀이 한 색인이다.
                for (int i = 0; i < out.length; i++) {
                    out[i] = data[(packed[i] >> 8) & 0xff];
                }
                return out;
            }
            int perByte = 1 << bits;
            int bitsPerPixel = 8 >> bits;
            int mask = (1 << bitsPerPixel) - 1;
            int packedW = subSample(w, bits);
            // 행마다 묶인 값에서 낮은 비트부터 색인을 꺼낸다.
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < w; x++) {
                    int v = (packed[y * packedW + (x >> bits)] >> 8) & 0xff;
                    out[y * w + x] = data[(v >> (bitsPerPixel * (x & (perByte - 1)))) & mask];
                }
            }
            return out;
        }
    }
}
