package org.duckdns.hjow.swingwebpanel.media.audio.vorbis;

import java.util.Arrays;
import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * A Vorbis codebook (Vorbis I specification, section 3): the Huffman code of its entries and the optional vector quantization lookup that
 * turns an entry into a vector of floats. The codeword lengths are assigned to codewords by the algorithm of the specification (the
 * lowest unused code of each length) and decoding walks a binary tree built from them, bit by bit.
 * <p>
 * Vorbis 코드북(Vorbis I 규격 3절): 항목의 허프만 부호와, 항목을 float 벡터로 바꾸는 선택적 벡터 양자화 조회. 부호어 길이는 규격의 알고리즘(길이마다
 * 가장 낮은 미사용 부호)으로 부호어에 배정하며, 디코딩은 그로부터 만든 이진 트리를 비트 단위로 따라간다.
 */
final class VorbisCodebook {

    /**
     * Largest accepted number of entries.
     * <p>
     * 허용하는 최대 항목 수.
     */
    private static final int MAX_ENTRIES = 1 << 20;

    /**
     * Largest accepted number of tree nodes and lookup values.
     * <p>
     * 허용하는 트리 노드 및 조회 값의 최대 수.
     */
    private static final int MAX_NODES = 1 << 22;

    /**
     * Dimensions of the vectors.
     * <p>
     * 벡터의 차원 수.
     */
    final int dimensions;

    /**
     * Number of entries.
     * <p>
     * 항목 수.
     */
    final int entries;

    /**
     * Lookup type: 0 none, 1 implicit, 2 explicit.
     * <p>
     * 조회 유형: 0 없음, 1 암시적, 2 명시적.
     */
    private int lookupType;

    /**
     * Minimum value of the lookup.
     * <p>
     * 조회의 최솟값.
     */
    private float minimum;

    /**
     * Delta of the lookup.
     * <p>
     * 조회의 델타.
     */
    private float delta;

    /**
     * Whether every value of a vector is added to the previous one.
     * <p>
     * 벡터의 각 값을 이전 값에 더하는지 여부.
     */
    private boolean sequence;

    /**
     * Number of multiplicands per dimension (type 1) or the total (type 2).
     * <p>
     * 차원당 곱셈 계수 수(유형 1) 또는 전체 수(유형 2).
     */
    private int lookupValues;

    /**
     * The multiplicands.
     * <p>
     * 곱셈 계수.
     */
    private int[] multiplicands;

    /**
     * Binary tree: two children per node; a positive value is a node number, a negative value <code>~entry</code> is a leaf, 0 is unset.
     * <p>
     * 이진 트리: 노드마다 자식 둘. 양수는 노드 번호, 음수 <code>~항목</code> 은 잎, 0 은 비어 있음.
     */
    private int[] tree = new int[64];

    /**
     * Number of nodes in the tree.
     * <p>
     * 트리의 노드 수.
     */
    private int nodes = 1;

