package org.duckdns.hjow.swingwebpanel.media.audio.mp3;

/**
 * Decoder of MPEG-1/2/2.5 Layer III frames (ISO/IEC 11172-3, 13818-3): side information, bit reservoir, scale factors, Huffman decoding,
 * requantization, stereo processing (MS and intensity), reordering, alias reduction, IMDCT with window switching and the polyphase synthesis
 * filter bank. One instance keeps the state of one stream (bit reservoir, overlap buffers, synthesis vectors). Not thread-safe.
 * The structure follows the clear reference decoders go-mp3 (Apache License 2.0, Copyright 2017 Hajime Hoshi, derived from PDMP3) and
 * minimp3 (CC0); the algorithms are those of the standard.
 * <p>
 * MPEG-1/2/2.5 Layer III 프레임 디코더(ISO/IEC 11172-3, 13818-3): 부가 정보, 비트 저장소, 스케일팩터, 허프만 해독, 재양자화, 스테레오 처리
 * (MS, 인텐시티), 재정렬, 얼라이어스 감소, 윈도 전환 IMDCT, 다상 합성 필터 뱅크. 인스턴스 하나가 스트림 하나의 상태(비트 저장소, 겹침 버퍼,
 * 합성 벡터)를 가진다. 스레드 안전하지 않다. 구조는 명확한 참조 디코더 go-mp3(Apache License 2.0, PDMP3 에서 파생)와 minimp3(CC0)를 따르고
 * 알고리즘은 표준의 것이다.
 */
final class Mp3FrameDecoder {

    /**
     * A frame that cannot be decoded (damaged data); the caller replaces it by silence. / 해독할 수 없는 프레임(손상된 데이터). 호출자가 무음으로 대체한다.
     */
    static final class FormatException extends Exception {
        /** Serialization version id. / 직렬화 버전 식별자. */
        private static final long serialVersionUID = 1L;

        /**
         * Creates the exception. / 예외를 만든다.
         *
         * @param message description / 설명
         */
        FormatException(String message) {
            super(message);
        }
    }

    /** Side information of one granule and channel. / 그래뉼 하나, 채널 하나의 부가 정보. */
    private static final class Gr {
        /** part2_3_length: bits of scale factors and Huffman data. / 스케일팩터와 허프만 데이터의 비트 수. */
        int part23;
        /** big_values: number of Huffman pairs. / 큰 값 쌍의 수. */
        int bigValues;
        /** global_gain. / 전체 이득. */
        int globalGain;
        /** scalefac_compress. / 스케일팩터 압축 코드. */
        int scalefacCompress;
        /** block_type (0 normal, 1 start, 2 short, 3 stop). / 블록 종류. */
        int blockType;
        /** mixed_block_flag. / 혼합 블록 표시. */
        boolean mixed;
        /** table_select of the three regions. / 영역 3개의 허프만 표 번호. */
        final int[] table = new int[3];
        /** subblock_gain of the three windows. / 창 3개의 서브블록 이득. */
        final int[] subblockGain = new int[3];
        /** region0_count. / 영역 0 의 스케일팩터 대역 수. */
        int region0;
        /** region1_count. / 영역 1 의 스케일팩터 대역 수. */
        int region1;
        /** Pre-emphasis flag preflag. / 사전 강조 표시 preflag. */
        boolean preflag;
        /** scalefac_scale. / 스케일팩터 배율 비트. */
        int scalefacScale;
        /** count1table_select. / count1 표 선택. */
        int count1Table;
        /** Scale factors by segment (see {@link #segmentCount}). / 구간별 스케일팩터. */
        final int[] sf = new int[64];
        /** True if the scale factor of a segment is the "illegal" all-ones value of a MPEG-2 intensity channel. / MPEG-2 인텐시티 채널의 모든 비트가 1 인 값이면 true. */
        final boolean[] sfIllegal = new boolean[64];
        /** Number of long segments (scale factor bands used as long). / 긴 구간의 수. */
        int longSegments;
        /** Number of short segments (band times three windows). / 짧은 구간의 수(대역 × 창 3개). */
        int shortSegments;
        /** First short scale factor band (0, or 3 for mixed blocks). / 첫 짧은 스케일팩터 대역(혼합은 3). */
        int firstShortBand;
        /** Segment widths. / 구간 폭. */
        int[] widths;

        /**
         * Returns the total number of segments. / 구간의 전체 수를 반환한다.
         *
         * @return count / 개수
         */
        int segmentCount() {
            return longSegments + shortSegments;
        }
    }

