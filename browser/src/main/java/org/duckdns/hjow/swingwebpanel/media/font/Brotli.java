package org.duckdns.hjow.swingwebpanel.media.font;

import java.util.Arrays;

/**
 * A decoder for the Brotli compressed data format (RFC 7932, https://www.rfc-editor.org/rfc/rfc7932.txt, July 2016), written in plain Java without a library:
 * it reads the stream header and every meta-block (empty and metadata blocks, uncompressed blocks, and compressed blocks with their block types, context
 * maps, prefix codes, insert-and-copy commands, the ring buffer of distances and the static dictionary with its 121 transformations; the tables come from
 * {@link BrotliData}, which the tool <code>tools/brotlidict</code> made from the RFC). The whole result is built in memory, so the caller gives the largest
 * output that it accepts: a stream that would produce more is rejected, and so is every stream that breaks a rule of the RFC ("the stream should be rejected
 * as invalid"), with a {@link BrotliException}; the decoder never reads or writes outside of its arrays. WOFF 2.0 files (see {@link Woff2}) are the user.
 * <p>
 * Brotli 압축 데이터 형식(RFC 7932, https://www.rfc-editor.org/rfc/rfc7932.txt, 2016 년 7 월)의 디코더이며 라이브러리 없이 순수 Java 로 썼다: 스트림 머리와 모든 메타 블록(빈 블록과
 * 메타데이터 블록, 압축하지 않은 블록, 블록 종류와 문맥 지도와 접두사 코드와 삽입·복사 명령과 거리 링 버퍼와 121 개의 변환이 있는 정적 사전을 가진 압축 블록)을 읽는다. 표는
 * <code>tools/brotlidict</code> 도구가 RFC 에서 만든 {@link BrotliData} 에서 온다. 결과는 통째로 메모리에 만들므로 호출자가 받아들이는 최대 출력을 준다. 그보다 많이 만드는 스트림과
 * RFC 규칙을 어기는 모든 스트림(RFC: "스트림을 잘못된 것으로 거부")은 {@link BrotliException} 으로 거부한다. 디코더는 자기 배열 밖을 읽거나 쓰지 않는다. WOFF 2.0 파일({@link Woff2})이 쓴다.
 */
public final class Brotli {

    /** Where to find the insert length code ranges per cell of the insert-and-copy alphabet (cell = symbol / 64): the first insert length code. 삽입 복사 알파벳의 칸(칸 = 기호 / 64)별 첫 삽입 길이 코드. */
    private static final int[] CELL_INSERT = { 0, 0, 0, 0, 8, 8, 0, 16, 8, 16, 16 };

    /** The first copy length code per cell of the insert-and-copy alphabet. 삽입 복사 알파벳의 칸별 첫 복사 길이 코드. */
    private static final int[] CELL_COPY = { 0, 8, 0, 8, 0, 8, 16, 0, 16, 8, 16 };

    /** The smallest insert length of each insert length code. 삽입 길이 코드별 가장 작은 삽입 길이. */
    private static final int[] INSERT_BASE = { 0, 1, 2, 3, 4, 5, 6, 8, 10, 14, 18, 26, 34, 50, 66, 98, 130, 194, 322, 578, 1090, 2114, 6210, 22594 };

    /** The number of extra bits of each insert length code. 삽입 길이 코드별 추가 비트 수. */
    private static final int[] INSERT_EXTRA = { 0, 0, 0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 7, 8, 9, 10, 12, 14, 24 };

    /** The smallest copy length of each copy length code. 복사 길이 코드별 가장 작은 복사 길이. */
    private static final int[] COPY_BASE = { 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 18, 22, 30, 38, 54, 70, 102, 134, 198, 326, 582, 1094, 2118 };

    /** The number of extra bits of each copy length code. 복사 길이 코드별 추가 비트 수. */
    private static final int[] COPY_EXTRA = { 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 7, 8, 9, 10, 24 };

    /** The smallest block count of each block count code. 블록 개수 코드별 가장 작은 블록 개수. */
    private static final int[] BLOCK_BASE = { 1, 5, 9, 13, 17, 25, 33, 41, 49, 65, 81, 97, 113, 145, 177, 209, 241, 305, 369, 497, 753, 1265, 2289, 4337, 8433, 16625 };

    /** The number of extra bits of each block count code. 블록 개수 코드별 추가 비트 수. */
    private static final int[] BLOCK_EXTRA = { 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 7, 8, 9, 10, 11, 12, 13, 24 };

    /** The order in which the code lengths of the code length alphabet are given (section 3.5). 코드 길이 알파벳의 코드 길이를 주는 순서(3.5 절). */
    private static final int[] CODE_LENGTH_ORDER = { 1, 2, 3, 4, 0, 5, 17, 6, 16, 7, 8, 9, 10, 11, 12, 13, 14, 15 };

    /** Which of the last four distances a short distance code refers to (section 4). 짧은 거리 코드가 가리키는 최근 거리 넷 중 하나(4 절). */
    private static final int[] SHORT_BASE = { 0, 1, 2, 3, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1 };

    /** What a short distance code adds to that distance (section 4). 짧은 거리 코드가 그 거리에 더하는 값(4 절). */
    private static final int[] SHORT_OFFSET = { 0, 0, 0, 0, -1, 1, -2, 2, -3, 3, -1, 1, -2, 2, -3, 3 };

    /** The largest number of bytes that a transformation adds to a word is 13; a word has at most 24 bytes. 변환이 단어에 더하는 바이트는 최대 13, 단어는 최대 24 바이트. */
    private static final int MAX_WORD = 24;

    /**
     * A canonical prefix code, ready for decoding: the number of codes of each length and the symbols in the order of their codes (like the tables of the
     * <code>puff</code> decoder of zlib), or a single symbol that takes no bits.
     * <p>
     * 디코딩할 수 있는 정규 접두사 코드: 길이별 코드 수와 코드 순서의 기호(zlib <code>puff</code> 디코더의 표처럼), 또는 비트를 쓰지 않는 기호 하나.
     */
    private static final class Code {
        /** The number of codes of each length 1..15. 길이 1~15 별 코드 수. */
        final int[] count = new int[16];
        /** The symbols ordered by code length and then by value. 코드 길이, 그다음 값 순서의 기호. */
        int[] symbols;
        /** The only symbol of a code with a single symbol, or -1. 기호가 하나뿐인 코드의 그 기호. 아니면 -1. */
        int single = -1;
    }