    /**
     * Parses a codebook.
     * <p>
     * 코드북을 해석한다.
     *
     * @param br packet bits positioned at the codebook / 코드북에 위치한 패킷 비트
     * @throws MediaException CORRUPT for invalid codebooks, TOO_LARGE for absurd sizes / 올바르지 않은 코드북은 CORRUPT, 터무니없는 크기는 TOO_LARGE
     */
    VorbisCodebook(VorbisBitReader br) throws MediaException {
        if (br.read(24) != 0x564342) {
            throw new MediaException(MediaException.Kind.CORRUPT, "invalid Vorbis codebook sync pattern");
        }
        dimensions = br.read(16);
        entries = br.read(24);
        if (dimensions < 1) {
            throw new MediaException(MediaException.Kind.CORRUPT, "a Vorbis codebook without dimensions");
        }
        if (entries < 0 || entries > MAX_ENTRIES) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "a Vorbis codebook with too many entries");
        }
        int[] lengths = new int[entries];
        boolean ordered = br.read(1) != 0;
        if (!ordered) {
            boolean sparse = br.read(1) != 0;
            // 항목마다 부호어 길이를 읽는다(드문드문한 코드북은 쓰는 항목만 길이가 있다).
            for (int i = 0; i < entries; i++) {
                if (sparse) {
                    lengths[i] = br.read(1) != 0 ? br.read(5) + 1 : 0;
                } else {
                    lengths[i] = br.read(5) + 1;
                }
            }
        } else {
            int cur = 0;
            int len = br.read(5) + 1;
            // 길이가 같은 항목의 묶음 크기를 읽으며 길이를 하나씩 늘린다.
            while (cur < entries) {
                int number = br.read(VorbisBitReader.ilog(entries - cur));
                if (number < 0 || number > entries - cur || len > 32 || br.isEndOfPacket()) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "invalid ordered Vorbis codebook");
                }
                Arrays.fill(lengths, cur, cur + number, len);
                cur += number;
                len++;
            }
        }
        if (br.isEndOfPacket()) {
            throw new MediaException(MediaException.Kind.CORRUPT, "a Vorbis codebook is cut off");
        }
        buildTree(lengths);
        parseLookup(br);
    }

    /**
     * Assigns codewords to the entries and builds the decoding tree.
     * <p>
     * 항목에 부호어를 배정하고 디코딩 트리를 만든다.
     *
     * @param lengths codeword lengths, 0 for unused entries / 부호어 길이. 쓰지 않는 항목은 0
     * @throws MediaException CORRUPT for an overspecified code / 과다 지정된 부호이면 CORRUPT
     */
    private void buildTree(int[] lengths) throws MediaException {
        long[] available = new long[33];
        boolean first = true;
        // 항목을 순서대로 보며 길이가 허용하는 가장 낮은 미사용 부호어를 배정한다.
        for (int i = 0; i < entries; i++) {
            int len = lengths[i];
            if (len == 0) {
                continue;
            }
            long code;
            if (first) {
                first = false;
                code = 0;
                // 항목마다 반복해서 처리한다.
                for (int y = 1; y <= len; y++) {
                    available[y] = 1L << (32 - y);
                }
            } else {
                int z = len;
                // 조건이 끝날 때까지 반복해서 처리한다.
                while (z > 0 && available[z] == 0) {
                    z--;
                }
                if (z == 0) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "overspecified Vorbis codebook");
                }
                long res = available[z];
                available[z] = 0;
                code = res >>> (32 - len);
                // 항목마다 반복해서 처리한다.
                for (int y = len; y > z; y--) {
                    available[y] = res + (1L << (32 - y));
                }
            }
            insert(code, len, i);
        }
    }

    /**
     * Inserts a codeword into the tree.
     * <p>
     * 부호어를 트리에 넣는다.
     *
     * @param code  the codeword, first bit read is the most significant of its <code>len</code> bits / 부호어. 먼저 읽는 비트가 <code>len</code> 비트 중 최상위
     * @param len   length in bits / 비트 길이
     * @param entry entry number / 항목 번호
     * @throws MediaException CORRUPT if the code collides with another or the tree gets too large / 부호가 다른 부호와 충돌하거나 트리가 너무 커지면 CORRUPT
     */
    private void insert(long code, int len, int entry) throws MediaException {
        int node = 0;
        // 부호어의 비트를 앞에서부터 따라가며 필요한 노드를 만든다.
        for (int b = len - 1; b >= 0; b--) {
            int slot = node * 2 + (int) ((code >> b) & 1);
            if (b == 0) {
                if (tree[slot] != 0) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "colliding Vorbis codewords");
                }
                tree[slot] = ~entry;
            } else {
                int next = tree[slot];
                if (next < 0) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "colliding Vorbis codewords");
                }
                if (next == 0) {
                    if (nodes >= MAX_NODES) {
                        throw new MediaException(MediaException.Kind.TOO_LARGE, "a Vorbis codebook tree is too large");
                    }
                    if ((nodes + 1) * 2 > tree.length) {
                        tree = Arrays.copyOf(tree, tree.length * 2);
                    }
                    next = nodes++;
                    tree[slot] = next;
                }
                node = next;
            }
        }
    }

    /**
     * Reads the vector lookup table.
     * <p>
     * 벡터 조회 표를 읽는다.
     *
     * @param br packet bits / 패킷 비트
     * @throws MediaException CORRUPT for an invalid table / 올바르지 않은 표이면 CORRUPT
     */
    private void parseLookup(VorbisBitReader br) throws MediaException {
        lookupType = br.read(4);
        if (lookupType == 0) {
            return;
        }
        if (lookupType > 2) {
            throw new MediaException(MediaException.Kind.CORRUPT, "invalid Vorbis lookup type");
        }
        minimum = unpack(br.read(32));
        delta = unpack(br.read(32));
        int valueBits = br.read(4) + 1;
        sequence = br.read(1) != 0;
        long values = lookupType == 1 ? lookup1Values() : (long) entries * dimensions;
        if (values > MAX_NODES || values * valueBits > br.bitsLeft()) {
            throw new MediaException(MediaException.Kind.CORRUPT, "invalid Vorbis lookup table size");
        }
        lookupValues = (int) values;
        multiplicands = new int[lookupValues];
        // 곱셈 계수를 하나씩 읽는다.
        for (int i = 0; i < lookupValues; i++) {
            multiplicands[i] = br.read(valueBits);
        }
        if (br.isEndOfPacket()) {
            throw new MediaException(MediaException.Kind.CORRUPT, "a Vorbis codebook is cut off");
        }
    }

    /**
     * The number of multiplicands of an implicit lookup: the largest integer whose power of the dimension count does not exceed the number of
     * entries.
     * <p>
     * 암시적 조회의 곱셈 계수 수: 차원 수 거듭제곱이 항목 수를 넘지 않는 가장 큰 정수.
     *
     * @return the number of values / 값의 수
     */
    private long lookup1Values() {
        long r = (long) Math.floor(Math.pow(entries, 1.0 / dimensions));
        // 부동소수점 오차를 정수 거듭제곱으로 바로잡는다.
        while (power(r + 1) <= entries) {
            r++;
        }
        // 조건이 끝날 때까지 반복해서 처리한다.
        while (r > 0 && power(r) > entries) {
            r--;
        }
        return r;
    }

    /**
     * Raises a number to the power of the dimension count, saturating at a large value.
     * <p>
     * 수를 차원 수 거듭제곱하며 큰 값에서 포화시킨다.
     *
     * @param base the number / 수
     * @return the power / 거듭제곱
     */
    private long power(long base) {
        long p = 1;
        // 곱할 때마다 한계를 넘는지 확인한다.
        for (int i = 0; i < dimensions; i++) {
            p *= base;
            if (p > (1L << 40)) {
                return 1L << 40;
            }
        }
        return p;
    }

    /**
     * Converts the 32-bit float format of the Vorbis specification to a float.
     * <p>
     * Vorbis 규격의 32비트 부동소수점 형식을 float 로 바꾼다.
     *
     * @param x the 32 bits / 32비트
     * @return the value / 값
     */
    static float unpack(int x) {
        long v = x & 0xFFFFFFFFL;
        double mantissa = v & 0x1FFFFF;
        int exponent = (int) ((v & 0x7FE00000L) >> 21);
        if ((v & 0x80000000L) != 0) {
            mantissa = -mantissa;
        }
        return (float) Math.scalb(mantissa, exponent - 788);
    }

    /**
     * Decodes one entry number.
     * <p>
     * 항목 번호 하나를 디코딩한다.
     *
     * @param br packet bits / 패킷 비트
     * @return the entry, or -1 if the packet ended or the bits do not form a code / 항목. 패킷이 끝났거나 부호가 아니면 -1
     */
    int decodeScalar(VorbisBitReader br) {
        int node = 0;
        // 비트를 하나씩 읽으며 잎에 닿을 때까지 트리를 따라간다.
        for (int i = 0; i < 33; i++) {
            int bit = br.read(1);
            if (br.isEndOfPacket()) {
                return -1;
            }
            int next = tree[node * 2 + bit];
            if (next < 0) {
                return ~next;
            }
            if (next == 0) {
                return -1;
            }
            node = next;
        }
        return -1;
    }

    /**
     * Tells whether this codebook has a vector lookup.
     * <p>
     * 이 코드북에 벡터 조회가 있는지 알려 준다.
     *
     * @return true if vectors can be decoded / 벡터를 디코딩할 수 있으면 true
     */
    boolean hasLookup() {
        return lookupType != 0;
    }

    /**
     * Decodes a vector.
     * <p>
     * 벡터를 디코딩한다.
     *
     * @param br  packet bits / 패킷 비트
     * @param out receives <code>dimensions</code> values / <code>dimensions</code> 개의 값을 받는다
     * @param off first index in <code>out</code> / <code>out</code> 의 첫 위치
     * @return false if the packet ended, the code is invalid or the book has no lookup / 패킷이 끝났거나 부호가 올바르지 않거나 조회가 없으면 false
     */
    boolean decodeVector(VorbisBitReader br, float[] out, int off) {
        int e = decodeScalar(br);
        if (e < 0 || lookupType == 0) {
            return false;
        }
        float last = 0;
        int idx = e;
        // 차원마다 곱셈 계수를 골라 값을 만든다(sequence 면 이전 값을 더한다).
        for (int i = 0; i < dimensions; i++) {
            int m;
            if (lookupType == 1) {
                m = multiplicands[idx % lookupValues];
                idx /= lookupValues;
            } else {
                m = multiplicands[e * dimensions + i];
            }
            float v = m * delta + minimum + last;
            out[off + i] = v;
            if (sequence) {
                last = v;
            }
        }
        return true;
    }
}
