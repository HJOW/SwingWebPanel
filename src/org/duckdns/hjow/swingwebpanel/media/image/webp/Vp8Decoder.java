package org.duckdns.hjow.swingwebpanel.media.image.webp;

import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * The decoder of a VP8 key frame, the lossy bitstream of WebP (plan item P18-03; RFC 6386). It reads the frame header, the per-macroblock
 * prediction modes from the first partition and the quantized DCT coefficients from the token partitions, reconstructs every macroblock
 * (intra prediction plus inverse transform) into a {@link Vp8Frame} and finally runs the loop filter over the frame. Only key frames exist
 * in a WebP image, so the inter prediction of VP8 is not part of this class. The decoder is written after the reference decoder of the
 * specification; the byte-exact parts (entropy decoding, transforms, prediction and filters) are tested against transliterations of the
 * code of the specification.
 * <p>
 * VP8 키 프레임, 곧 WebP 손실 비트스트림의 디코더(계획 P18-03, RFC 6386). 프레임 헤더, 첫 파티션의 매크로블록별 예측 모드, 토큰 파티션의
 * 양자화된 DCT 계수를 읽고 모든 매크로블록을 복원(인트라 예측과 역변환)해 {@link Vp8Frame} 에 담은 뒤 프레임 전체에 루프 필터를 적용한다.
 * WebP 이미지에는 키 프레임만 있으므로 VP8 의 인터 예측은 이 클래스에 없다. 디코더는 규격의 참조 디코더를 따라 작성했으며 바이트 단위로 정확해야 하는
 * 부분(엔트로피 디코딩, 변환, 예측, 필터)은 규격 코드를 옮긴 시험으로 확인한다.
 */
final class Vp8Decoder {

    /**
     * Maps a position in the zigzag scan order to the raster position in the 4x4 block (RFC 6386 section 13).
     * <p>
     * 지그재그 주사 순서의 위치를 4x4 블록의 래스터 위치로 바꾼다(RFC 6386 13절).
     */
    private static final int[] ZIGZAG = { 0, 1, 4, 8, 5, 2, 3, 6, 9, 12, 13, 10, 7, 11, 14, 15 };

    /**
     * Maps a position in the scan order to its probability band (RFC 6386 section 13.3).
     * <p>
     * 주사 순서의 위치를 확률 대역으로 바꾼다(RFC 6386 13.3절).
     */
    private static final int[] BANDS = { 0, 1, 2, 3, 6, 4, 5, 6, 6, 6, 6, 6, 6, 6, 6, 7 };

    /**
     * Probabilities of the extra bits of the token category 3, most significant bit first.
     * <p>
     * 토큰 범주 3 의 추가 비트 확률(최상위 비트부터).
     */
    private static final int[] CAT3 = { 173, 148, 140 };

    /**
     * Probabilities of the extra bits of the token category 4.
     * <p>
     * 토큰 범주 4 의 추가 비트 확률.
     */
    private static final int[] CAT4 = { 176, 155, 140, 135 };

    /**
     * Probabilities of the extra bits of the token category 5.
     * <p>
     * 토큰 범주 5 의 추가 비트 확률.
     */
    private static final int[] CAT5 = { 180, 157, 141, 134, 130 };

    /**
     * Probabilities of the extra bits of the token category 6.
     * <p>
     * 토큰 범주 6 의 추가 비트 확률.
     */
    private static final int[] CAT6 = { 254, 254, 243, 230, 196, 177, 153, 140, 133, 130, 129 };

    /**
     * Subblock mode that a macroblock mode implies for the context of its neighbours: DC, V, H, TM.
     * <p>
     * 매크로블록 모드가 이웃의 문맥에 대해 뜻하는 서브블록 모드: DC, V, H, TM.
     */
    private static final int[] IMPLIED_BMODE = { Vp8ModeTables.B_DC_PRED, Vp8ModeTables.B_VE_PRED, Vp8ModeTables.B_HE_PRED,
        Vp8ModeTables.B_TM_PRED };

    /**
     * Number of decoded macroblock columns.
     * <p>
     * 디코딩할 매크로블록 열 수.
     */
    private int mbCols;

    /**
     * Number of decoded macroblock rows.
     * <p>
     * 디코딩할 매크로블록 행 수.
     */
    private int mbRows;

    /**
     * Whether the segment feature is enabled.
     * <p>
     * 세그먼트 기능이 켜졌는지 여부.
     */
    private boolean segmentsEnabled;

    /**
     * Whether the segment id of every macroblock is transmitted.
     * <p>
     * 매크로블록마다 세그먼트 id 가 전송되는지 여부.
     */
    private boolean updateSegmentMap;

    /**
     * Whether the segment values replace (true) or are added to (false) the frame values.
     * <p>
     * 세그먼트 값이 프레임 값을 대체하는지(true) 더해지는지(false) 여부.
     */
    private boolean segmentAbsolute;

    /**
     * Quantizer index (absolute) or delta of each of the 4 segments.
     * <p>
     * 세그먼트 4개의 양자화 색인(절대값) 또는 변화량.
     */
    private final int[] segmentQuant = new int[4];

    /**
     * Loop filter level (absolute) or delta of each of the 4 segments.
     * <p>
     * 세그먼트 4개의 루프 필터 수준(절대값) 또는 변화량.
     */
    private final int[] segmentFilter = new int[4];