    /** The compressed data. 압축 데이터. */
    private final byte[] data;
    /** The end of the compressed data (exclusive). 압축 데이터의 끝(제외). */
    private final int end;
    /** The position of the next byte of the compressed data to load into the bit buffer. 비트 버퍼에 적재할 다음 압축 데이터 바이트의 위치. */
    private int pos;
    /** The bit buffer, the next bit is the lowest. 비트 버퍼. 다음 비트가 가장 낮은 비트이다. */
    private long bitBuffer;
    /** The number of valid bits in the bit buffer. 비트 버퍼의 유효 비트 수. */
    private int bitCount;

    /** The output so far. 지금까지의 출력. */
    private byte[] out = new byte[4096];
    /** The number of bytes of the output. 출력의 바이트 수. */
    private int outLen;
    /** The largest output that is accepted. 받아들이는 최대 출력. */
    private final int maxOutput;
    /** The size of the sliding window, the largest distance that is not a dictionary reference. 슬라이딩 윈도 크기. 사전 참조가 아닌 거리의 최대값. */
    private int windowSize;
    /** The last four distances, the last one first (section 4). 최근 거리 넷. 가장 최근이 처음(4 절). */
    private final int[] distances = { 4, 11, 15, 16 };

    /** The static dictionary, read when the first reference needs it. 정적 사전. 첫 참조가 필요할 때 읽는다. */
    private byte[] dictionary;
    /** The byte offsets of the words of each length in the dictionary. 사전에서 길이별 단어의 바이트 오프셋. */
    private int[] dictionaryOffset;
    /** The prefixes, the elementary transform numbers and the suffixes of the 121 transformations. 121 개 변환의 접두사, 기본 변환 번호, 접미사. */
    private byte[][] prefix;
    /** The elementary transform numbers of the transformations. 변환의 기본 변환 번호. */
    private int[] kind;
    /** The suffixes of the transformations. 변환의 접미사. */
    private byte[][] suffix;
    /** The context lookup tables Lut0, Lut1 and Lut2. 문맥 조회표 Lut0, Lut1, Lut2. */
    private byte[] lut;

    /**
     * Creates a decoder for a range of bytes.
     * <p>
     * 바이트 범위를 위한 디코더를 만든다.
     *
     * @param data      the bytes / 바이트
     * @param offset    the first byte of the compressed stream / 압축 스트림의 첫 바이트
     * @param length    the length of the compressed stream / 압축 스트림의 길이
     * @param maxOutput the largest accepted output in bytes / 받아들이는 최대 출력(바이트)
     */
    private Brotli(byte[] data, int offset, int length, int maxOutput) {
        this.data = data;
        this.pos = offset;
        this.end = offset + length;
        this.maxOutput = maxOutput;
    }

    /**
     * Decodes a Brotli stream.
     * <p>
     * Brotli 스트림을 디코딩한다.
     *
     * @param data      the bytes that hold the stream / 스트림이 든 바이트
     * @param offset    the first byte of the stream / 스트림의 첫 바이트
     * @param length    the length of the stream (bytes after it are not read) / 스트림의 길이(그 뒤의 바이트는 읽지 않는다)
     * @param maxOutput the largest output that the caller accepts, in bytes / 호출자가 받아들이는 최대 출력(바이트)
     * @return the uncompressed bytes / 압축을 푼 바이트
     * @throws BrotliException if the stream is not valid, is cut off or would produce more than <code>maxOutput</code> bytes / 스트림이 올바르지 않거나 끊겼거나 <code>maxOutput</code> 바이트보다
     *                         많이 만들면
     */
    public static byte[] decode(byte[] data, int offset, int length, int maxOutput) throws BrotliException {
        return decode(data, offset, length, maxOutput, null);
    }

    /**
     * Decodes a Brotli stream and reports which parts of the format it used (for the tests, which check that their streams cover the format).
     * <p>
     * Brotli 스트림을 디코딩하고 형식의 어느 부분을 썼는지 알린다(시험이 자기 스트림이 형식을 덮는지 확인하는 데 쓴다).
     *
     * @param data      the bytes that hold the stream / 스트림이 든 바이트
     * @param offset    the first byte of the stream / 스트림의 첫 바이트
     * @param length    the length of the stream / 스트림의 길이
     * @param maxOutput the largest accepted output in bytes / 받아들이는 최대 출력(바이트)
     * @param stats     null, or an array of at least 8 that receives: 0 the number of references to the static dictionary, 1 the number of compressed meta-blocks, 2 the number
     *                  of uncompressed meta-blocks, 3 the bit mask of the context modes that were used (bit 0 LSB6 ... bit 3 Signed), 4 the largest number of block types of
     *                  one category, 5 the largest number of literal prefix trees, 6 the number of meta-blocks with NPOSTFIX or NDIRECT not zero, 7 the number of different
     *                  word transformations that were used / null 이거나, 크기 8 이상으로 다음을 받는 배열: 0 정적 사전 참조 수, 1 압축 메타 블록 수, 2 압축하지 않은 메타 블록 수,
     *                  3 쓴 문맥 모드의 비트 마스크(비트 0 LSB6 ... 비트 3 Signed), 4 한 범주의 블록 종류 수 최대값, 5 리터럴 접두사 트리 수 최대값, 6 NPOSTFIX 나 NDIRECT 가 0 이 아닌 메타 블록 수,
     *                  7 쓴 단어 변환의 서로 다른 수
     * @return the uncompressed bytes / 압축을 푼 바이트
     * @throws BrotliException if the stream is not valid / 스트림이 올바르지 않으면
     */
    static byte[] decode(byte[] data, int offset, int length, int maxOutput, int[] stats) throws BrotliException {
        if (data == null || offset < 0 || length < 0 || offset + length > data.length || maxOutput < 0) {
            throw new BrotliException("invalid arguments");
        }
        Brotli d = new Brotli(data, offset, length, maxOutput);
        d.run();
        if (stats != null) {
            int transforms = 0;
            // 쓴 변환의 수를 센다.
            for (int i = 0; i < d.usedTransforms.length; i++) {
                if (d.usedTransforms[i]) {
                    transforms++;
                }
            }
            stats[0] = d.dictionaryReferences;
            stats[1] = d.compressedBlocks;
            stats[2] = d.uncompressedBlocks;
            stats[3] = d.modeMask;
            stats[4] = d.maxBlockTypes;
            stats[5] = d.maxLiteralTrees;
            stats[6] = d.distanceParameterBlocks;
            stats[7] = transforms;
        }
        return Arrays.copyOf(d.out, d.outLen);
    }

