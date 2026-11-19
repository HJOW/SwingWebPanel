package org.duckdns.hjow.swingwebpanel.media.audio.vorbis;

import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * Vorbis residue (Vorbis I specification, section 8): the spectrum divided by the floor, coded with vector quantization in up to eight
 * passes over partitions of the spectrum. Types 0 and 1 differ in how the vector of a partition is spread over the partition; type 2
 * interleaves all channels of a submap into one long vector.
 * <p>
 * Vorbis 잔차(Vorbis I 규격 8절): 플로어로 나눈 스펙트럼을 스펙트럼의 파티션에 대해 최대 여덟 번의 패스로 벡터 양자화해 부호화한다. 유형 0 과 1 은
 * 파티션의 벡터를 파티션에 펼치는 방식이 다르고, 유형 2 는 하위 맵의 모든 채널을 하나의 긴 벡터로 인터리브한다.
 */
final class VorbisResidue {

    /**
     * The codebooks of the setup header.
     * <p>
     * 설정 헤더의 코드북.
     */
    private final VorbisCodebook[] books;

    /**
     * Residue type, 0, 1 or 2.
     * <p>
     * 잔차 유형(0, 1, 2).
     */
    private final int type;

    /**
     * First spectrum line that carries residue.
     * <p>
     * 잔차가 있는 첫 스펙트럼 위치.
     */
    private final int begin;

    /**
     * End (exclusive) of the lines that carry residue.
     * <p>
     * 잔차가 있는 위치의 끝(미포함).
     */
    private final int end;

    /**
     * Lines per partition.
     * <p>
     * 파티션당 스펙트럼 위치 수.
     */
    private final int partitionSize;

    /**
     * Number of classifications.
     * <p>
     * 분류 수.
     */
    private final int classifications;

    /**
     * Codebook that codes the classifications.
     * <p>
     * 분류를 부호화하는 코드북.
     */
    private final int classBook;

    /**
     * Codebook of every classification and pass, -1 for none.
     * <p>
     * 분류와 패스마다의 코드북. 없으면 -1.
     */
    private final int[][] passBooks;

    /**
     * Parses a residue configuration.
     * <p>
     * 잔차 설정을 해석한다.
     *
     * @param br    packet bits / 패킷 비트
     * @param books the codebooks / 코드북
     * @param type  residue type / 잔차 유형
     * @throws MediaException CORRUPT for an invalid configuration / 올바르지 않은 설정이면 CORRUPT
     */
    VorbisResidue(VorbisBitReader br, VorbisCodebook[] books, int type) throws MediaException {
        this.books = books;
        this.type = type;
        begin = br.read(24);
        end = br.read(24);
        partitionSize = br.read(24) + 1;
        classifications = br.read(6) + 1;
        classBook = br.read(8);
        if (classBook >= books.length || begin < 0 || end < 0) {
            throw new MediaException(MediaException.Kind.CORRUPT, "invalid Vorbis residue configuration");
        }
        int[] cascade = new int[classifications];
        // 분류마다 어느 패스에서 코드북을 쓰는지(cascade 비트)를 읽는다.
        for (int i = 0; i < classifications; i++) {
            int low = br.read(3);
            int high = br.read(1) != 0 ? br.read(5) : 0;
            cascade[i] = high * 8 + low;
        }
        passBooks = new int[classifications][8];
        // 쓰는 패스마다 코드북 번호를 읽는다.
        for (int i = 0; i < classifications; i++) {
            for (int j = 0; j < 8; j++) {
                if ((cascade[i] & (1 << j)) != 0) {
                    passBooks[i][j] = br.read(8);
                    if (passBooks[i][j] >= books.length) {
                        throw new MediaException(MediaException.Kind.CORRUPT, "invalid Vorbis residue codebook");
                    }
                } else {
                    passBooks[i][j] = -1;
                }
            }
        }
        if (br.isEndOfPacket()) {
            throw new MediaException(MediaException.Kind.CORRUPT, "a Vorbis residue configuration is cut off");
        }
    }

    /**
     * Decodes the residue of the channels of one submap into zero-initialized vectors.
     * <p>
     * 하위 맵의 채널들의 잔차를 0 으로 초기화된 벡터에 디코딩한다.
     *
     * @param br    packet bits / 패킷 비트
     * @param vec   one vector of at least <code>half</code> values per channel / 채널마다 <code>half</code> 개 이상의 값을 담는 벡터
     * @param skip  true for channels without residue / 잔차가 없는 채널이면 true
     * @param count number of channels / 채널 수
     * @param half  half of the block size / 블록 크기의 절반
     */
    void decode(VorbisBitReader br, float[][] vec, boolean[] skip, int count, int half) {
        if (type != 2) {
            decodeVectors(br, vec, skip, count, half);
            return;
        }
        boolean all = true;
        // 모든 채널에 잔차가 없으면 아무것도 읽지 않는다.
        for (int c = 0; c < count; c++) {
            if (!skip[c]) {
                all = false;
            }
        }
        if (all) {
            return;
        }
        float[][] one = new float[1][half * count];
        decodeVectors(br, one, new boolean[1], 1, half * count);
        // 하나의 긴 벡터를 채널별로 풀어 낸다(샘플이 채널 순서로 인터리브되어 있다).
        for (int i = 0; i < half; i++) {
            for (int c = 0; c < count; c++) {
                vec[c][i] = one[0][i * count + c];
            }
        }
    }