    /**
     * Probabilities of the tree that codes the segment id.
     * <p>
     * 세그먼트 id 를 부호화하는 트리의 확률.
     */
    private final int[] segmentTreeProbs = { 255, 255, 255 };

    /**
     * Whether the simple loop filter is used.
     * <p>
     * 단순 루프 필터를 쓰는지 여부.
     */
    private boolean simpleFilter;

    /**
     * Loop filter level of the frame (0 switches the filter off).
     * <p>
     * 프레임의 루프 필터 수준(0 이면 필터를 끈다).
     */
    private int filterLevel;

    /**
     * Sharpness of the loop filter.
     * <p>
     * 루프 필터의 선명도.
     */
    private int sharpness;

    /**
     * Whether the loop filter level is adjusted by reference frame and mode deltas.
     * <p>
     * 루프 필터 수준을 참조 프레임/모드 변화량으로 조정하는지 여부.
     */
    private boolean filterDeltaEnabled;

    /**
     * Loop filter delta by reference frame; only index 0 (intra) matters for a key frame.
     * <p>
     * 참조 프레임별 루프 필터 변화량. 키 프레임에서는 0번(인트라)만 쓴다.
     */
    private final int[] refDelta = new int[4];

    /**
     * Loop filter delta by prediction mode; only index 0 (B_PRED) matters for a key frame.
     * <p>
     * 예측 모드별 루프 필터 변화량. 키 프레임에서는 0번(B_PRED)만 쓴다.
     */
    private final int[] modeDelta = new int[4];

    /**
     * The token probabilities of this frame (a copy of the defaults that the frame header updates).
     * <p>
     * 이 프레임의 토큰 확률(프레임 헤더가 갱신하는 기본값의 복사본).
     */
    private final int[] probs = new int[Vp8CoeffTables.DEFAULT_PROBS.length];

    /**
     * Whether a per-macroblock flag "no coefficients" is transmitted.
     * <p>
     * 매크로블록별 "계수 없음" 플래그가 전송되는지 여부.
     */
    private boolean skipEnabled;

    /**
     * Probability that the skip flag is 0.
     * <p>
     * 건너뜀 플래그가 0 일 확률.
     */
    private int skipProb;

    /**
     * Dequantization factors of the 4 segments: [segment][0..5] = y1dc, y1ac, y2dc, y2ac, uvdc, uvac.
     * <p>
     * 세그먼트 4개의 역양자화 계수: [세그먼트][0..5] = y1dc, y1ac, y2dc, y2ac, uvdc, uvac.
     */
    private final int[][] dequant = new int[4][6];

    /**
     * Number of the first macroblock that is not decoded yet; used to report positions in errors.
     * <p>
     * 아직 디코딩하지 않은 첫 매크로블록의 번호. 오류 위치 보고에 쓴다.
     */
    private int progress;

    /**
     * Creates a decoder; use {@link #decode} instead.
     * <p>
     * 디코더를 만든다. 대신 {@link #decode} 를 쓴다.
     */
    private Vp8Decoder() {
    }

    /**
     * Reads the size of the picture from the header of a key frame without decoding it.
     * <p>
     * 키 프레임의 헤더에서 디코딩 없이 그림 크기를 읽는다.
     *
     * @param data frame data (the "VP8 " chunk payload) / 프레임 데이터("VP8 " 청크 내용)
     * @param off  offset of the frame / 프레임의 위치
     * @param len  length of the frame / 프레임의 길이
     * @return {width, height} / {가로, 세로}
     * @throws MediaException if the header is not valid / 헤더가 올바르지 않을 때
     */
    static int[] readSize(byte[] data, int off, int len) throws MediaException {
        if (len < 10) {
            throw new MediaException(MediaException.Kind.CORRUPT, "VP8 frame is too short");
        }
        int tag = (data[off] & 0xff) | ((data[off + 1] & 0xff) << 8) | ((data[off + 2] & 0xff) << 16);
        if ((tag & 1) != 0) {
            throw new MediaException(MediaException.Kind.CORRUPT, "VP8 frame is not a key frame");
        }
        if ((data[off + 3] & 0xff) != 0x9d || (data[off + 4] & 0xff) != 0x01 || (data[off + 5] & 0xff) != 0x2a) {
            throw new MediaException(MediaException.Kind.CORRUPT, "VP8 start code is missing");
        }
        int w = ((data[off + 6] & 0xff) | ((data[off + 7] & 0xff) << 8)) & 0x3fff;
        int h = ((data[off + 8] & 0xff) | ((data[off + 9] & 0xff) << 8)) & 0x3fff;
        if (w == 0 || h == 0) {
            throw new MediaException(MediaException.Kind.CORRUPT, "VP8 frame has no size");
        }
        return new int[] { w, h };
    }

    /**
     * Decodes a key frame.
     * <p>
     * 키 프레임을 디코딩한다.
     *
     * @param data   frame data (the payload of the "VP8 " chunk) / 프레임 데이터("VP8 " 청크의 내용)
     * @param off    offset of the frame / 프레임의 위치
     * @param len    length of the frame / 프레임의 길이
     * @param limits decoding limits / 디코딩 제한
     * @return the reconstructed and loop-filtered frame / 복원되고 루프 필터링된 프레임
     * @throws MediaException CORRUPT for damaged or truncated data, TOO_LARGE over a limit, CANCELLED if the thread is interrupted / 손상되거나 잘린 데이터는 CORRUPT, 제한 초과는 TOO_LARGE, 스레드가 인터럽트되면 CANCELLED
     */
    static Vp8Frame decode(byte[] data, int off, int len, DecodeLimits limits) throws MediaException {
        return new Vp8Decoder().run(data, off, len, limits);
    }