    /** The number of references to the static dictionary (for the statistics). 정적 사전 참조 수(통계용). */
    private int dictionaryReferences;
    /** The number of compressed meta-blocks (for the statistics). 압축 메타 블록 수(통계용). */
    private int compressedBlocks;
    /** The number of uncompressed meta-blocks (for the statistics). 압축하지 않은 메타 블록 수(통계용). */
    private int uncompressedBlocks;
    /** The bit mask of the context modes that were used (for the statistics). 쓴 문맥 모드의 비트 마스크(통계용). */
    private int modeMask;
    /** The largest number of block types of a category (for the statistics). 범주의 블록 종류 수 최대값(통계용). */
    private int maxBlockTypes;
    /** The largest number of literal prefix trees (for the statistics). 리터럴 접두사 트리 수 최대값(통계용). */
    private int maxLiteralTrees;
    /** The number of meta-blocks with NPOSTFIX or NDIRECT not zero (for the statistics). NPOSTFIX 나 NDIRECT 가 0 이 아닌 메타 블록 수(통계용). */
    private int distanceParameterBlocks;
    /** The word transformations that were used (for the statistics). 쓴 단어 변환(통계용). */
    private final boolean[] usedTransforms = new boolean[121];

    /**
     * Decodes a Brotli stream that fills a whole array.
     * <p>
     * 배열 전체를 채운 Brotli 스트림을 디코딩한다.
     *
     * @param data      the stream / 스트림
     * @param maxOutput the largest output that the caller accepts, in bytes / 호출자가 받아들이는 최대 출력(바이트)
     * @return the uncompressed bytes / 압축을 푼 바이트
     * @throws BrotliException if the stream is not valid / 스트림이 올바르지 않으면
     */
    public static byte[] decode(byte[] data, int maxOutput) throws BrotliException {
        return decode(data, 0, data.length, maxOutput);
    }

    // ------------------------------------------------------------------------------------------------------ bits

    /**
     * Reads an integer of up to 24 bits (the lowest bit first, section 1.5.1).
     * <p>
     * 최대 24 비트의 정수를 읽는다(낮은 비트가 먼저, 1.5.1 절).
     *
     * @param n the number of bits / 비트 수
     * @return the value / 값
     * @throws BrotliException if the stream ends / 스트림이 끝나면
     */
    private int bits(int n) throws BrotliException {
        // 필요한 비트가 모일 때까지 바이트를 적재한다.
        while (bitCount < n) {
            if (pos >= end) {
                throw new BrotliException("the stream ends too early");
            }
            bitBuffer |= (long) (data[pos++] & 0xFF) << bitCount;
            bitCount += 8;
        }
        int v = (int) (bitBuffer & ((1L << n) - 1));
        bitBuffer >>>= n;
        bitCount -= n;
        return v;
    }

    /**
     * Skips the bits up to the next byte boundary; they must be zero.
     * <p>
     * 다음 바이트 경계까지의 비트를 건너뛴다. 모두 0 이어야 한다.
     *
     * @throws BrotliException if a skipped bit is set / 건너뛴 비트가 1 이면
     */
    private void alignToByte() throws BrotliException {
        int drop = bitCount & 7;
        if (drop > 0 && bits(drop) != 0) {
            throw new BrotliException("the padding bits are not zero");
        }
    }

    /**
     * Gives the whole bytes that are still in the bit buffer back to the input, after the position was aligned to a byte boundary.
     * <p>
     * 위치를 바이트 경계에 맞춘 뒤 비트 버퍼에 남은 온전한 바이트를 입력으로 돌려준다.
     */
    private void unloadBuffer() {
        pos -= bitCount >> 3;
        bitBuffer = 0;
        bitCount = 0;
    }

    // ------------------------------------------------------------------------------------------------------ output

    /**
     * Makes room for more output.
     * <p>
     * 출력을 더 담을 자리를 만든다.
     *
     * @param more the number of bytes to add / 더할 바이트 수
     * @throws BrotliException if the output would be larger than the limit / 출력이 한도를 넘으면
     */
    private void reserve(int more) throws BrotliException {
        if ((long) outLen + more > maxOutput) {
            throw new BrotliException("the output is larger than " + maxOutput + " bytes");
        }
        if (outLen + more > out.length) {
            out = Arrays.copyOf(out, (int) Math.min((long) maxOutput + 1, Math.max((long) out.length * 2, (long) outLen + more)));
        }
    }

    // ------------------------------------------------------------------------------------------------------ prefix codes

    /**
     * Builds a canonical prefix code from code lengths (section 3.2).
     * <p>
     * 코드 길이에서 정규 접두사 코드를 만든다(3.2 절).
     *
     * @param lengths the code length of every symbol, 0 for a symbol that is not used / 기호마다 코드 길이. 쓰지 않는 기호는 0
     * @param n       the number of symbols / 기호 수
     * @return the code / 코드
     */
    private static Code build(int[] lengths, int n) {
        Code c = new Code();
        // 길이별 코드 수를 센다.
        for (int i = 0; i < n; i++) {
            c.count[lengths[i]]++;
        }
        c.count[0] = 0;
        int[] offset = new int[17];
        // 길이별 기호 구간의 시작을 정한다.
        for (int len = 1; len < 16; len++) {
            offset[len + 1] = offset[len] + c.count[len];
        }
        c.symbols = new int[offset[16]];
        // 길이 다음 기호 값 순서로 기호를 놓는다.
        for (int i = 0; i < n; i++) {
            if (lengths[i] != 0) {
                c.symbols[offset[lengths[i]]++] = i;
            }
        }
        return c;
    }