    /** x^(4/3) for integers up to 8206 (8191 plus linbits). / 8206 까지 정수의 x^(4/3). */
    private static final float[] POW43 = new float[8207 + 16];
    /** IMDCT basis of the 36-point transform. / 36점 변환의 IMDCT 기저. */
    private static final float[][] COS36 = new float[18][36];
    /** IMDCT basis of the 12-point transform. / 12점 변환의 IMDCT 기저. */
    private static final float[][] COS12 = new float[6][12];
    /** IMDCT windows for block types 0 to 3. / 블록 종류 0~3 의 IMDCT 윈도. */
    private static final float[][] IMDCT_WINDOW = new float[4][36];
    /** Matrixing coefficients of the synthesis filter bank. / 합성 필터 뱅크의 행렬 계수. */
    private static final float[][] SYNTH_N = new float[64][32];
    /** Alias reduction cosine coefficients. / 얼라이어스 감소 코사인 계수. */
    private static final float[] CS = new float[8];
    /** Alias reduction sine coefficients. / 얼라이어스 감소 사인 계수. */
    private static final float[] CA = new float[8];
    /** Pre-emphasis table pretab. / 사전 강조 표. */
    private static final int[] PRETAB = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 3, 3, 3, 2, 0 };
    /** MPEG-1 slen1 and slen2 by scalefac_compress. / scalefac_compress 값에 따른 MPEG-1 slen1 과 slen2 표. */
    private static final int[][] SLEN = { { 0, 0 }, { 0, 1 }, { 0, 2 }, { 0, 3 }, { 3, 0 }, { 1, 1 }, { 1, 2 }, { 1, 3 }, { 2, 1 }, { 2, 2 }, { 2, 3 },
            { 3, 1 }, { 3, 2 }, { 3, 3 }, { 4, 2 }, { 4, 3 } };
    /** MPEG-2 scale factor partition sizes [block class][table][partition]. / MPEG-2 스케일팩터 분할 크기. */
    private static final int[][][] LSF_PARTITION = {
            { { 6, 5, 5, 5 }, { 6, 5, 7, 3 }, { 11, 10, 0, 0 }, { 7, 7, 7, 0 }, { 6, 6, 6, 3 }, { 8, 8, 5, 0 } },
            { { 9, 9, 9, 9 }, { 9, 9, 12, 6 }, { 18, 18, 0, 0 }, { 12, 12, 12, 0 }, { 12, 9, 9, 6 }, { 15, 12, 9, 0 } },
            { { 6, 9, 9, 9 }, { 6, 9, 12, 6 }, { 15, 18, 0, 0 }, { 6, 15, 12, 0 }, { 6, 12, 9, 6 }, { 6, 18, 9, 0 } } };

    static {
        // 4/3 거듭제곱 표를 만든다.
        for (int i = 0; i < POW43.length; i++) {
            POW43[i] = (float) Math.pow(i, 4.0 / 3.0);
        }
        // 36점과 12점 IMDCT 기저를 만든다.
        for (int i = 0; i < 18; i++) {
            for (int j = 0; j < 36; j++) {
                COS36[i][j] = (float) Math.cos(Math.PI / 72 * (2 * j + 1 + 18) * (2 * i + 1));
            }
        }
        for (int i = 0; i < 6; i++) { // 12점 IMDCT 기저의 행
            for (int j = 0; j < 12; j++) { // 12점 IMDCT 기저의 열
                COS12[i][j] = (float) Math.cos(Math.PI / 24 * (2 * j + 1 + 6) * (2 * i + 1));
            }
        }
        // 블록 종류별 윈도를 만든다.
        for (int i = 0; i < 36; i++) {
            IMDCT_WINDOW[0][i] = (float) Math.sin(Math.PI / 36 * (i + 0.5));
        }
        for (int i = 0; i < 18; i++) { // 긴 윈도 앞부분의 사인 값
            IMDCT_WINDOW[1][i] = (float) Math.sin(Math.PI / 36 * (i + 0.5));
        }
        for (int i = 18; i < 24; i++) { // 시작 윈도의 평평한 구간
            IMDCT_WINDOW[1][i] = 1.0f;
        }
        for (int i = 24; i < 30; i++) { // 시작 윈도의 짧은 감쇠 구간
            IMDCT_WINDOW[1][i] = (float) Math.sin(Math.PI / 12 * (i + 0.5 - 18.0));
        }
        for (int i = 0; i < 12; i++) { // 짧은 윈도의 사인 값
            IMDCT_WINDOW[2][i] = (float) Math.sin(Math.PI / 12 * (i + 0.5));
        }
        for (int i = 6; i < 12; i++) { // 정지 윈도의 짧은 상승 구간
            IMDCT_WINDOW[3][i] = (float) Math.sin(Math.PI / 12 * (i + 0.5 - 6.0));
        }
        for (int i = 12; i < 18; i++) { // 정지 윈도의 평평한 구간
            IMDCT_WINDOW[3][i] = 1.0f;
        }
        for (int i = 18; i < 36; i++) { // 정지 윈도의 긴 감쇠 구간
            IMDCT_WINDOW[3][i] = (float) Math.sin(Math.PI / 36 * (i + 0.5));
        }
        // 합성 필터 뱅크의 행렬 계수를 만든다.
        for (int i = 0; i < 64; i++) {
            for (int j = 0; j < 32; j++) {
                SYNTH_N[i][j] = (float) Math.cos((16 + i) * (2 * j + 1) * (Math.PI / 64.0));
            }
        }
        // 얼라이어스 감소 계수를 표준의 c[i] 에서 계산한다.
        double[] c = { -0.6, -0.535, -0.33, -0.185, -0.095, -0.041, -0.0142, -0.0037 };
        for (int i = 0; i < 8; i++) {
            double sq = Math.sqrt(1.0 + c[i] * c[i]);
            CS[i] = (float) (1.0 / sq);
            CA[i] = (float) (c[i] / sq);
        }
    }

    /** Maximum number of bytes of the bit reservoir (MPEG-1). / 비트 저장소의 최대 바이트 수(MPEG-1). */
    private static final int MAX_RESERVOIR = 511;

    /** Overlap buffers [channel][sub band][18]. / 겹침 버퍼. */
    private final float[][][] overlap = new float[2][32][18];
    /** Synthesis vectors V [channel][1024]. / 합성 벡터 V. */
    private final float[][] vvec = new float[2][1024];
    /** Tail of the main data of earlier frames. / 앞선 프레임들의 주 데이터 끝부분. */
    private byte[] reservoir = new byte[0];
    /** Dequantized spectra [granule][channel][576]. / 역양자화된 스펙트럼. */
    private final float[][][] xr = new float[2][2][576];
    /** Integer values read by the Huffman decoder. / 허프만 해독기가 읽은 정수 값. */
    private final int[] ival = new int[576 + 8];
    /** Scratch buffer for reordering. / 재정렬용 임시 버퍼. */
    private final float[] tmp = new float[576];
    /** Scratch vector of the subband samples. / 서브밴드 샘플 임시 벡터. */
    private final float[] svec = new float[32];
    /** Scratch vector U of the synthesis filter. / 합성 필터의 임시 벡터 U. */
    private final float[] uvec = new float[512];
    /** Scratch input of the IMDCT. / IMDCT 임시 입력. */
    private final float[] imdctIn = new float[18];
    /** Scratch output of the IMDCT. / IMDCT 임시 출력. */
    private final float[] imdctOut = new float[36];

    /**
     * Forgets the bit reservoir, the overlap and the filter state (call after a seek). / 비트 저장소, 겹침, 필터 상태를 버린다(탐색 뒤에 호출).
     */
    void reset() {
        reservoir = new byte[0];
        for (int ch = 0; ch < 2; ch++) { // 채널마다 상태를 지운다
            java.util.Arrays.fill(vvec[ch], 0f);
            for (int sb = 0; sb < 32; sb++) { // 서브밴드마다 겹침 버퍼를 지운다
                java.util.Arrays.fill(overlap[ch][sb], 0f);
            }
        }
    }

    /**
     * Decodes one frame into floating-point samples in the range -1 to 1.
     * <p>
     * 프레임 하나를 -1~1 범위의 부동소수점 샘플로 해독한다.
     *
     * @param d      data / 데이터
     * @param off    position of the frame header / 프레임 헤더 위치
     * @param h      parsed header / 해석된 헤더
     * @param pcm    output [channel][sample], at least 576 times the number of granules per channel / 출력 [채널][샘플]
     * @return false if the frame could not be decoded because the bit reservoir lacked data (silence is written) / 비트 저장소의 데이터가 모자라 해독하지 못하면 false(무음을 쓴다)
     * @throws FormatException if the frame is damaged / 프레임이 손상되었을 때
     */
    boolean decode(byte[] d, int off, Mp3Header h, float[][] pcm) throws FormatException {
        int nch = h.channels();
        int ngr = h.granules();
        boolean mpeg1 = h.version == 1;
        int end = Math.min(d.length, off + h.frameBytes);
        int p = off + 4 + (h.crc ? 2 : 0);
        int sideLen = h.sideInfoBytes();
        if (p + sideLen > end) {
            throw new FormatException("truncated frame");
        }
        Mp3Bits si = new Mp3Bits(d, p, sideLen);
        int mainDataBegin = si.bits(mpeg1 ? 9 : 8);
        si.bits(mpeg1 ? (nch == 1 ? 5 : 3) : (nch == 1 ? 1 : 2));
        int[][] scfsi = new int[2][4];
        if (mpeg1) {
            // 채널마다 스케일팩터 공유 정보 4비트를 읽는다.
            for (int ch = 0; ch < nch; ch++) {
                for (int b = 0; b < 4; b++) {
                    scfsi[ch][b] = si.bit();
                }
            }
        }
        Gr[][] g = new Gr[2][2];
        // 그래뉼과 채널마다 부가 정보를 읽는다.
        for (int gr = 0; gr < ngr; gr++) {
            for (int ch = 0; ch < nch; ch++) {
                Gr x = new Gr();
                g[gr][ch] = x;
                x.part23 = si.bits(12);
                x.bigValues = si.bits(9);
                x.globalGain = si.bits(8);
                x.scalefacCompress = si.bits(mpeg1 ? 4 : 9);
                if (si.bit() == 1) {
                    x.blockType = si.bits(2);
                    x.mixed = si.bit() == 1;
                    x.table[0] = si.bits(5);
                    x.table[1] = si.bits(5);
                    for (int w = 0; w < 3; w++) { // 서브블록 이득 3개를 읽는다
                        x.subblockGain[w] = si.bits(3);
                    }
                    if (x.blockType == 0) {
                        throw new FormatException("invalid block type 0 with window switching");
                    }
                    x.region0 = (x.blockType == 2 && !x.mixed) ? 8 : 7;
                    x.region1 = 20 - x.region0;
                } else {
                    for (int r = 0; r < 3; r++) { // 영역 3개의 표 번호를 읽는다
                        x.table[r] = si.bits(5);
                    }
                    x.region0 = si.bits(4);
                    x.region1 = si.bits(3);
                }
                x.preflag = mpeg1 && si.bit() == 1;
                x.scalefacScale = si.bit();
                x.count1Table = si.bit();
                if (x.bigValues > 288) {
                    throw new FormatException("big_values too large");
                }
            }
        }
        // 이전 프레임들의 주 데이터와 이 프레임의 주 데이터를 이어 붙인다.
        int mainStart = p + sideLen;
        int mainLen = Math.max(0, end - mainStart);
        byte[] all = new byte[reservoir.length + mainLen];
        System.arraycopy(reservoir, 0, all, 0, reservoir.length);
        System.arraycopy(d, mainStart, all, reservoir.length, mainLen);
        int startIdx = reservoir.length - mainDataBegin;
        int keep = Math.min(MAX_RESERVOIR, all.length);
        byte[] newReservoir = new byte[keep];
        System.arraycopy(all, all.length - keep, newReservoir, 0, keep);
        reservoir = newReservoir;
        if (startIdx < 0) {
            // 비트 저장소에 필요한 데이터가 없으면 이 프레임은 무음으로 처리한다.
            for (int ch = 0; ch < nch; ch++) {
                java.util.Arrays.fill(pcm[ch], 0, 576 * ngr, 0f);
            }
            return false;
        }
        Mp3Bits md = new Mp3Bits(all, startIdx, all.length - startIdx);
        int sr = bandLayout(h);
        // 그래뉼과 채널마다 스케일팩터와 허프만 데이터를 읽어 역양자화한다.
        for (int gr = 0; gr < ngr; gr++) {
            for (int ch = 0; ch < nch; ch++) {
                Gr x = g[gr][ch];
                int part2Start = md.position();
                setupSegments(x, sr, h);
                if (mpeg1) {
                    readScalefactorsMpeg1(md, x, scfsi[ch], gr == 1 ? g[0][ch] : null);
                } else {
                    readScalefactorsMpeg2(md, x, ch == 1 && (h.modeExtension & 1) != 0 && h.mode == 1);
                }
                int partEnd = part2Start + x.part23;
                huffman(md, x, sr, partEnd, h);
                md.seek(partEnd);
                dequantize(x, xr[gr][ch]);
            }
        }
        // 그래뉼마다 스테레오 처리 이후의 변환을 한다.
        for (int gr = 0; gr < ngr; gr++) {
            if (nch == 2) {
                stereo(h, g[gr][0], g[gr][1], xr[gr][0], xr[gr][1]);
            }
            for (int ch = 0; ch < nch; ch++) { // 채널마다 부호를 0 으로 채운다
                Gr x = g[gr][ch];
                float[] spec = xr[gr][ch];
                if (x.blockType == 2) {
                    reorder(x, spec);
                }
                antialias(x, spec);
                hybrid(x, spec, ch);
                synthesize(spec, ch, pcm[ch], gr * 576);
            }
        }
        return true;
    }

    /**
     * Returns the index of the scale factor band layout (0 to 7) of the stream.
     * <p>
     * 스트림의 스케일팩터 대역 배치 번호(0~7)를 반환한다.
     *
     * @param h header / 헤더
     * @return layout index / 배치 번호
     */
    private static int bandLayout(Mp3Header h) {
        int idx;
        if (h.version == 1) {
            idx = h.sampleRate == 44100 ? 5 : (h.sampleRate == 48000 ? 6 : 7);
        } else if (h.version == 2) {
            idx = h.sampleRate == 22050 ? 2 : (h.sampleRate == 24000 ? 3 : 4);
        } else {
            idx = h.sampleRate == 8000 ? 1 : 0;
        }
        return idx;
    }

    /**
     * Chooses the segment layout of a granule from its block type.
     * <p>
     * 블록 종류에 따라 그래뉼의 구간 배치를 정한다.
     *
     * @param x  granule / 그래뉼
     * @param sr band layout index / 대역 배치 번호
     * @param h  header / 헤더
     */
    private static void setupSegments(Gr x, int sr, Mp3Header h) {
        if (x.blockType == 2 && x.mixed) {
            x.widths = Mp3Tables.SFB_MIXED[sr];
            x.longSegments = h.version == 1 ? 8 : 6;
            x.firstShortBand = 3;
        } else if (x.blockType == 2) {
            x.widths = Mp3Tables.SFB_SHORT[sr];
            x.longSegments = 0;
            x.firstShortBand = 0;
        } else {
            x.widths = Mp3Tables.SFB_LONG[sr];
            x.longSegments = 22;
            x.firstShortBand = 0;
        }
        int n = 0;
        // 끝 표시 0 이 나올 때까지 구간 수를 센다.
        while (x.widths[n] != 0) {
            n++;
        }
        x.shortSegments = n - x.longSegments;
    }

    /**
     * Reads the MPEG-1 scale factors of a granule (with scfsi sharing from granule 0).
     * <p>
     * 그래뉼의 MPEG-1 스케일팩터를 읽는다(그래뉼 0 과의 scfsi 공유 포함).
     *
     * @param md    main data / 주 데이터
     * @param x     granule / 그래뉼
     * @param scfsi sharing flags of the channel / 채널의 공유 표시
     * @param prev  granule 0 of the same channel, or null for granule 0 / 같은 채널의 그래뉼 0. 그래뉼 0 이면 null
     */
    private static void readScalefactorsMpeg1(Mp3Bits md, Gr x, int[] scfsi, Gr prev) {
        int slen1 = SLEN[x.scalefacCompress][0];
        int slen2 = SLEN[x.scalefacCompress][1];
        if (x.blockType == 2) {
            int idx = 0;
            // 혼합 블록의 앞쪽 긴 구간은 slen1 로 읽는다.
            for (int i = 0; i < x.longSegments; i++) {
                x.sf[idx++] = md.bits(slen1);
            }
            // 짧은 대역은 대역 6 앞은 slen1, 그 뒤는 slen2 로 창 3개를 차례로 읽는다.
            for (int band = x.firstShortBand; band < 12; band++) {
                int n = band < 6 ? slen1 : slen2;
                for (int w = 0; w < 3; w++) { // 짧은 대역의 창 3개를 차례로 읽는다
                    x.sf[idx++] = md.bits(n);
                }
            }
            return;
        }
        int[] from = { 0, 6, 11, 16 };
        int[] to = { 6, 11, 16, 21 };
        // 4개 그룹마다 새로 읽거나 그래뉼 0 의 값을 복사한다.
        for (int grp = 0; grp < 4; grp++) {
            int n = grp < 2 ? slen1 : slen2;
            for (int i = from[grp]; i < to[grp]; i++) { // 그룹의 스케일팩터 대역을 읽거나 복사한다
                if (prev != null && scfsi[grp] == 1 && prev.blockType != 2) {
                    x.sf[i] = prev.sf[i];
                } else {
                    x.sf[i] = md.bits(n);
                }
            }
        }
    }

    /**
     * Reads the MPEG-2/2.5 (LSF) scale factors of a granule.
     * <p>
     * 그래뉼의 MPEG-2/2.5(LSF) 스케일팩터를 읽는다.
     *
     * @param md       main data / 주 데이터
     * @param x        granule / 그래뉼
     * @param isChannel true for the right channel of an intensity stereo frame / 인텐시티 스테레오 프레임의 오른쪽 채널이면 true
     */
    private static void readScalefactorsMpeg2(Mp3Bits md, Gr x, boolean isChannel) {
        int sfc = x.scalefacCompress;
        int[] slen = new int[4];
        int table;
        if (!isChannel) {
            if (sfc < 400) {
                slen[0] = (sfc >> 4) / 5;
                slen[1] = (sfc >> 4) % 5;
                slen[2] = (sfc & 15) >> 2;
                slen[3] = sfc & 3;
                table = 0;
            } else if (sfc < 500) {
                int s = sfc - 400;
                slen[0] = (s >> 2) / 5;
                slen[1] = (s >> 2) % 5;
                slen[2] = s & 3;
                table = 1;
            } else {
                int s = sfc - 500;
                slen[0] = s / 3;
                slen[1] = s % 3;
                table = 2;
                x.preflag = true;
            }
        } else {
            int s = sfc >> 1;
            if (s < 180) {
                slen[0] = s / 36;
                slen[1] = (s % 36) / 6;
                slen[2] = (s % 36) % 6;
                table = 3;
            } else if (s < 244) {
                s -= 180;
                slen[0] = (s & 63) >> 4;
                slen[1] = (s & 15) >> 2;
                slen[2] = s & 3;
                table = 4;
            } else {
                s -= 244;
                slen[0] = s / 3;
                slen[1] = s % 3;
                table = 5;
            }
        }
        int cls = x.blockType == 2 ? (x.mixed ? 2 : 1) : 0;
        int idx = 0;
        // 분할 4개마다 지정된 개수의 스케일팩터를 읽는다.
        for (int part = 0; part < 4; part++) {
            int count = LSF_PARTITION[cls][table][part];
            for (int i = 0; i < count && idx < 63; i++) { // 분할의 스케일팩터를 차례로 읽는다
                int v = md.bits(slen[part]);
                x.sf[idx] = v;
                x.sfIllegal[idx] = isChannel && slen[part] > 0 && v == (1 << slen[part]) - 1;
                idx++;
            }
        }
    }

    /**
     * Decodes the Huffman coded spectrum of a granule into integer values.
     * <p>
     * 그래뉼의 허프만 부호화된 스펙트럼을 정수 값으로 해독한다.
     *
     * @param md      main data / 주 데이터
     * @param x       granule / 그래뉼
     * @param sr      band layout / 대역 배치
     * @param partEnd bit position where the data of this granule ends / 이 그래뉼의 데이터가 끝나는 비트 위치
     * @param h       header / 헤더
     * @throws FormatException on an invalid code / 올바르지 않은 부호일 때
     */
    private void huffman(Mp3Bits md, Gr x, int sr, int partEnd, Mp3Header h) throws FormatException {
        java.util.Arrays.fill(ival, 0);
        int region1Start;
        int region2Start;
        if (x.blockType == 2) {
            // 짧은 블록은 앞쪽 짧은 대역 3개(창 3개씩)가 영역 0 이다. 대부분의 표본화 주파수에서 36 선이지만 MPEG-2.5 8 kHz 는 대역 폭이 8 이라 72 선이다.
            region1Start = x.mixed ? 36 : 3 * (x.widths[0] + x.widths[3] + x.widths[6]);
            region2Start = 576;
        } else {
            int[] cum = cumulative(Mp3Tables.SFB_LONG[sr]);
            int i1 = x.region0 + 1;
            int i2 = x.region0 + x.region1 + 2;
            region1Start = cum[Math.min(i1, 22)];
            region2Start = cum[Math.min(i2, 22)];
        }
        int pos = 0;
        int limit = x.bigValues * 2;
        // 큰 값 영역을 쌍 단위로 해독한다.
        while (pos < limit) {
            int tbl = pos < region1Start ? x.table[0] : (pos < region2Start ? x.table[1] : x.table[2]);
            decodePair(md, tbl, pos);
            pos += 2;
            if (md.overrun()) {
                throw new FormatException("huffman data overrun");
            }
        }
        // count1 영역을 4개 묶음 단위로 해독한다.
        int tbl = 32 + x.count1Table;
        while (pos <= 572 && md.position() < partEnd) {
            int before = md.position();
            int[] q = decodeQuad(md, tbl);
            if (md.position() > partEnd) {
                md.seek(before);
                break;
            }
            ival[pos] = q[0];
            ival[pos + 1] = q[1];
            ival[pos + 2] = q[2];
            ival[pos + 3] = q[3];
            pos += 4;
        }
    }

    /**
     * Computes cumulative band start positions from widths.
     * <p>
     * 폭에서 누적 대역 시작 위치를 계산한다.
     *
     * @param widths widths terminated by 0 / 0 으로 끝나는 폭
     * @return starts, one more entry than bands / 시작 위치(대역 수보다 항목이 하나 많음)
     */
    private static int[] cumulative(int[] widths) {
        int[] c = new int[widths.length];
        int s = 0;
        // 폭을 차례로 더해 누적 위치를 만든다.
        for (int i = 0; widths[i] != 0; i++) {
            c[i] = s;
            s += widths[i];
            c[i + 1] = s;
        }
        return c;
    }

    /**
     * Decodes one pair of big values with a Huffman table, including the linbits and the sign bits.
     * <p>
     * 큰 값 한 쌍을 허프만 표로 해독한다(linbits와 부호 비트 포함).
     *
     * @param md  main data / 주 데이터
     * @param tbl table number / 표 번호
     * @param pos index of the first value / 첫 값의 위치
     * @throws FormatException on an invalid code / 올바르지 않은 부호일 때
     */
    private void decodePair(Mp3Bits md, int tbl, int pos) throws FormatException {
        int len = Mp3Tables.TREE_LENGTH[tbl];
        if (len == 0) {
            return;
        }
        int v = walkTree(md, tbl);
        int xv = (v >> 4) & 15;
        int yv = v & 15;
        int lin = Mp3Tables.LINBITS[tbl];
        if (lin != 0 && xv == 15) {
            xv += md.bits(lin);
        }
        if (xv != 0 && md.bit() == 1) {
            xv = -xv;
        }
        if (lin != 0 && yv == 15) {
            yv += md.bits(lin);
        }
        if (yv != 0 && md.bit() == 1) {
            yv = -yv;
        }
        ival[pos] = xv;
        ival[pos + 1] = yv;
    }

    /**
     * Decodes one quadruple of the count1 region.
     * <p>
     * count1 영역의 4개 묶음 하나를 해독한다.
     *
     * @param md  main data / 주 데이터
     * @param tbl table number (32 or 33) / 표 번호(32 또는 33)
     * @return the four values v, w, x, y / 값 4개 v, w, x, y
     * @throws FormatException on an invalid code / 올바르지 않은 부호일 때
     */
    private int[] decodeQuad(Mp3Bits md, int tbl) throws FormatException {
        int y = walkTree(md, tbl) & 15;
        int[] q = new int[4];
        q[0] = (y >> 3) & 1;
        q[1] = (y >> 2) & 1;
        q[2] = (y >> 1) & 1;
        q[3] = y & 1;
        // 값이 0 이 아닌 항목마다 부호 비트를 읽는다.
        for (int i = 0; i < 4; i++) {
            if (q[i] != 0 && md.bit() == 1) {
                q[i] = -q[i];
            }
        }
        return q;
    }

    /**
     * Walks a Huffman tree bit by bit and returns the leaf value.
     * <p>
     * 허프만 트리를 비트 단위로 따라가 잎의 값을 반환한다.
     *
     * @param md  main data / 주 데이터
     * @param tbl table number / 표 번호
     * @return leaf value / 잎의 값
     * @throws FormatException if no leaf is reached / 잎에 도달하지 못하면
     */
    private static int walkTree(Mp3Bits md, int tbl) throws FormatException {
        char[] t = Mp3Tables.HUFFMAN_TREE;
        int base = Mp3Tables.TREE_OFFSET[tbl];
        int treeLen = Mp3Tables.TREE_LENGTH[tbl];
        int point = 0;
        // 최대 32비트까지 트리를 따라 내려간다.
        for (int bitsLeft = 32; bitsLeft > 0; bitsLeft--) {
            int word = t[base + point] & 0xFFFF;
            if ((word & 0xFF00) == 0) {
                return word;
            }
            if (md.bit() != 0) {
                // 오른쪽 자식으로 간다(250 이상이면 연쇄 점프).
                while ((t[base + point] & 0xFF) >= 250) {
                    point += t[base + point] & 0xFF;
                }
                point += t[base + point] & 0xFF;
            } else {
                while (((t[base + point] & 0xFFFF) >> 8) >= 250) { // 연쇄 점프를 따라간다
                    point += (t[base + point] & 0xFFFF) >> 8;
                }
                point += (t[base + point] & 0xFFFF) >> 8;
            }
            if (point >= treeLen || point < 0) {
                break;
            }
        }
        throw new FormatException("illegal Huffman code");
    }

    /**
     * Requantizes the integer values of a granule into spectral lines.
     * <p>
     * 그래뉼의 정수 값을 스펙트럼 선으로 재양자화한다.
     *
     * @param x   granule / 그래뉼
     * @param out output spectrum (576) / 출력 스펙트럼(576)
     */
    private void dequantize(Gr x, float[] out) {
        double mult = x.scalefacScale != 0 ? 1.0 : 0.5;
        int pos = 0;
        int segs = x.segmentCount();
        // 구간마다 이득을 계산해 해당 구간의 선들을 재양자화한다.
        for (int s = 0; s < segs; s++) {
            int width = x.widths[s];
            boolean isLong = s < x.longSegments;
            double exp;
            if (isLong) {
                int bandIdx = s;
                int pre = x.preflag && bandIdx < PRETAB.length ? PRETAB[bandIdx] : 0;
                exp = (x.globalGain - 210) / 4.0 - mult * (x.sf[s] + pre);
            } else {
                int w = (s - x.longSegments) % 3;
                exp = (x.globalGain - 210 - 8.0 * x.subblockGain[w]) / 4.0 - mult * x.sf[s];
            }
            float scale = (float) Math.pow(2.0, exp);
            for (int i = 0; i < width && pos < 576; i++, pos++) { // 구간의 선을 재양자화한다
                int v = ival[pos];
                if (v == 0) {
                    out[pos] = 0f;
                } else if (v > 0) {
                    out[pos] = POW43[v] * scale;
                } else {
                    out[pos] = -POW43[-v] * scale;
                }
            }
        }
        // 구간 밖의 나머지 선은 0 이다.
        for (; pos < 576; pos++) {
            out[pos] = 0f;
        }
    }

    /**
     * Applies MS and intensity stereo to the spectra of the two channels of a granule.
     * <p>
     * 그래뉼의 두 채널 스펙트럼에 MS 와 인텐시티 스테레오를 적용한다.
     *
     * @param h  header / 헤더
     * @param gl left granule info / 왼쪽 그래뉼 정보
     * @param gr right granule info / 오른쪽 그래뉼 정보
     * @param l  left spectrum / 왼쪽 스펙트럼
     * @param r  right spectrum / 오른쪽 스펙트럼
     */
    private void stereo(Mp3Header h, Gr gl, Gr gr, float[] l, float[] r) {
        boolean ms = h.mode == 1 && (h.modeExtension & 2) != 0;
        boolean is = h.mode == 1 && (h.modeExtension & 1) != 0;
        if (!ms && !is) {
            return;
        }
        int segs = gr.segmentCount();
        // 인텐시티 스테레오의 시작 대역을 오른쪽 채널의 마지막 0 이 아닌 선에서 찾는다.
        int[] lastBand = { -1, -1, -1 };
        int pos = 0;
        for (int s = 0; is && s < segs; s++) { // 구간마다 오른쪽 채널의 0 이 아닌 선을 찾는다
            int width = gr.widths[s];
            boolean any = false;
            for (int i = 0; i < width && pos + i < 576; i++) { // 구간 안의 선을 검사한다
                if (r[pos + i] != 0f) {
                    any = true;
                    break;
                }
            }
            if (any) {
                if (s < gr.longSegments) {
                    lastBand[0] = Math.max(lastBand[0], s);
                    lastBand[1] = lastBand[0];
                    lastBand[2] = lastBand[0];
                } else {
                    int w = (s - gr.longSegments) % 3;
                    lastBand[w] = Math.max(lastBand[w], s);
                }
            }
            pos += width;
        }
        double i0 = ((gr.scalefacCompress & 1) != 0) ? Math.pow(2.0, -0.5) : Math.pow(2.0, -0.25);
        final float invSqrt2 = (float) (Math.sqrt(2.0) / 2.0);
        pos = 0;
        // 구간마다 인텐시티 대역이면 비율을 곱하고 아니면 MS 를 적용한다.
        for (int s = 0; s < segs; s++) {
            int width = gr.widths[s];
            boolean isLong = s < gr.longSegments;
            int w = isLong ? 0 : (s - gr.longSegments) % 3;
            boolean intensity = false;
            int srcSeg = s;
            if (is) {
                int bound = isLong ? lastBand[0] : lastBand[w];
                // 마지막 대역은 스케일팩터가 전송되지 않으므로 바로 앞 대역의 위치를 쓴다.
                if (isLong && s == 21) {
                    srcSeg = 20;
                } else if (!isLong && gr.firstShortBand + (s - gr.longSegments) / 3 == 12) {
                    srcSeg = s - 3;
                }
                if (srcSeg > bound) {
                    intensity = !gr.sfIllegal[srcSeg] && gr.sf[srcSeg] < (h.version == 1 ? 7 : 64);
                }
            }
            if (intensity) {
                int ipos = gr.sf[srcSeg];
                float kl;
                float kr;
                if (h.version == 1) {
                    if (ipos == 6) {
                        kl = 1f;
                        kr = 0f;
                    } else {
                        double t = Math.tan(ipos * Math.PI / 12.0);
                        kl = (float) (t / (1.0 + t));
                        kr = (float) (1.0 / (1.0 + t));
                    }
                } else if ((ipos & 1) != 0) {
                    kl = (float) Math.pow(i0, (ipos + 1) / 2);
                    kr = 1f;
                } else {
                    kl = 1f;
                    kr = (float) Math.pow(i0, ipos / 2);
                }
                for (int i = 0; i < width && pos + i < 576; i++) { // 인텐시티 비율을 곱한다
                    float v = l[pos + i];
                    l[pos + i] = v * kl;
                    r[pos + i] = v * kr;
                }
            } else if (ms) {
                for (int i = 0; i < width && pos + i < 576; i++) { // MS 변환을 적용한다
                    float a = l[pos + i];
                    float b = r[pos + i];
                    l[pos + i] = (a + b) * invSqrt2;
                    r[pos + i] = (a - b) * invSqrt2;
                }
            }
            pos += width;
        }
    }

    /**
     * Reorders the lines of the short blocks of a granule from band-window order to window-interleaved order.
     * <p>
     * 그래뉼의 짧은 블록 선들을 대역-창 순서에서 창이 교차하는 순서로 재정렬한다.
     *
     * @param x    granule / 그래뉼
     * @param spec spectrum (changed in place) / 스펙트럼(제자리에서 바뀜)
     */
    private void reorder(Gr x, float[] spec) {
        int pos = 0;
        int segs = x.segmentCount();
        // 긴 구간은 그대로 지나간다.
        for (int s = 0; s < x.longSegments; s++) {
            pos += x.widths[s];
        }
        // 짧은 대역마다 창 3개의 선을 교차시켜 임시 버퍼에 넣었다가 되돌려 쓴다.
        for (int s = x.longSegments; s + 2 < segs; s += 3) {
            int width = x.widths[s];
            for (int j = 0; j < width; j++) { // 창 3개의 선을 교차시킨다
                tmp[3 * j] = spec[pos + j];
                tmp[3 * j + 1] = spec[pos + width + j];
                tmp[3 * j + 2] = spec[pos + 2 * width + j];
            }
            System.arraycopy(tmp, 0, spec, pos, 3 * width);
            pos += 3 * width;
        }
    }

    /**
     * Applies the alias reduction butterflies between adjacent sub bands (not used for pure short blocks).
     * <p>
     * 인접 서브밴드 사이에 얼라이어스 감소 버터플라이를 적용한다(순수 짧은 블록에는 쓰지 않는다).
     *
     * @param x    granule / 그래뉼
     * @param spec spectrum / 스펙트럼
     */
    private static void antialias(Gr x, float[] spec) {
        if (x.blockType == 2 && !x.mixed) {
            return;
        }
        int limit = (x.blockType == 2 && x.mixed) ? 2 : 32;
        // 서브밴드 경계마다 8쌍에 버터플라이를 적용한다.
        for (int sb = 1; sb < limit; sb++) {
            for (int i = 0; i < 8; i++) {
                int li = 18 * sb - 1 - i;
                int ui = 18 * sb + i;
                float lo = spec[li];
                float up = spec[ui];
                spec[li] = lo * CS[i] - up * CA[i];
                spec[ui] = up * CS[i] + lo * CA[i];
            }
        }
    }

    /**
     * IMDCT with windowing and overlap-add of the 32 sub bands, followed by the frequency inversion.
     * <p>
     * 서브밴드 32개의 윈도를 곱한 IMDCT 와 겹쳐 더하기, 이어서 주파수 반전.
     *
     * @param x    granule / 그래뉼
     * @param spec spectrum, replaced by the sub band samples / 스펙트럼. 서브밴드 샘플로 바뀐다
     * @param ch   channel / 채널
     */
    private void hybrid(Gr x, float[] spec, int ch) {
        // 서브밴드마다 18개 입력을 36개 출력으로 변환한 뒤 앞 블록의 뒷부분과 더한다.
        for (int sb = 0; sb < 32; sb++) {
            int bt = x.blockType;
            if (x.mixed && x.blockType == 2 && sb < 2) {
                bt = 0;
            }
            for (int i = 0; i < 18; i++) { // 서브밴드 입력을 복사한다
                imdctIn[i] = spec[sb * 18 + i];
            }
            java.util.Arrays.fill(imdctOut, 0f);
            if (bt == 2) {
                float[] win = IMDCT_WINDOW[2];
                for (int w = 0; w < 3; w++) { // 창마다 변환한다
                    for (int p = 0; p < 12; p++) { // 출력 샘플마다 합을 구한다
                        float sum = 0f;
                        for (int m = 0; m < 6; m++) { // 입력 계수에 대해 합한다
                            sum += imdctIn[w + 3 * m] * COS12[m][p];
                        }
                        imdctOut[6 * w + p + 6] += sum * win[p];
                    }
                }
            } else {
                float[] win = IMDCT_WINDOW[bt];
                for (int p = 0; p < 36; p++) { // 출력 샘플마다 합을 구한다
                    float sum = 0f;
                    for (int m = 0; m < 18; m++) { // 입력 계수에 대해 합한다
                        sum += imdctIn[m] * COS36[m][p];
                    }
                    imdctOut[p] = sum * win[p];
                }
            }
            float[] ov = overlap[ch][sb];
            for (int i = 0; i < 18; i++) { // 겹쳐 더하고 다음 블록용으로 저장한다
                spec[sb * 18 + i] = imdctOut[i] + ov[i];
                ov[i] = imdctOut[i + 18];
            }
        }
        // 홀수 서브밴드의 홀수 번째 샘플의 부호를 뒤집는다.
        for (int sb = 1; sb < 32; sb += 2) {
            for (int i = 1; i < 18; i += 2) {
                spec[sb * 18 + i] = -spec[sb * 18 + i];
            }
        }
    }

    /**
     * Runs the polyphase synthesis filter bank over the 18 time slots of a granule.
     * <p>
     * 그래뉼의 시간 슬롯 18개에 다상 합성 필터 뱅크를 적용한다.
     *
     * @param spec sub band samples [sub band * 18 + slot] / 서브밴드 샘플
     * @param ch   channel / 채널
     * @param out  output buffer / 출력 버퍼
     * @param base first output index / 첫 출력 위치
     */
    private void synthesize(float[] spec, int ch, float[] out, int base) {
        float[] v = vvec[ch];
        float[] win = Mp3Tables.SYNTH_WINDOW;
        // 시간 슬롯마다 V 벡터를 밀고 새로 계산한 뒤 32개 PCM 샘플을 만든다.
        for (int ss = 0; ss < 18; ss++) {
            System.arraycopy(v, 0, v, 64, 1024 - 64);
            for (int j = 0; j < 32; j++) { // 서브밴드 샘플을 모은다
                svec[j] = spec[j * 18 + ss];
            }
            for (int i = 0; i < 64; i++) { // 행렬 곱으로 V 벡터를 계산한다
                float sum = 0f;
                float[] row = SYNTH_N[i];
                for (int j = 0; j < 32; j++) { // 서브밴드에 대해 합한다
                    sum += row[j] * svec[j];
                }
                v[i] = sum;
            }
            for (int i = 0; i < 512; i += 64) { // U 벡터를 만든다
                System.arraycopy(v, i << 1, uvec, i, 32);
                System.arraycopy(v, (i << 1) + 96, uvec, i + 32, 32);
            }
            for (int i = 0; i < 512; i++) { // 합성 윈도를 곱한다
                uvec[i] *= win[i];
            }
            for (int i = 0; i < 32; i++) { // PCM 샘플 32개를 만든다
                float sum = 0f;
                for (int j = 0; j < 512; j += 32) { // U 벡터의 32칸 간격 값을 합한다
                    sum += uvec[j + i];
                }
                out[base + 32 * ss + i] = sum;
            }
        }
    }
}