    /**
     * Does the work of {@link #decode}.
     * <p>
     * {@link #decode} 의 실제 작업을 한다.
     *
     * @param data   frame data / 프레임 데이터
     * @param off    offset / 위치
     * @param len    length / 길이
     * @param limits limits / 제한
     * @return the frame / 프레임
     * @throws MediaException on failure / 실패 시
     */
    private Vp8Frame run(byte[] data, int off, int len, DecodeLimits limits) throws MediaException {
        int[] size = readSize(data, off, len);
        int tag = (data[off] & 0xff) | ((data[off + 1] & 0xff) << 8) | ((data[off + 2] & 0xff) << 16);
        int profile = (tag >> 1) & 7;
        boolean show = ((tag >> 4) & 1) != 0;
        int part0Size = tag >>> 5;
        if (profile > 3) {
            throw new MediaException(MediaException.Kind.CORRUPT, "unknown VP8 profile " + profile);
        }
        if (!show) {
            throw new MediaException(MediaException.Kind.CORRUPT, "the VP8 frame is not displayable");
        }
        limits.checkImageSize(size[0], size[1]);
        int firstStart = off + 10;
        if (part0Size > len - 10) {
            throw new MediaException(MediaException.Kind.CORRUPT, "truncated first partition of the VP8 frame");
        }
        Vp8BoolDecoder br = new Vp8BoolDecoder(data, firstStart, part0Size);
        System.arraycopy(Vp8CoeffTables.DEFAULT_PROBS, 0, probs, 0, probs.length);
        // 색 공간과 클램핑 형식 비트는 읽기만 한다(항상 클램핑하므로 영향이 없다).
        br.getLiteral(2);
        readSegmentHeader(br);
        readFilterHeader(br);
        int partitions = 1 << br.getLiteral(2);
        int[] quantHeader = readQuantHeader(br);
        br.getFlag();
        readProbabilityUpdates(br);
        if (br.isOverrun()) {
            throw new MediaException(MediaException.Kind.CORRUPT, "truncated VP8 frame header");
        }
        Vp8BoolDecoder[] token = openPartitions(data, firstStart + part0Size, len - 10 - part0Size, partitions);
        initDequant(quantHeader);
        Vp8Frame frame = new Vp8Frame(size[0], size[1]);
        mbCols = frame.mbCols;
        mbRows = frame.mbRows;
        int[] levels = new int[mbCols * mbRows];
        boolean[] inner = new boolean[mbCols * mbRows];
        decodeMacroblocks(frame, br, token, levels, inner);
        if (br.isOverrun()) {
            throw new MediaException(MediaException.Kind.CORRUPT, "truncated first partition of the VP8 frame");
        }
        if (filterLevel != 0) {
            Vp8LoopFilter.filterFrame(frame, simpleFilter, sharpness, levels, inner);
            checkInterrupt();
        }
        return frame;
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
     * Reads the segment header (RFC 6386 section 9.3).
     * <p>
     * 세그먼트 헤더를 읽는다(RFC 6386 9.3절).
     *
     * @param br first partition / 첫 파티션
     */
    private void readSegmentHeader(Vp8BoolDecoder br) {
        segmentsEnabled = br.getFlag() != 0;
        if (!segmentsEnabled) {
            return;
        }
        updateSegmentMap = br.getFlag() != 0;
        boolean updateData = br.getFlag() != 0;
        if (updateData) {
            segmentAbsolute = br.getFlag() != 0;
            // 세그먼트마다 양자화 값을 읽는다(없으면 0).
            for (int i = 0; i < 4; i++) {
                segmentQuant[i] = br.getOptionalSigned(7);
            }
            // 세그먼트마다 루프 필터 값을 읽는다(없으면 0).
            for (int i = 0; i < 4; i++) {
                segmentFilter[i] = br.getOptionalSigned(6);
            }
        }
        if (updateSegmentMap) {
            // 세그먼트 id 트리의 확률을 읽는다(없으면 255).
            for (int i = 0; i < 3; i++) {
                segmentTreeProbs[i] = br.getFlag() != 0 ? br.getLiteral(8) : 255;
            }
        }
    }

    /**
     * Reads the loop filter header (RFC 6386 section 9.6).
     * <p>
     * 루프 필터 헤더를 읽는다(RFC 6386 9.6절).
     *
     * @param br first partition / 첫 파티션
     */
    private void readFilterHeader(Vp8BoolDecoder br) {
        simpleFilter = br.getFlag() != 0;
        filterLevel = br.getLiteral(6);
        sharpness = br.getLiteral(3);
        filterDeltaEnabled = br.getFlag() != 0;
        if (filterDeltaEnabled && br.getFlag() != 0) {
            // 참조 프레임별 변화량 네 개를 읽는다.
            for (int i = 0; i < 4; i++) {
                refDelta[i] = br.getOptionalSigned(6);
            }
            // 예측 모드별 변화량 네 개를 읽는다.
            for (int i = 0; i < 4; i++) {
                modeDelta[i] = br.getOptionalSigned(6);
            }
        }
    }

    /**
     * Reads the quantizer indices (RFC 6386 section 9.6).
     * <p>
     * 양자화 색인을 읽는다(RFC 6386 9.6절).
     *
     * @param br first partition / 첫 파티션
     * @return {base index, y1 dc delta, y2 dc delta, y2 ac delta, uv dc delta, uv ac delta} / {기본 색인, y1 dc 변화량, y2 dc 변화량, y2 ac 변화량, uv dc 변화량, uv ac 변화량}
     */
    private int[] readQuantHeader(Vp8BoolDecoder br) {
        int[] q = new int[6];
        q[0] = br.getLiteral(7);
        // 다섯 개의 변화량을 읽는다(각각 플래그가 꺼져 있으면 0).
        for (int i = 1; i < 6; i++) {
            q[i] = br.getOptionalSigned(4);
        }
        return q;
    }

    /**
     * Reads the updates of the token probabilities and the skip flag settings (RFC 6386 sections 13.4 and 9.10).
     * <p>
     * 토큰 확률의 갱신과 건너뜀 플래그 설정을 읽는다(RFC 6386 13.4, 9.10절).
     *
     * @param br first partition / 첫 파티션
     */
    private void readProbabilityUpdates(Vp8BoolDecoder br) {
        // 모든 확률 위치마다 갱신 여부 비트를 읽고 켜져 있으면 8비트 새 값을 읽는다.
        for (int i = 0; i < probs.length; i++) {
            if (br.getBit(Vp8CoeffTables.UPDATE_PROBS[i]) != 0) {
                probs[i] = br.getLiteral(8);
            }
        }
        skipEnabled = br.getFlag() != 0;
        skipProb = skipEnabled ? br.getLiteral(8) : 0;
    }

    /**
     * Sets up the decoders of the token partitions (RFC 6386 section 9.5).
     * <p>
     * 토큰 파티션의 디코더를 준비한다(RFC 6386 9.5절).
     *
     * @param data       data array / 데이터 배열
     * @param start      position after the first partition / 첫 파티션 뒤의 위치
     * @param available  number of bytes after the first partition / 첫 파티션 뒤의 바이트 수
     * @param partitions number of partitions / 파티션 수
     * @return one decoder per partition / 파티션마다 디코더 하나
     * @throws MediaException if the sizes do not fit into the data / 크기가 데이터에 맞지 않을 때
     */
    private Vp8BoolDecoder[] openPartitions(byte[] data, int start, int available, int partitions) throws MediaException {
        int tableSize = 3 * (partitions - 1);
        if (available < tableSize) {
            throw new MediaException(MediaException.Kind.CORRUPT, "truncated partition table of the VP8 frame");
        }
        Vp8BoolDecoder[] result = new Vp8BoolDecoder[partitions];
        int pos = start + tableSize;
        int left = available - tableSize;
        // 마지막을 뺀 파티션은 표에서 크기를 읽고 마지막은 남은 전부이다.
        for (int i = 0; i < partitions; i++) {
            int size;
            if (i < partitions - 1) {
                int t = start + 3 * i;
                size = (data[t] & 0xff) | ((data[t + 1] & 0xff) << 8) | ((data[t + 2] & 0xff) << 16);
            } else {
                size = left;
            }
            if (size > left) {
                throw new MediaException(MediaException.Kind.CORRUPT, "truncated partition " + i + " of the VP8 frame");
            }
            result[i] = new Vp8BoolDecoder(data, pos, size);
            pos += size;
            left -= size;
        }
        return result;
    }

    /**
     * Computes the dequantization factors of the segments (RFC 6386 section 14.1).
     * <p>
     * 세그먼트별 역양자화 계수를 계산한다(RFC 6386 14.1절).
     *
     * @param q the quantizer header values / 양자화 헤더 값
     */
    private void initDequant(int[] q) {
        // 세그먼트가 꺼져 있으면 첫 세그먼트만 쓰이지만 네 개 모두 같은 값으로 채운다.
        for (int s = 0; s < 4; s++) {
            int base = q[0];
            if (segmentsEnabled) {
                base = segmentAbsolute ? segmentQuant[s] : base + segmentQuant[s];
            }
            int[] d = dequant[s];
            d[0] = Vp8CoeffTables.DC_Q[clampQ(base + q[1])];
            d[1] = Vp8CoeffTables.AC_Q[clampQ(base)];
            d[2] = Vp8CoeffTables.DC_Q[clampQ(base + q[2])] * 2;
            d[3] = Vp8CoeffTables.AC_Q[clampQ(base + q[3])] * 155 / 100;
            if (d[3] < 8) {
                d[3] = 8;
            }
            d[4] = Vp8CoeffTables.DC_Q[clampQ(base + q[4])];
            if (d[4] > 132) {
                d[4] = 132;
            }
            d[5] = Vp8CoeffTables.AC_Q[clampQ(base + q[5])];
        }
    }

    /**
     * Clamps a quantizer index to 0..127.
     * <p>
     * 양자화 색인을 0..127 로 자른다.
     *
     * @param q index / 색인
     * @return clamped index / 자른 색인
     */
    private static int clampQ(int q) {
        return q < 0 ? 0 : (q > 127 ? 127 : q);
    }

    /**
     * Computes the loop filter level of a macroblock (RFC 6386 section 9.6 and the reference decoder).
     * <p>
     * 매크로블록의 루프 필터 수준을 계산한다(RFC 6386 9.6절과 참조 디코더).
     *
     * @param segment segment id / 세그먼트 id
     * @param yMode   luma mode of the macroblock / 매크로블록의 휘도 모드
     * @return level 0..63 / 수준 0..63
     */
    private int macroblockFilterLevel(int segment, int yMode) {
        int level = filterLevel;
        if (segmentsEnabled) {
            level = segmentAbsolute ? segmentFilter[segment] : level + segmentFilter[segment];
            level = level < 0 ? 0 : (level > 63 ? 63 : level);
        }
        if (filterDeltaEnabled) {
            level += refDelta[0];
            if (yMode == Vp8ModeTables.B_PRED) {
                level += modeDelta[0];
            }
        }
        return level < 0 ? 0 : (level > 63 ? 63 : level);
    }

    /**
     * Decodes all macroblocks: modes, coefficients and reconstruction, row by row.
     * <p>
     * 모든 매크로블록(모드, 계수, 복원)을 행 단위로 디코딩한다.
     *
     * @param f      destination frame / 목적지 프레임
     * @param br     first partition (modes) / 첫 파티션(모드)
     * @param token  token partitions / 토큰 파티션들
     * @param levels receives the filter level of each macroblock / 매크로블록별 필터 수준을 받는다
     * @param inner  receives for each macroblock whether inner edges are filtered / 매크로블록별로 안쪽 경계 필터링 여부를 받는다
     * @throws MediaException on damaged data or cancellation / 손상된 데이터나 취소 시
     */
    private void decodeMacroblocks(Vp8Frame f, Vp8BoolDecoder br, Vp8BoolDecoder[] token, int[] levels, boolean[] inner)
            throws MediaException {
        int[] topNz = new int[mbCols * 9];
        int[] leftNz = new int[9];
        int[] topModes = new int[mbCols * 4];
        int[] leftModes = new int[4];
        int[] bm = new int[16];
        int[] coeffs = new int[25 * 16];
        int[] y2out = new int[16];
        int[] a = new int[8];
        int[] l = new int[4];
        int[] pred = new int[16];
        // 매크로블록 행마다 처리한다.
        for (int mby = 0; mby < mbRows; mby++) {
            checkInterrupt();
            Vp8BoolDecoder tb = token[mby & (token.length - 1)];
            java.util.Arrays.fill(leftNz, 0);
            java.util.Arrays.fill(leftModes, Vp8ModeTables.B_DC_PRED);
            // 행 안의 매크로블록을 왼쪽부터 처리한다.
            for (int mbx = 0; mbx < mbCols; mbx++) {
                progress = mby * mbCols + mbx;
                int segment = 0;
                if (updateSegmentMap) {
                    segment = br.getBit(segmentTreeProbs[0]) != 0 ? 2 + br.getBit(segmentTreeProbs[2]) : br.getBit(segmentTreeProbs[1]);
                }
                boolean skip = skipEnabled && br.getBit(skipProb) != 0;
                int yMode = br.getTree(Vp8ModeTables.KF_YMODE_TREE, Vp8ModeTables.KF_YMODE_PROB, 0);
                readSubblockModes(br, yMode, mbx, topModes, leftModes, bm);
                int uvMode = br.getTree(Vp8ModeTables.UV_MODE_TREE, Vp8ModeTables.KF_UV_MODE_PROB, 0);
                boolean anyNz = false;
                if (!skip) {
                    java.util.Arrays.fill(coeffs, 0);
                    anyNz = decodeResiduals(tb, yMode, segment, mbx, topNz, leftNz, coeffs);
                } else {
                    resetContext(yMode, mbx, topNz, leftNz);
                }
                if (tb.isOverrun()) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "truncated token partition of the VP8 frame");
                }
                levels[progress] = macroblockFilterLevel(segment, yMode);
                inner[progress] = anyNz || yMode == Vp8ModeTables.B_PRED;
                reconstruct(f, mbx, mby, yMode, uvMode, bm, !skip, coeffs, y2out, a, l, pred);
            }
            // 다음 행의 위-오른쪽 예측에 쓰도록 맨 오른쪽 매크로블록 아래 행 오른쪽 4픽셀을 마지막 픽셀로 채운다.
            int last = f.yIndex(mbCols * 16 - 1, mby * 16 + 15);
            byte lastPixel = f.y[last];
            // 오른쪽 4픽셀을 마지막 픽셀 값으로 채운다.
            for (int i = 1; i <= 4; i++) {
                f.y[last + i] = lastPixel;
            }
        }
    }

    /**
     * Reads the modes of the 16 luma subblocks of a macroblock, or derives them from the macroblock mode, and updates the contexts for
     * the neighbours (RFC 6386 sections 11.2 and 11.3).
     * <p>
     * 매크로블록의 휘도 서브블록 16개의 모드를 읽거나 매크로블록 모드에서 유도하고 이웃을 위한 문맥을 갱신한다(RFC 6386 11.2, 11.3절).
     *
     * @param br        first partition / 첫 파티션
     * @param yMode     macroblock luma mode / 매크로블록 휘도 모드
     * @param mbx       macroblock column / 매크로블록 열
     * @param topModes  modes of the bottom subblock row of the macroblocks above, 4 per column / 위쪽 매크로블록 맨 아래 서브블록 행의 모드(열마다 4개)
     * @param leftModes modes of the right subblock column of the macroblock to the left / 왼쪽 매크로블록 맨 오른쪽 서브블록 열의 모드
     * @param bm        receives the 16 subblock modes / 서브블록 모드 16개를 받는다
     */
    private void readSubblockModes(Vp8BoolDecoder br, int yMode, int mbx, int[] topModes, int[] leftModes, int[] bm) {
        if (yMode == Vp8ModeTables.B_PRED) {
            // 서브블록마다 위와 왼쪽 이웃의 모드로 확률을 골라 모드를 읽는다.
            for (int i = 0; i < 16; i++) {
                int above = i < 4 ? topModes[mbx * 4 + i] : bm[i - 4];
                int left = (i & 3) == 0 ? leftModes[i >> 2] : bm[i - 1];
                bm[i] = br.getTree(Vp8ModeTables.BMODE_TREE, Vp8ModeTables.KF_BMODE_PROBS, (above * 10 + left) * 9);
            }
        } else {
            int implied = IMPLIED_BMODE[yMode];
            // 16x16 모드는 서브블록 모드가 모두 같다고 본다(이웃 문맥용).
            for (int i = 0; i < 16; i++) {
                bm[i] = implied;
            }
        }
        // 이 매크로블록의 아래 행과 오른쪽 열 모드를 이웃의 문맥으로 저장한다.
        for (int i = 0; i < 4; i++) {
            topModes[mbx * 4 + i] = bm[12 + i];
            leftModes[i] = bm[4 * i + 3];
        }
    }

    /**
     * Clears the non-zero contexts of a macroblock without coefficients; the context of the Y2 block is kept if the macroblock has none.
     * <p>
     * 계수가 없는 매크로블록의 비영 문맥을 지운다. Y2 블록이 없는 매크로블록이면 Y2 문맥은 유지한다.
     *
     * @param yMode  macroblock luma mode / 매크로블록 휘도 모드
     * @param mbx    macroblock column / 매크로블록 열
     * @param topNz  contexts above / 위쪽 문맥
     * @param leftNz contexts to the left / 왼쪽 문맥
     */
    private static void resetContext(int yMode, int mbx, int[] topNz, int[] leftNz) {
        int count = yMode == Vp8ModeTables.B_PRED ? 8 : 9;
        // Y2 를 가진 매크로블록이면 Y2 문맥까지 9개, 아니면 Y/U/V 8개를 지운다.
        for (int i = 0; i < count; i++) {
            topNz[mbx * 9 + i] = 0;
            leftNz[i] = 0;
        }
    }

    /**
     * Decodes the coefficients of all blocks of a macroblock (RFC 6386 section 13).
     * <p>
     * 매크로블록의 모든 블록의 계수를 디코딩한다(RFC 6386 13절).
     *
     * @param tb      token partition / 토큰 파티션
     * @param yMode   macroblock luma mode / 매크로블록 휘도 모드
     * @param segment segment id / 세그먼트 id
     * @param mbx     macroblock column / 매크로블록 열
     * @param topNz   contexts above / 위쪽 문맥
     * @param leftNz  contexts to the left / 왼쪽 문맥
     * @param coeffs  receives 25 blocks of 16 dequantized coefficients: 16 luma, 4 U, 4 V, 1 Y2 / 역양자화된 계수 16개씩 블록 25개를 받는다: 휘도 16, U 4, V 4, Y2 1
     * @return true if any block has a coefficient token / 계수 토큰이 있는 블록이 하나라도 있으면 true
     */
    private boolean decodeResiduals(Vp8BoolDecoder tb, int yMode, int segment, int mbx, int[] topNz, int[] leftNz, int[] coeffs) {
        int[] dq = dequant[segment];
        int tBase = mbx * 9;
        boolean any = false;
        int firstLuma;
        int lumaType;
        if (yMode != Vp8ModeTables.B_PRED) {
            // Y2 블록(종류 1)을 먼저 디코딩한다.
            int ctx = leftNz[8] + topNz[tBase + 8];
            int n = decodeBlock(tb, 1, ctx, dq[2], dq[3], 0, coeffs, 24 * 16);
            int nz = n > 0 ? 1 : 0;
            leftNz[8] = nz;
            topNz[tBase + 8] = nz;
            any |= nz != 0;
            firstLuma = 1;
            lumaType = 0;
        } else {
            firstLuma = 0;
            lumaType = 3;
        }
        // 휘도 블록 16개를 래스터 순서로 디코딩한다.
        for (int i = 0; i < 16; i++) {
            int bx = i & 3;
            int by = i >> 2;
            int ctx = leftNz[by] + topNz[tBase + bx];
            int n = decodeBlock(tb, lumaType, ctx, dq[0], dq[1], firstLuma, coeffs, i * 16);
            int nz = n > firstLuma ? 1 : 0;
            leftNz[by] = nz;
            topNz[tBase + bx] = nz;
            any |= nz != 0;
        }
        // 색차 평면 둘(U 와 V)의 블록 4개씩을 디코딩한다.
        for (int plane = 0; plane < 2; plane++) {
            int ctxBase = 4 + plane * 2;
            // 색차 블록 4개를 래스터 순서로 디코딩한다.
            for (int i = 0; i < 4; i++) {
                int bx = i & 1;
                int by = i >> 1;
                int ctx = leftNz[ctxBase + by] + topNz[tBase + ctxBase + bx];
                int n = decodeBlock(tb, 2, ctx, dq[4], dq[5], 0, coeffs, (16 + plane * 4 + i) * 16);
                int nz = n > 0 ? 1 : 0;
                leftNz[ctxBase + by] = nz;
                topNz[tBase + ctxBase + bx] = nz;
                any |= nz != 0;
            }
        }
        return any;
    }

    /**
     * Decodes the tokens of one 4x4 block with the token tree of RFC 6386 section 13.2 and stores the dequantized coefficients at their
     * raster positions.
     * <p>
     * 4x4 블록 하나의 토큰을 RFC 6386 13.2절의 토큰 트리로 디코딩하고 역양자화한 계수를 래스터 위치에 저장한다.
     *
     * @param br    token partition / 토큰 파티션
     * @param type  block type: 0 luma after Y2, 1 Y2, 2 chroma, 3 luma with DC / 블록 종류: 0 Y2 뒤의 휘도, 1 Y2, 2 색차, 3 DC 가 있는 휘도
     * @param ctx   0, 1 or 2: how many neighbours had coefficients / 0, 1, 2: 계수가 있던 이웃의 수
     * @param dcq   dequantization factor of the DC coefficient / DC 계수의 역양자화 계수
     * @param acq   dequantization factor of the other coefficients / 나머지 계수의 역양자화 계수
     * @param first position of the first coefficient, 1 for type 0 / 첫 계수의 위치(종류 0 이면 1)
     * @param out   destination of the coefficients / 계수의 목적지
     * @param base  offset of the block in out / out 안의 블록 위치
     * @return the position after the last decoded token (equals first if the block ended at once) / 마지막으로 디코딩한 토큰 다음 위치(곧바로 끝났으면 first 와 같다)
     */
    private int decodeBlock(Vp8BoolDecoder br, int type, int ctx, int dcq, int acq, int first, int[] out, int base) {
        int[] p = probs;
        int n = first;
        int pi = ((type * 8 + BANDS[n]) * 3 + ctx) * 11;
        // 위치마다 토큰을 읽는다. 블록 끝(EOB) 토큰이 오거나 16개를 채우면 끝난다.
        while (n < 16) {
            if (br.getBit(p[pi]) == 0) {
                return n;
            }
            // 0 토큰이 이어지는 동안은 EOB 를 확인하지 않고 다음 위치로 간다.
            while (br.getBit(p[pi + 1]) == 0) {
                n++;
                if (n == 16) {
                    return 16;
                }
                pi = ((type * 8 + BANDS[n]) * 3) * 11;
            }
            int v;
            int nextCtx;
            if (br.getBit(p[pi + 2]) == 0) {
                v = 1;
                nextCtx = 1;
            } else {
                v = largeValue(br, p, pi);
                nextCtx = 2;
            }
            if (br.getBit(128) != 0) {
                v = -v;
            }
            out[base + ZIGZAG[n]] = v * (n > 0 ? acq : dcq);
            n++;
            pi = n < 16 ? ((type * 8 + BANDS[n]) * 3 + nextCtx) * 11 : 0;
        }
        return 16;
    }

    /**
     * Decodes a token value of 2 or more (the part of the token tree below the node "one"), including the extra bits of the categories.
     * <p>
     * 2 이상의 토큰 값을 디코딩한다(토큰 트리에서 "1" 노드 아래 부분). 범주의 추가 비트도 읽는다.
     *
     * @param br token partition / 토큰 파티션
     * @param p  probabilities / 확률
     * @param pi index of the probabilities of the current context / 현재 문맥 확률의 위치
     * @return the absolute value / 절대값
     */
    private static int largeValue(Vp8BoolDecoder br, int[] p, int pi) {
        if (br.getBit(p[pi + 3]) == 0) {
            if (br.getBit(p[pi + 4]) == 0) {
                return 2;
            }
            return 3 + br.getBit(p[pi + 5]);
        }
        if (br.getBit(p[pi + 6]) == 0) {
            if (br.getBit(p[pi + 7]) == 0) {
                return 5 + br.getBit(159);
            }
            int v = 7 + 2 * br.getBit(165);
            return v + br.getBit(145);
        }
        int bit1 = br.getBit(p[pi + 8]);
        int bit0 = br.getBit(p[pi + 9 + bit1]);
        int cat = 2 * bit1 + bit0;
        int[] table = cat == 0 ? CAT3 : (cat == 1 ? CAT4 : (cat == 2 ? CAT5 : CAT6));
        int v = 0;
        // 추가 비트를 최상위부터 읽어 모은다.
        for (int i = 0; i < table.length; i++) {
            v += v + br.getBit(table[i]);
        }
        return v + 3 + (8 << cat);
    }

    /**
     * Reconstructs one macroblock into the frame: prediction and the inverse transforms of the residual.
     * <p>
     * 매크로블록 하나를 프레임에 복원한다: 예측과 잔차의 역변환.
     *
     * @param f           frame / 프레임
     * @param mbx         macroblock column / 매크로블록 열
     * @param mby         macroblock row / 매크로블록 행
     * @param yMode       luma mode / 휘도 모드
     * @param uvMode      chroma mode / 색차 모드
     * @param bm          subblock modes / 서브블록 모드
     * @param hasResidual whether coefficients were decoded / 계수를 디코딩했는지
     * @param coeffs      coefficients / 계수
     * @param y2out       scratch array of 16 values / 값 16개짜리 작업 배열
     * @param a           scratch array of 8 values / 값 8개짜리 작업 배열
     * @param l           scratch array of 4 values / 값 4개짜리 작업 배열
     * @param pred        scratch array of 16 values / 값 16개짜리 작업 배열
     */
    private void reconstruct(Vp8Frame f, int mbx, int mby, int yMode, int uvMode, int[] bm, boolean hasResidual, int[] coeffs,
            int[] y2out, int[] a, int[] l, int[] pred) {
        int ys = f.yStride;
        int yBase = f.yIndex(mbx * 16, mby * 16);
        if (yMode == Vp8ModeTables.B_PRED) {
            // 오른쪽 열 서브블록들이 쓰는 위-오른쪽 4픽셀(위쪽 매크로블록 행의 맨 아래 줄)을 구한다.
            int[] topRight = new int[4];
            int trBase = f.yIndex(mbx * 16 + 16, mby * 16 - 1);
            if (mby == 0) {
                java.util.Arrays.fill(topRight, 127);
            } else {
                // 마지막 열이면 위쪽 행의 맨 끝에 채워 둔 값을 그대로 쓴다.
                for (int i = 0; i < 4; i++) {
                    topRight[i] = f.y[trBase + i] & 0xff;
                }
            }
            // 서브블록 16개를 래스터 순서로 예측하고 잔차를 더한다.
            for (int i = 0; i < 16; i++) {
                int sx = i & 3;
                int sy = i >> 2;
                int base = yBase + sy * 4 * ys + sx * 4;
                int above = base - ys;
                // 위 4픽셀과 위-오른쪽 4픽셀을 모은다.
                for (int k = 0; k < 4; k++) {
                    a[k] = f.y[above + k] & 0xff;
                }
                // 위-오른쪽 4픽셀을 모은다.
                for (int k = 0; k < 4; k++) {
                    a[4 + k] = sx == 3 ? topRight[k] : (f.y[above + 4 + k] & 0xff);
                }
                // 왼쪽 4픽셀을 모은다.
                for (int k = 0; k < 4; k++) {
                    l[k] = f.y[base + k * ys - 1] & 0xff;
                }
                int p = f.y[above - 1] & 0xff;
                Vp8Predict.predict4x4(bm[i], a, l, p, pred);
                // 예측값을 평면에 쓴다.
                for (int r = 0; r < 4; r++) {
                    for (int c = 0; c < 4; c++) {
                        f.y[base + r * ys + c] = (byte) pred[r * 4 + c];
                    }
                }
                if (hasResidual) {
                    addResidual(coeffs, i * 16, f.y, base, ys);
                }
            }
        } else {
            Vp8Predict.predictBlock(f.y, yBase, ys, 16, yMode, mby > 0, mbx > 0);
            if (hasResidual) {
                Vp8Predict.inverseWalsh(coeffs, 24 * 16, y2out);
                // Y2 변환 결과를 각 휘도 블록의 DC 계수로 넣는다.
                for (int i = 0; i < 16; i++) {
                    coeffs[i * 16] = y2out[i];
                }
                // 휘도 블록 16개의 잔차를 더한다.
                for (int i = 0; i < 16; i++) {
                    addResidual(coeffs, i * 16, f.y, yBase + (i >> 2) * 4 * ys + (i & 3) * 4, ys);
                }
            }
        }
        int cs = f.uvStride;
        int uBase = f.uvIndex(mbx * 8, mby * 8);
        Vp8Predict.predictBlock(f.u, uBase, cs, 8, uvMode, mby > 0, mbx > 0);
        Vp8Predict.predictBlock(f.v, uBase, cs, 8, uvMode, mby > 0, mbx > 0);
        if (hasResidual) {
            // U 와 V 블록 4개씩의 잔차를 더한다.
            for (int i = 0; i < 4; i++) {
                int base = uBase + (i >> 1) * 4 * cs + (i & 1) * 4;
                addResidual(coeffs, (16 + i) * 16, f.u, base, cs);
                addResidual(coeffs, (20 + i) * 16, f.v, base, cs);
            }
        }
    }

    /**
     * Adds the inverse transform of a block to the prediction, skipping empty blocks and using the shortcut for DC-only blocks.
     * <p>
     * 블록의 역변환을 예측값에 더한다. 빈 블록은 건너뛰고 DC 만 있는 블록은 지름길을 쓴다.
     *
     * @param c      coefficients / 계수
     * @param off    offset of the block / 블록 위치
     * @param plane  plane / 평면
     * @param base   index of the block's top-left pixel / 블록의 왼쪽 위 픽셀 색인
     * @param stride row distance / 행 사이 거리
     */
    private static void addResidual(int[] c, int off, byte[] plane, int base, int stride) {
        int ac = 0;
        // AC 계수가 하나라도 0 이 아닌지 모아 본다.
        for (int i = 1; i < 16; i++) {
            ac |= c[off + i];
        }
        if (ac == 0) {
            if (c[off] != 0) {
                Vp8Predict.dcOnlyAdd(c[off], plane, base, stride);
            }
        } else {
            Vp8Predict.inverseDctAdd(c, off, plane, base, stride);
        }
    }
}