    /**
     * Decodes one symbol (a prefix code is packed starting with its most significant bit, section 1.5.1).
     * <p>
     * 기호 하나를 디코딩한다(접두사 코드는 가장 높은 비트부터 채운다. 1.5.1 절).
     *
     * @param c the code / 코드
     * @return the symbol / 기호
     * @throws BrotliException if no code matches or the stream ends / 맞는 코드가 없거나 스트림이 끝나면
     */
    private int symbol(Code c) throws BrotliException {
        if (c.single >= 0) {
            return c.single;
        }
        int code = 0;
        int first = 0;
        int index = 0;
        // 코드 길이를 1 부터 늘려 가며 비트를 하나씩 읽어 맞는 코드를 찾는다.
        for (int len = 1; len < 16; len++) {
            code |= bits(1);
            int count = c.count[len];
            if (code - count < first) {
                return c.symbols[index + (code - first)];
            }
            index += count;
            first += count;
            first <<= 1;
            code <<= 1;
        }
        throw new BrotliException("an invalid prefix code was read");
    }

    /**
     * Returns the smallest number of bits that can hold every symbol of an alphabet.
     * <p>
     * 알파벳의 모든 기호를 담을 수 있는 가장 작은 비트 수를 반환한다.
     *
     * @param alphabetSize the size of the alphabet / 알파벳 크기
     * @return the number of bits / 비트 수
     */
    private static int alphabetBits(int alphabetSize) {
        return 32 - Integer.numberOfLeadingZeros(Math.max(1, alphabetSize - 1));
    }

    /**
     * Reads a prefix code of an alphabet (sections 3.4 and 3.5).
     * <p>
     * 알파벳의 접두사 코드를 읽는다(3.4, 3.5 절).
     *
     * @param alphabetSize the size of the alphabet / 알파벳 크기
     * @return the code / 코드
     * @throws BrotliException if the description of the code is invalid / 코드의 설명이 올바르지 않으면
     */
    private Code readCode(int alphabetSize) throws BrotliException {
        int hskip = bits(2);
        if (hskip == 1) {
            return readSimpleCode(alphabetSize);
        }
        // 코드 길이 알파벳의 코드 길이를 정해진 순서로 읽는다(앞의 hskip 개는 0 이다).
        int[] codeLengthLengths = new int[18];
        int space = 32;
        int numCodes = 0;
        // 길이를 하나씩 읽으며 남은 공간을 줄인다(공간이 다 차면 멈춘다).
        for (int i = hskip; i < 18; i++) {
            int len = codeLengthCodeLength();
            codeLengthLengths[CODE_LENGTH_ORDER[i]] = len;
            if (len != 0) {
                space -= 32 >> len;
                numCodes++;
                if (space <= 0) {
                    break;
                }
            }
        }
        if (numCodes != 1 && space != 0) {
            throw new BrotliException("the code length code is not complete");
        }
        Code lengthCode = build(codeLengthLengths, 18);
        if (numCodes == 1) {
            // 길이가 0 인 코드 하나: 그 기호가 비트 없이 나온다.
            for (int i = 0; i < 18; i++) {
                if (codeLengthLengths[i] != 0) {
                    lengthCode.single = i;
                }
            }
        }
        int[] lengths = new int[alphabetSize];
        int symbol = 0;
        int previous = 8;
        int repeat = 0;
        int repeatLength = 0;
        int remaining = 32768;
        // 기호마다 코드 길이를 읽는다(16 은 앞의 길이를, 17 은 0 을 되풀이한다).
        while (symbol < alphabetSize && remaining > 0) {
            int c = symbol(lengthCode);
            if (c < 16) {
                repeat = 0;
                lengths[symbol++] = c;
                if (c != 0) {
                    previous = c;
                    remaining -= 32768 >> c;
                }
                continue;
            }
            int extra = c == 16 ? 2 : 3;
            int newLength = c == 16 ? previous : 0;
            if (repeatLength != newLength) {
                repeat = 0;
                repeatLength = newLength;
            }
            int old = repeat;
            if (repeat > 0) {
                repeat -= 2;
                repeat <<= extra;
            }
            repeat += bits(extra) + 3;
            int delta = repeat - old;
            if (symbol + delta > alphabetSize) {
                throw new BrotliException("a code length repeat is too long");
            }
            // 되풀이한 길이를 채운다.
            for (int k = 0; k < delta; k++) {
                lengths[symbol++] = repeatLength;
            }
            if (repeatLength != 0) {
                remaining -= delta << (15 - repeatLength);
            }
        }
        if (remaining != 0) {
            throw new BrotliException("the prefix code is not complete");
        }
        return build(lengths, alphabetSize);
    }

    /**
     * Reads one code length of the code length alphabet with the variable-length code of section 3.5.
     * <p>
     * 3.5 절의 가변 길이 코드로 코드 길이 알파벳의 코드 길이 하나를 읽는다.
     *
     * @return the code length 0..5 / 코드 길이 0~5
     * @throws BrotliException if the stream ends / 스트림이 끝나면
     */
    private int codeLengthCodeLength() throws BrotliException {
        int v = bits(2);
        if (v == 0) {
            return 0;
        }
        if (v == 1) {
            return 4;
        }
        if (v == 2) {
            return 3;
        }
        if (bits(1) == 0) {
            return 2;
        }
        return bits(1) == 0 ? 1 : 5;
    }