    /**
     * The decoding loop of the specification for one or several channels.
     * <p>
     * 채널 하나 또는 여럿에 대한 규격의 디코딩 반복.
     *
     * @param br    packet bits / 패킷 비트
     * @param v     the vectors / 벡터
     * @param skip  true for channels that are not decoded / 디코딩하지 않는 채널이면 true
     * @param count number of vectors / 벡터 수
     * @param size  length of every vector / 벡터마다의 길이
     */
    private void decodeVectors(VorbisBitReader br, float[][] v, boolean[] skip, int count, int size) {
        int limitBegin = Math.min(begin, size);
        int limitEnd = Math.min(end, size);
        int toRead = limitEnd - limitBegin;
        if (toRead <= 0) {
            return;
        }
        VorbisCodebook classBookRef = books[classBook];
        int classWords = classBookRef.dimensions;
        int partitions = toRead / partitionSize;
        if (partitions == 0) {
            return;
        }
        int[][] classes = new int[count][partitions + classWords];
        float[] tmp = new float[1024];
        // 최대 여덟 번의 패스로 파티션마다 벡터를 더해 간다.
        for (int pass = 0; pass < 8; pass++) {
            int pc = 0;
            while (pc < partitions) {
                if (pass == 0) {
                    // 첫 패스에서는 채널마다 분류 번호들을 읽는다(한 부호어에 classWords 개가 들어 있다).
                    for (int j = 0; j < count; j++) {
                        if (skip[j]) {
                            continue;
                        }
                        int temp = classBookRef.decodeScalar(br);
                        if (temp < 0) {
                            return;
                        }
                        // 항목마다 반복해서 처리한다.
                        for (int i = classWords - 1; i >= 0; i--) {
                            classes[j][i + pc] = temp % classifications;
                            temp /= classifications;
                        }
                    }
                }
                // 항목마다 반복해서 처리한다.
                for (int i = 0; i < classWords && pc < partitions; i++) {
                    // 항목마다 반복해서 처리한다.
                    for (int j = 0; j < count; j++) {
                        if (skip[j]) {
                            continue;
                        }
                        int book = passBooks[classes[j][pc]][pass];
                        if (book >= 0) {
                            int offset = limitBegin + pc * partitionSize;
                            if (!decodePartition(br, books[book], v[j], offset, tmp)) {
                                return;
                            }
                        }
                    }
                    pc++;
                }
            }
        }
    }

    /**
     * Decodes one partition with a codebook and adds it to the vector.
     * <p>
     * 코드북으로 파티션 하나를 디코딩해 벡터에 더한다.
     *
     * @param br     packet bits / 패킷 비트
     * @param book   the codebook / 코드북
     * @param out    the vector of the channel / 채널의 벡터
     * @param offset first line of the partition / 파티션의 첫 위치
     * @param tmp    scratch / 작업 공간
     * @return false if the packet ended or the codebook cannot be used / 패킷이 끝났거나 코드북을 쓸 수 없으면 false
     */
    private boolean decodePartition(VorbisBitReader br, VorbisCodebook book, float[] out, int offset, float[] tmp) {
        int dims = book.dimensions;
        if (dims > tmp.length || !book.hasLookup()) {
            return false;
        }
        if (type == 0) {
            if (partitionSize % dims != 0) {
                return false;
            }
            int step = partitionSize / dims;
            // 벡터를 읽을 때마다 값 하나씩을 파티션 안에서 step 간격으로 펼쳐 넣는다.
            for (int i = 0; i < step; i++) {
                if (!book.decodeVector(br, tmp, 0)) {
                    return false;
                }
                // 항목마다 반복해서 처리한다.
                for (int k = 0; k < dims; k++) {
                    out[offset + i + k * step] += tmp[k];
                }
            }
        } else {
            // 파티션을 벡터 길이만큼씩 이어서 채운다.
            for (int i = 0; i < partitionSize; i += dims) {
                if (!book.decodeVector(br, tmp, 0)) {
                    return false;
                }
                // 항목마다 반복해서 처리한다.
                for (int k = 0; k < dims && offset + i + k < out.length; k++) {
                    out[offset + i + k] += tmp[k];
                }
            }
        }
        return true;
    }
}