    /**
     * Reads a simple prefix code of up to four symbols (section 3.4); its first two bits (the value 1) have been read.
     * <p>
     * 기호가 최대 넷인 단순 접두사 코드를 읽는다(3.4 절). 처음 두 비트(값 1)는 이미 읽었다.
     *
     * @param alphabetSize the size of the alphabet / 알파벳 크기
     * @return the code / 코드
     * @throws BrotliException if a symbol is out of the alphabet or repeated / 기호가 알파벳 밖이거나 되풀이되면
     */
    private Code readSimpleCode(int alphabetSize) throws BrotliException {
        int nsym = bits(2) + 1;
        int[] symbols = new int[nsym];
        int width = alphabetBits(alphabetSize);
        // 기호를 읽고 범위와 중복을 확인한다.
        for (int i = 0; i < nsym; i++) {
            symbols[i] = bits(width);
            if (symbols[i] >= alphabetSize) {
                throw new BrotliException("a symbol is outside of the alphabet");
            }
            // 앞에서 읽은 기호와 같은 값이 있는지 본다.
            for (int k = 0; k < i; k++) {
                if (symbols[k] == symbols[i]) {
                    throw new BrotliException("a symbol is repeated");
                }
            }
        }
        int[] lengths = new int[alphabetSize];
        if (nsym == 1) {
            Code c = new Code();
            c.single = symbols[0];
            return c;
        }
        if (nsym == 2) {
            lengths[symbols[0]] = 1;
            lengths[symbols[1]] = 1;
        } else if (nsym == 3) {
            lengths[symbols[0]] = 1;
            lengths[symbols[1]] = 2;
            lengths[symbols[2]] = 2;
        } else if (bits(1) == 0) {
            lengths[symbols[0]] = 2;
            lengths[symbols[1]] = 2;
            lengths[symbols[2]] = 2;
            lengths[symbols[3]] = 2;
        } else {
            lengths[symbols[0]] = 1;
            lengths[symbols[1]] = 2;
            lengths[symbols[2]] = 3;
            lengths[symbols[3]] = 3;
        }
        return build(lengths, alphabetSize);
    }

    /**
     * Reads a variable-length count of 1 to 256 (the numbers of block types and of prefix trees, section 9.2).
     * <p>
     * 1~256 의 가변 길이 개수를 읽는다(블록 종류와 접두사 트리의 수. 9.2 절).
     *
     * @return the count / 개수
     * @throws BrotliException if the stream ends / 스트림이 끝나면
     */
    private int readCount() throws BrotliException {
        if (bits(1) == 0) {
            return 1;
        }
        int n = bits(3);
        return n == 0 ? 2 : (1 << n) + 1 + bits(n);
    }

    // ------------------------------------------------------------------------------------------------------ meta-blocks

    /**
     * Decodes the whole stream: the header and the meta-blocks (section 10).
     * <p>
     * 스트림 전체를 디코딩한다: 머리와 메타 블록(10 절).
     *
     * @throws BrotliException if the stream is invalid / 스트림이 올바르지 않으면
     */
    private void run() throws BrotliException {
        windowSize = (1 << readWindowBits()) - 16;
        boolean last;
        // 마지막 메타 블록까지 메타 블록을 하나씩 읽는다.
        do {
            last = bits(1) == 1;
            if (last && bits(1) == 1) {
                break;
            }
            int nibbles = bits(2);
            if (nibbles == 3) {
                skipMetadata();
                continue;
            }
            nibbles += 4;
            int mlen = bits(nibbles * 4) + 1;
            if (nibbles > 4 && (mlen - 1) >>> ((nibbles - 1) * 4) == 0) {
                throw new BrotliException("the length of a meta-block has a leading zero nibble");
            }
            if (!last && bits(1) == 1) {
                copyUncompressed(mlen);
                continue;
            }
            compressed(mlen);
        } while (!last);
    }

    /**
     * Reads the window size of the stream header (section 9.1).
     * <p>
     * 스트림 머리의 윈도 크기를 읽는다(9.1 절).
     *
     * @return WBITS, 10..24 / WBITS, 10~24
     * @throws BrotliException if the pattern is invalid / 패턴이 올바르지 않으면
     */
    private int readWindowBits() throws BrotliException {
        if (bits(1) == 0) {
            return 16;
        }
        int n = bits(3);
        if (n != 0) {
            return 17 + n;
        }
        int m = bits(3);
        if (m == 1) {
            throw new BrotliException("an invalid window size pattern");
        }
        return m == 0 ? 17 : 8 + m;
    }

    /**
     * Skips a metadata meta-block (section 9.2: MNIBBLES is zero).
     * <p>
     * 메타데이터 메타 블록을 건너뛴다(9.2 절: MNIBBLES 가 0).
     *
     * @throws BrotliException if the block is invalid / 블록이 올바르지 않으면
     */
    private void skipMetadata() throws BrotliException {
        if (bits(1) != 0) {
            throw new BrotliException("the reserved bit of a metadata block is set");
        }
        int skipBytes = bits(2);
        long skip = 0;
        if (skipBytes > 0) {
            skip = (long) bits(skipBytes * 8) + 1;
            if (skipBytes > 1 && (skip - 1) >> ((skipBytes - 1) * 8) == 0) {
                throw new BrotliException("the length of a metadata block has a leading zero byte");
            }
        }
        alignToByte();
        unloadBuffer();
        if (pos + skip > end) {
            throw new BrotliException("the metadata is cut off");
        }
        pos += (int) skip;
    }

    /**
     * Copies an uncompressed meta-block to the output (section 9.2).
     * <p>
     * 압축하지 않은 메타 블록을 출력에 복사한다(9.2 절).
     *
     * @param mlen the number of bytes / 바이트 수
     * @throws BrotliException if the block is cut off or too large / 블록이 끊겼거나 너무 크면
     */
    private void copyUncompressed(int mlen) throws BrotliException {
        alignToByte();
        unloadBuffer();
        if (pos + (long) mlen > end) {
            throw new BrotliException("an uncompressed meta-block is cut off");
        }
        reserve(mlen);
        uncompressedBlocks++;
        System.arraycopy(data, pos, out, outLen, mlen);
        outLen += mlen;
        pos += mlen;
    }

    /** The categories of the block switch commands. 블록 전환 명령의 범주. */
    private static final int LITERAL = 0;
    /** The insert-and-copy category. 삽입·복사 범주. */
    private static final int COMMAND = 1;
    /** The distance category. 거리 범주. */
    private static final int DISTANCE = 2;

    /**
     * Decodes a compressed meta-block (sections 9.2, 9.3 and 10).
     * <p>
     * 압축된 메타 블록을 디코딩한다(9.2, 9.3, 10 절).
     *
     * @param mlen the length of the uncompressed data of the block / 블록의 압축을 푼 데이터 길이
     * @throws BrotliException if the block is invalid / 블록이 올바르지 않으면
     */
    private void compressed(int mlen) throws BrotliException {
        int[] blockTypes = new int[3];
        Code[] typeCode = new Code[3];
        Code[] countCode = new Code[3];
        int[] left = new int[3];
        int[] current = new int[3];
        int[] previous = new int[3];
        // 범주(리터럴, 삽입·복사, 거리)마다 블록 종류의 수와 전환 코드, 첫 블록의 개수를 읽는다.
        for (int cat = 0; cat < 3; cat++) {
            blockTypes[cat] = readCount();
            if (blockTypes[cat] >= 2) {
                typeCode[cat] = readCode(blockTypes[cat] + 2);
                countCode[cat] = readCode(26);
                left[cat] = readBlockCount(countCode[cat]);
            } else {
                left[cat] = Integer.MAX_VALUE;
            }
            previous[cat] = 1;
            current[cat] = 0;
        }
        int postfix = bits(2);
        int direct = bits(4) << postfix;
        compressedBlocks++;
        if (postfix != 0 || direct != 0) {
            distanceParameterBlocks++;
        }
        // 통계: 범주의 블록 종류 수 최대값.
        for (int cat = 0; cat < 3; cat++) {
            maxBlockTypes = Math.max(maxBlockTypes, blockTypes[cat]);
        }
        int[] modes = new int[blockTypes[LITERAL]];
        // 리터럴 블록 종류마다 문맥 모드를 읽는다.
        for (int i = 0; i < modes.length; i++) {
            modes[i] = bits(2);
            modeMask |= 1 << modes[i];
        }
        int literalTrees = readCount();
        maxLiteralTrees = Math.max(maxLiteralTrees, literalTrees);
        byte[] literalMap = literalTrees >= 2 ? readContextMap(64 * blockTypes[LITERAL], literalTrees) : new byte[64 * blockTypes[LITERAL]];
        int distanceTrees = readCount();
        byte[] distanceMap = distanceTrees >= 2 ? readContextMap(4 * blockTypes[DISTANCE], distanceTrees) : new byte[4 * blockTypes[DISTANCE]];
        Code[] literalCodes = new Code[literalTrees];
        // 리터럴 접두사 코드를 읽는다.
        for (int i = 0; i < literalTrees; i++) {
            literalCodes[i] = readCode(256);
        }
        Code[] commandCodes = new Code[blockTypes[COMMAND]];
        // 삽입·복사 길이 접두사 코드를 읽는다.
        for (int i = 0; i < commandCodes.length; i++) {
            commandCodes[i] = readCode(704);
        }
        Code[] distanceCodes = new Code[distanceTrees];
        // 거리 접두사 코드를 읽는다.
        for (int i = 0; i < distanceTrees; i++) {
            distanceCodes[i] = readCode(16 + direct + (48 << postfix));
        }
        int target = outLen + mlen;
        if (target > maxOutput || target < 0) {
            throw new BrotliException("the output is larger than " + maxOutput + " bytes");
        }
        // 메타 블록의 출력이 MLEN 이 될 때까지 명령(삽입·복사)을 하나씩 처리한다.
        while (outLen < target) {
            if (left[COMMAND] == 0) {
                switchBlock(COMMAND, blockTypes, typeCode, countCode, left, current, previous);
            }
            left[COMMAND]--;
            int cmd = symbol(commandCodes[current[COMMAND]]);
            int cell = cmd >> 6;
            int insertCode = CELL_INSERT[cell] + ((cmd >> 3) & 7);
            int copyCode = CELL_COPY[cell] + (cmd & 7);
            int insertLength = INSERT_BASE[insertCode] + bits(INSERT_EXTRA[insertCode]);
            int copyLength = COPY_BASE[copyCode] + bits(COPY_EXTRA[copyCode]);
            if ((long) outLen + insertLength > target) {
                throw new BrotliException("an insert exceeds the length of the meta-block");
            }
            reserve(insertLength);
            // 삽입할 리터럴을 문맥에 따른 코드로 하나씩 읽는다.
            for (int i = 0; i < insertLength; i++) {
                if (left[LITERAL] == 0) {
                    switchBlock(LITERAL, blockTypes, typeCode, countCode, left, current, previous);
                }
                left[LITERAL]--;
                int p1 = outLen >= 1 ? out[outLen - 1] & 0xFF : 0;
                int p2 = outLen >= 2 ? out[outLen - 2] & 0xFF : 0;
                int cid = contextId(modes[current[LITERAL]], p1, p2);
                int tree = literalMap[64 * current[LITERAL] + cid] & 0xFF;
                out[outLen++] = (byte) symbol(literalCodes[tree]);
            }
            if (outLen == target) {
                break;
            }
            int distance;
            boolean push = false;
            if (cmd < 128) {
                distance = distances[0];
            } else {
                if (left[DISTANCE] == 0) {
                    switchBlock(DISTANCE, blockTypes, typeCode, countCode, left, current, previous);
                }
                left[DISTANCE]--;
                int cid = copyLength > 4 ? 3 : copyLength - 2;
                int dcode = symbol(distanceCodes[distanceMap[4 * current[DISTANCE] + cid] & 0xFF]);
                if (dcode < 16) {
                    distance = distances[SHORT_BASE[dcode]] + SHORT_OFFSET[dcode];
                    if (distance <= 0) {
                        throw new BrotliException("a short distance code gives a distance that is not positive");
                    }
                    push = dcode != 0;
                } else if (dcode < 16 + direct) {
                    distance = dcode - 15;
                    push = true;
                } else {
                    int h = dcode - direct - 16;
                    int nbits = 1 + (h >> (postfix + 1));
                    int hcode = h >> postfix;
                    int lcode = h & ((1 << postfix) - 1);
                    int offset = ((2 + (hcode & 1)) << nbits) - 4;
                    distance = ((offset + bits(nbits)) << postfix) + lcode + direct + 1;
                    push = true;
                }
            }
            int maxDistance = Math.min(windowSize, outLen);
            if (distance > maxDistance) {
                // 거리가 윈도와 지금까지의 출력을 넘으면 정적 사전의 단어이다(8 절). 거리 링 버퍼에는 넣지 않는다.
                dictionaryWord(copyLength, distance - (maxDistance + 1), target);
                continue;
            }
            if (push) {
                distances[3] = distances[2];
                distances[2] = distances[1];
                distances[1] = distances[0];
                distances[0] = distance;
            }
            if ((long) outLen + copyLength > target) {
                throw new BrotliException("a copy exceeds the length of the meta-block");
            }
            reserve(copyLength);
            // 앞에서 distance 만큼 떨어진 바이트를 하나씩 복사한다(겹쳐도 앞에서부터 되풀이된다).
            for (int i = 0; i < copyLength; i++) {
                out[outLen] = out[outLen - distance];
                outLen++;
            }
        }
        if (outLen != target) {
            throw new BrotliException("the meta-block is not complete");
        }
    }

    /**
     * Computes the context id of a literal (section 7.1).
     * <p>
     * 리터럴의 문맥 ID 를 계산한다(7.1 절).
     *
     * @param mode the context mode: 0 LSB6, 1 MSB6, 2 UTF8, 3 Signed / 문맥 모드: 0 LSB6, 1 MSB6, 2 UTF8, 3 Signed
     * @param p1   the last byte / 마지막 바이트
     * @param p2   the byte before it / 그 앞의 바이트
     * @return the context id 0..63 / 문맥 ID 0~63
     */
    private int contextId(int mode, int p1, int p2) {
        if (mode == 0) {
            return p1 & 0x3F;
        }
        if (mode == 1) {
            return p1 >> 2;
        }
        if (lut == null) {
            lut = base64(BrotliData.CONTEXT_LUTS);
        }
        if (mode == 2) {
            return (lut[p1] & 0xFF) | (lut[256 + p2] & 0xFF);
        }
        return ((lut[512 + p1] & 0xFF) << 3) | (lut[512 + p2] & 0xFF);
    }

    /**
     * Reads a block count (section 6).
     * <p>
     * 블록 개수를 읽는다(6 절).
     *
     * @param code the prefix code of the block count codes / 블록 개수 코드의 접두사 코드
     * @return the count / 개수
     * @throws BrotliException if the stream ends / 스트림이 끝나면
     */
    private int readBlockCount(Code code) throws BrotliException {
        int s = symbol(code);
        return BLOCK_BASE[s] + bits(BLOCK_EXTRA[s]);
    }

    /**
     * Switches to the next block of a category: reads the block type and the block count (section 6).
     * <p>
     * 범주의 다음 블록으로 바꾼다: 블록 종류와 블록 개수를 읽는다(6 절).
     *
     * @param cat        the category / 범주
     * @param blockTypes the numbers of block types / 블록 종류 수
     * @param typeCode   the block type codes / 블록 종류 코드
     * @param countCode  the block count codes / 블록 개수 코드
     * @param left       the commands left in the current blocks / 현재 블록에 남은 개수
     * @param current    the current block types / 현재 블록 종류
     * @param previous   the previous block types / 이전 블록 종류
     * @throws BrotliException if the stream is invalid / 스트림이 올바르지 않으면
     */
    private void switchBlock(int cat, int[] blockTypes, Code[] typeCode, Code[] countCode, int[] left, int[] current, int[] previous) throws BrotliException {
        if (blockTypes[cat] < 2) {
            left[cat] = Integer.MAX_VALUE;
            return;
        }
        int s = symbol(typeCode[cat]);
        int type;
        if (s == 0) {
            type = previous[cat];
        } else if (s == 1) {
            type = current[cat] + 1;
        } else {
            type = s - 2;
        }
        if (type >= blockTypes[cat]) {
            type -= blockTypes[cat];
        }
        if (type >= blockTypes[cat]) {
            throw new BrotliException("an invalid block type");
        }
        previous[cat] = current[cat];
        current[cat] = type;
        left[cat] = readBlockCount(countCode[cat]);
    }

    /**
     * Reads a context map (section 7.3) with the run length coding of zeros and the optional inverse move-to-front transform.
     * <p>
     * 0 의 런 길이 부호화와 선택적인 역 move-to-front 변환이 있는 문맥 지도를 읽는다(7.3 절).
     *
     * @param size  the number of values / 값의 수
     * @param trees the number of prefix trees (the values are 0..trees-1) / 접두사 트리 수(값은 0~trees-1)
     * @return the map / 지도
     * @throws BrotliException if the map is invalid / 지도가 올바르지 않으면
     */
    private byte[] readContextMap(int size, int trees) throws BrotliException {
        int rleMax = bits(1) == 0 ? 0 : bits(4) + 1;
        Code code = readCode(trees + rleMax);
        byte[] map = new byte[size];
        int i = 0;
        // 값을 하나씩 읽는다(1~rleMax 기호는 0 의 되풀이이다).
        while (i < size) {
            int s = symbol(code);
            if (s == 0) {
                map[i++] = 0;
            } else if (s <= rleMax) {
                int reps = (1 << s) + bits(s);
                if (i + reps > size) {
                    throw new BrotliException("a run in a context map is too long");
                }
                i += reps;
            } else {
                map[i++] = (byte) (s - rleMax);
            }
        }
        if (bits(1) == 1) {
            int[] mtf = new int[256];
            // 역 move-to-front 변환의 목록을 초기화한다.
            for (int k = 0; k < 256; k++) {
                mtf[k] = k;
            }
            // 값마다 목록의 그 위치 값으로 바꾸고 그 값을 맨 앞으로 옮긴다.
            for (int k = 0; k < size; k++) {
                int index = map[k] & 0xFF;
                int value = mtf[index];
                map[k] = (byte) value;
                // 값 앞의 항목을 한 칸씩 뒤로 밀어 값을 맨 앞에 둔다.
                for (; index > 0; index--) {
                    mtf[index] = mtf[index - 1];
                }
                mtf[0] = value;
            }
        }
        return map;
    }

    // ------------------------------------------------------------------------------------------------------ dictionary

    /**
     * Appends a word of the static dictionary with its transformation (section 8).
     * <p>
     * 정적 사전의 단어를 변환과 함께 출력에 더한다(8 절).
     *
     * @param length the length of the word (the copy length of the command) / 단어 길이(명령의 복사 길이)
     * @param wordId the number of the word: the distance minus the largest distance plus one / 단어 번호: 거리에서 (최대 거리 + 1)을 뺀 값
     * @param target the end of the meta-block in the output / 출력에서 메타 블록의 끝
     * @throws BrotliException if the reference is invalid / 참조가 올바르지 않으면
     */
    private void dictionaryWord(int length, int wordId, int target) throws BrotliException {
        if (length < 4 || length > MAX_WORD) {
            throw new BrotliException("a dictionary reference of length " + length);
        }
        if (dictionary == null) {
            loadDictionary();
        }
        int depth = BrotliData.NDBITS[length];
        int index = wordId & ((1 << depth) - 1);
        int transform = wordId >> depth;
        if (transform > 120 || wordId < 0) {
            throw new BrotliException("an invalid dictionary word number");
        }
        dictionaryReferences++;
        usedTransforms[transform] = true;
        int from = dictionaryOffset[length] + index * length;
        int start = 0;
        int stop = length;
        int k = kind[transform];
        if (k >= 3 && k <= 11) {
            start = Math.min(k - 2, length);
        } else if (k >= 12) {
            stop = Math.max(0, length - (k - 11));
        }
        byte[] word = new byte[Math.max(0, stop - start)];
        System.arraycopy(dictionary, from + start, word, 0, word.length);
        if (k == 1) {
            ferment(word, 0);
        } else if (k == 2) {
            // 단어를 처음부터 끝까지 글자 단위로 변환한다.
            for (int i = 0; i < word.length;) {
                i += ferment(word, i);
            }
        }
        long total = (long) prefix[transform].length + word.length + suffix[transform].length;
        if (outLen + total > target) {
            throw new BrotliException("a dictionary word exceeds the length of the meta-block");
        }
        reserve((int) total);
        System.arraycopy(prefix[transform], 0, out, outLen, prefix[transform].length);
        outLen += prefix[transform].length;
        System.arraycopy(word, 0, out, outLen, word.length);
        outLen += word.length;
        System.arraycopy(suffix[transform], 0, out, outLen, suffix[transform].length);
        outLen += suffix[transform].length;
    }

    /**
     * The Ferment function of section 8 for the character at a position: changes the case of a letter of the word in place and tells how many bytes
     * the character takes (UTF-8 lead byte rules).
     * <p>
     * 8 절의 Ferment 함수: 단어의 한 위치 글자의 대소문자를 제자리에서 바꾸고 그 글자가 차지하는 바이트 수를 알려 준다(UTF-8 선행 바이트 규칙).
     *
     * @param word the word / 단어
     * @param pos  the position of the character / 글자의 위치
     * @return the number of bytes of the character / 글자의 바이트 수
     */
    private static int ferment(byte[] word, int pos) {
        if (word.length == 0) {
            return 1;
        }
        int b = word[pos] & 0xFF;
        if (b < 192) {
            if (b >= 97 && b <= 122) {
                word[pos] = (byte) (b ^ 32);
            }
            return 1;
        }
        if (b < 224) {
            if (pos + 1 < word.length) {
                word[pos + 1] ^= 32;
            }
            return 2;
        }
        if (pos + 2 < word.length) {
            word[pos + 2] ^= 5;
        }
        return 3;
    }

    /**
     * Reads the static dictionary, its word offsets and the word transformations from {@link BrotliData}.
     * <p>
     * {@link BrotliData} 에서 정적 사전, 단어 오프셋, 단어 변환을 읽는다.
     */
    private void loadDictionary() {
        StringBuilder text = new StringBuilder();
        // 사전의 Base64 조각을 이어 붙여 해독한다.
        for (int i = 0; i < BrotliData.DICTIONARY.length; i++) {
            text.append(BrotliData.DICTIONARY[i]);
        }
        dictionary = base64(text.toString());
        dictionaryOffset = new int[26];
        // 길이별 단어 수와 오프셋을 NDBITS 로 계산한다.
        for (int len = 0; len < 25; len++) {
            int words = len < 4 ? 0 : 1 << BrotliData.NDBITS[len];
            dictionaryOffset[len + 1] = dictionaryOffset[len] + len * words;
        }
        byte[] t = base64(BrotliData.TRANSFORMS);
        prefix = new byte[121][];
        suffix = new byte[121][];
        kind = new int[121];
        int p = 0;
        // 변환마다 접두사, 번호, 접미사를 읽는다(접두사와 접미사는 0 으로 끝난다).
        for (int i = 0; i < 121; i++) {
            int s = p;
            // 접두사는 0 바이트까지이다.
            while (t[p] != 0) {
                p++;
            }
            prefix[i] = Arrays.copyOfRange(t, s, p);
            p++;
            kind[i] = t[p++];
            s = p;
            // 접미사는 0 바이트까지이다.
            while (t[p] != 0) {
                p++;
            }
            suffix[i] = Arrays.copyOfRange(t, s, p);
            p++;
        }
    }

    /**
     * Decodes Base64 text (the alphabet of RFC 4648 with padding).
     * <p>
     * Base64 텍스트를 해독한다(RFC 4648 의 알파벳과 패딩).
     *
     * @param s the text / 텍스트
     * @return the bytes / 바이트
     */
    private static byte[] base64(String s) {
        int pad = s.endsWith("==") ? 2 : (s.endsWith("=") ? 1 : 0);
        byte[] out = new byte[s.length() / 4 * 3 - pad];
        int o = 0;
        int acc = 0;
        int n = 0;
        // 글자마다 6 비트를 모아 바이트를 만든다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int v;
            if (c >= 'A' && c <= 'Z') {
                v = c - 'A';
            } else if (c >= 'a' && c <= 'z') {
                v = c - 'a' + 26;
            } else if (c >= '0' && c <= '9') {
                v = c - '0' + 52;
            } else if (c == '+') {
                v = 62;
            } else if (c == '/') {
                v = 63;
            } else {
                break;
            }
            acc = (acc << 6) | v;
            n += 6;
            if (n >= 8) {
                n -= 8;
                out[o++] = (byte) (acc >> n);
                acc &= (1 << n) - 1;
            }
        }
        return out;
    }
}
