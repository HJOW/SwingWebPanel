package org.duckdns.hjow.swingwebpanel.media.image.avif;

import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * Reads the quantized transform coefficients of one transform block (plan item P18-03; AV1 specification sections 5.11.39 "Coefficients
 * syntax" and 8.3.2 for the contexts) and keeps the above and left level/DC contexts that the coefficient syntax needs. It also decodes the
 * intra transform type. One reader belongs to one tile decoder.
 * <p>
 * 변환 블록 하나의 양자화된 변환 계수를 읽고(계획 P18-03; AV1 규격 5.11.39 "Coefficients syntax", 문맥은 8.3.2절) 계수 구문에 필요한 위쪽/왼쪽
 * 수준/DC 문맥을 유지한다. 인트라 변환 종류도 디코딩한다. 읽기 객체 하나는 타일 디코더 하나에 속한다.
 */
final class Av1CoeffReader {

    /**
     * The transform type identifiers (DCT_DCT is 0).
     * <p>
     * 변환 종류 식별자(DCT_DCT 는 0).
     */
    private static final int DCT_DCT = 0;

    /**
     * The identity transform in both directions.
     * <p>
     * 두 방향 모두 항등 변환.
     */
    private static final int IDTX = 9;

    /**
     * The transform class of one dimensional transforms along the vertical direction.
     * <p>
     * 세로 방향 1차원 변환의 변환 종류 분류.
     */
    private static final int TX_CLASS_VERT = 2;

    /**
     * The transform class of one dimensional transforms along the horizontal direction.
     * <p>
     * 가로 방향 1차원 변환의 변환 종류 분류.
     */
    private static final int TX_CLASS_HORIZ = 1;

    /**
     * The two dimensional transform class.
     * <p>
     * 2차원 변환 분류.
     */
    private static final int TX_CLASS_2D = 0;

    /**
     * The tile decoder that owns this reader and holds the block state.
     * <p>
     * 이 읽기 객체를 소유하며 블록 상태를 가진 타일 디코더.
     */
    private final Av1TileDecoder t;

    /**
     * The above level context of each plane, in units of 4 samples of the plane.
     * <p>
     * 각 평면의 위쪽 수준 문맥. 평면의 4샘플 단위.
     */
    final int[][] aboveLevel;

    /**
     * The above DC sign context of each plane.
     * <p>
     * 각 평면의 위쪽 DC 부호 문맥.
     */
    final int[][] aboveDc;

    /**
     * The left level context of each plane.
     * <p>
     * 각 평면의 왼쪽 수준 문맥.
     */
    final int[][] leftLevel;

    /**
     * The left DC sign context of each plane.
     * <p>
     * 각 평면의 왼쪽 DC 부호 문맥.
     */
    final int[][] leftDc;

    /**
     * The quantized coefficient levels of the current block.
     * <p>
     * 현재 블록의 양자화된 계수 수준.
     */
    final int[] quant = new int[1024];

    /**
     * The transform type of the current transform block that {@link #coeffs} determined.
     * <p>
     * {@link #coeffs} 가 정한 현재 변환 블록의 변환 종류.
     */
    int planeTxType;

    /**
     * Creates the reader.
     * <p>
     * 읽기 객체를 만든다.
     *
     * @param t the tile decoder / 타일 디코더
     */
    Av1CoeffReader(Av1TileDecoder t) {
        this.t = t;
        int cols = t.frame.miCols + 64;
        int rows = t.frame.miRows + 64;
        aboveLevel = new int[3][cols];
        aboveDc = new int[3][cols];
        leftLevel = new int[3][rows];
        leftDc = new int[3][rows];
    }

    /**
     * Zeroes the left contexts (<code>clear_left_context</code>).
     * <p>
     * 왼쪽 문맥을 0 으로 만든다(<code>clear_left_context</code>).
     */
    void clearLeft() {
        // 평면마다 왼쪽 문맥 배열을 0 으로 채운다.
        for (int p = 0; p < 3; p++) {
            java.util.Arrays.fill(leftLevel[p], 0);
            java.util.Arrays.fill(leftDc[p], 0);
        }
    }

    /**
     * The transform class of a transform type.
     * <p>
     * 변환 종류의 변환 분류.
     *
     * @param txType the transform type / 변환 종류
     * @return the class / 분류
     */
    static int txClass(int txType) {
        if (txType == 10 || txType == 12 || txType == 14) {
            return TX_CLASS_VERT;
        }
        if (txType == 11 || txType == 13 || txType == 15) {
            return TX_CLASS_HORIZ;
        }
        return TX_CLASS_2D;
    }

    /**
     * Picks the scan order (<code>get_scan</code>).
     * <p>
     * 스캔 순서를 고른다(<code>get_scan</code>).
     *
     * @param txSz   the transform size / 변환 크기
     * @param txType the transform type / 변환 종류
     * @return the scan table / 스캔 표
     */
    static int[] scan(int txSz, int txType) {
        if (txSz == 17) {
            return AvifTables.DEFAULT_SCAN_16X32;
        }
        if (txSz == 18) {
            return AvifTables.DEFAULT_SCAN_32X16;
        }
        if (AvifTables.TX_SIZE_SQR_UP[txSz] == 4) {
            return AvifTables.DEFAULT_SCAN_32X32;
        }
        if (txType == IDTX) {
            return defaultScan(txSz);
        }
        int cls = txClass(txType);
        if (cls == TX_CLASS_VERT) {
            return mrowScan(txSz);
        }
        if (cls == TX_CLASS_HORIZ) {
            return mcolScan(txSz);
        }
        return defaultScan(txSz);
    }

    /**
     * The default scan of a transform size.
     * <p>
     * 변환 크기의 기본 스캔.
     *
     * @param txSz the transform size / 변환 크기
     * @return the scan table / 스캔 표
     */
    private static int[] defaultScan(int txSz) {
        switch (txSz) {
        case 0:
            return AvifTables.DEFAULT_SCAN_4X4;
        case 5:
            return AvifTables.DEFAULT_SCAN_4X8;
        case 6:
            return AvifTables.DEFAULT_SCAN_8X4;
        case 1:
            return AvifTables.DEFAULT_SCAN_8X8;
        case 7:
            return AvifTables.DEFAULT_SCAN_8X16;
        case 8:
            return AvifTables.DEFAULT_SCAN_16X8;
        case 2:
            return AvifTables.DEFAULT_SCAN_16X16;
        case 9:
            return AvifTables.DEFAULT_SCAN_16X32;
        case 10:
            return AvifTables.DEFAULT_SCAN_32X16;
        case 13:
            return AvifTables.DEFAULT_SCAN_4X16;
        case 14:
            return AvifTables.DEFAULT_SCAN_16X4;
        case 15:
            return AvifTables.DEFAULT_SCAN_8X32;
        case 16:
            return AvifTables.DEFAULT_SCAN_32X8;
        default:
            return AvifTables.DEFAULT_SCAN_32X32;
        }
    }

    /**
     * The row-first scan of a transform size.
     * <p>
     * 변환 크기의 행 우선 스캔.
     *
     * @param txSz the transform size / 변환 크기
     * @return the scan table / 스캔 표
     */
    private static int[] mrowScan(int txSz) {
        switch (txSz) {
        case 0:
            return AvifTables.MROW_SCAN_4X4;
        case 5:
            return AvifTables.MROW_SCAN_4X8;
        case 6:
            return AvifTables.MROW_SCAN_8X4;
        case 1:
            return AvifTables.MROW_SCAN_8X8;
        case 7:
            return AvifTables.MROW_SCAN_8X16;
        case 8:
            return AvifTables.MROW_SCAN_16X8;
        case 2:
            return AvifTables.MROW_SCAN_16X16;
        case 13:
            return AvifTables.MROW_SCAN_4X16;
        default:
            return AvifTables.MROW_SCAN_16X4;
        }
    }

    /**
     * The column-first scan of a transform size.
     * <p>
     * 변환 크기의 열 우선 스캔.
     *
     * @param txSz the transform size / 변환 크기
     * @return the scan table / 스캔 표
     */
    private static int[] mcolScan(int txSz) {
        switch (txSz) {
        case 0:
            return AvifTables.MCOL_SCAN_4X4;
        case 5:
            return AvifTables.MCOL_SCAN_4X8;
        case 6:
            return AvifTables.MCOL_SCAN_8X4;
        case 1:
            return AvifTables.MCOL_SCAN_8X8;
        case 7:
            return AvifTables.MCOL_SCAN_8X16;
        case 8:
            return AvifTables.MCOL_SCAN_16X8;
        case 2:
            return AvifTables.MCOL_SCAN_16X16;
        case 13:
            return AvifTables.MCOL_SCAN_4X16;
        default:
            return AvifTables.MCOL_SCAN_16X4;
        }
    }

    /**
     * The transform set of a transform size for an intra block (<code>get_tx_set</code>): 0 only DCT, 1 and 2 the intra sets.
     * <p>
     * 인트라 블록에서 변환 크기의 변환 집합(<code>get_tx_set</code>): 0 은 DCT 만, 1 과 2 는 인트라 집합.
     *
     * @param txSz the transform size / 변환 크기
     * @return the set / 집합
     */
    int txSet(int txSz) {
        int sqr = AvifTables.TX_SIZE_SQR[txSz];
        int sqrUp = AvifTables.TX_SIZE_SQR_UP[txSz];
        if (sqrUp > 3) {
            return 0;
        }
        // 인터 블록은 변환 집합 1..3 중에서 고른다(집합 3 은 IDTX 와 DCT 만).
        if (t.isInter) {
            if (t.hdr.reducedTxSet || sqrUp == 3) {
                return 3;
            }
            return sqr == 2 ? 2 : 1;
        }
        if (sqrUp == 3) {
            return 0;
        }
        if (t.hdr.reducedTxSet) {
            return 2;
        }
        if (sqr == 2) {
            return 2;
        }
        return 1;
    }

    /**
     * Computes the transform type of a transform block (<code>compute_tx_type</code>).
     * <p>
     * 변환 블록의 변환 종류를 계산한다(<code>compute_tx_type</code>).
     *
     * @param plane the plane / 평면
     * @param txSz  the transform size / 변환 크기
     * @param bx    the x position in 4x4 units of the plane / 평면의 4x4 단위 x 위치
     * @param by    the y position in 4x4 units of the plane / 평면의 4x4 단위 y 위치
     * @return the type / 종류
     */
    private int computeTxType(int plane, int txSz, int bx, int by) {
        if (t.lossless || AvifTables.TX_SIZE_SQR_UP[txSz] > 3) {
            return DCT_DCT;
        }
        int set = txSet(txSz);
        if (plane == 0) {
            return t.frame.txType[by * t.frame.miStride + bx];
        }
        if (t.isInter) {
            // 인터 블록의 색차는 대응하는 휘도 변환 블록의 종류를 쓴다(현재 블록 안에서만).
            int x4 = Math.max(t.miCol, bx << t.seq.subsamplingX);
            int y4 = Math.max(t.miRow, by << t.seq.subsamplingY);
            int lumaType = t.frame.txType[y4 * t.frame.miStride + x4];
            return AvifInterTables.TX_TYPE_IN_SET_INTER[set * 16 + lumaType] == 0 ? DCT_DCT : lumaType;
        }
        int type = AvifTables.MODE_TO_TXFM[t.uvMode];
        if (AvifTables.TX_TYPE_IN_SET_INTRA[set * 16 + type] == 0) {
            return DCT_DCT;
        }
        return type;
    }

    /**
     * Reads the intra transform type of a luma transform block and stores it in the frame (<code>transform_type</code>).
     * <p>
     * 휘도 변환 블록의 인트라 변환 종류를 읽어 프레임에 저장한다(<code>transform_type</code>).
     *
     * @param x4   the x position in 4x4 units / 4x4 단위 x 위치
     * @param y4   the y position in 4x4 units / 4x4 단위 y 위치
     * @param txSz the transform size / 변환 크기
     */
    private void readTransformType(int x4, int y4, int txSz) {
        int set = txSet(txSz);
        int type = DCT_DCT;
        int q = t.hdr.segmentationEnabled ? t.hdr.qindexOfSegment(t.segmentId) : t.hdr.baseQIdx;
        if (set > 0 && q > 0 && t.isInter) {
            int sqr = AvifTables.TX_SIZE_SQR[txSz];
            // 인터 블록은 집합에 따라 분포와 역 변환 표가 다르다.
            if (set == 1) {
                type = AvifInterTables.TX_TYPE_INTER_INV_SET1[t.sd.symbol(t.cdfs.interTxTypeSet1, sqr * 17, 16)];
            } else if (set == 2) {
                type = AvifInterTables.TX_TYPE_INTER_INV_SET2[t.sd.symbol(t.cdfs.interTxTypeSet2, 0, 12)];
            } else {
                type = AvifInterTables.TX_TYPE_INTER_INV_SET3[t.sd.symbol(t.cdfs.interTxTypeSet3, sqr * 3, 2)];
            }
        } else if (set > 0 && q > 0) {
            int intraDir = t.useFilterIntra ? AvifTables.FILTER_INTRA_MODE_TO_INTRA_DIR[t.filterIntraMode] : t.yMode;
            int sqr = AvifTables.TX_SIZE_SQR[txSz];
            if (set == 1) {
                int sym = t.sd.symbol(t.cdfs.intraTxTypeSet1, (sqr * 13 + intraDir) * 8, 7);
                type = AvifTables.TX_TYPE_INTRA_INV_SET1[sym];
            } else {
                int sym = t.sd.symbol(t.cdfs.intraTxTypeSet2, (sqr * 13 + intraDir) * 6, 5);
                type = AvifTables.TX_TYPE_INTRA_INV_SET2[sym];
            }
        }
        int w4 = AvifTables.TX_WIDTH[txSz] >> 2;
        int h4 = AvifTables.TX_HEIGHT[txSz] >> 2;
        // 변환 블록이 덮는 모든 4x4 위치에 변환 종류를 기록한다.
        for (int j = 0; j < h4; j++) {
            for (int i = 0; i < w4; i++) {
                t.frame.txType[(y4 + j) * t.frame.miStride + x4 + i] = (byte) type;
            }
        }
    }

    /**
     * Reads the coefficients of a transform block (<code>coeffs</code>).
     * <p>
     * 변환 블록의 계수를 읽는다(<code>coeffs</code>).
     *
     * @param plane  the plane / 평면
     * @param startX the x position of the block in samples / 블록의 x 위치(샘플)
     * @param startY the y position of the block in samples / 블록의 y 위치(샘플)
     * @param txSz   the transform size / 변환 크기
     * @return the end of block position (0 if all coefficients are zero); the levels are in {@link #quant} / 끝 위치(계수가 모두 0 이면 0). 수준은 {@link #quant} 에 있다
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터면 CORRUPT
     */
    int coeffs(int plane, int startX, int startY, int txSz) throws MediaException {
        Av1Cdfs cdfs = t.cdfs;
        Av1SymbolDecoder sd = t.sd;
        Av1Frame f = t.frame;
        int x4 = startX >> 2;
        int y4 = startY >> 2;
        int w4 = AvifTables.TX_WIDTH[txSz] >> 2;
        int h4 = AvifTables.TX_HEIGHT[txSz] >> 2;
        int txSzCtx = (AvifTables.TX_SIZE_SQR[txSz] + AvifTables.TX_SIZE_SQR_UP[txSz] + 1) >> 1;
        int ptype = plane > 0 ? 1 : 0;
        int segEob = (txSz == 17 || txSz == 18) ? 512 : Math.min(1024, AvifTables.TX_WIDTH[txSz] * AvifTables.TX_HEIGHT[txSz]);
        java.util.Arrays.fill(quant, 0, segEob, 0);
        int eob = 0;
        int culLevel = 0;
        int dcCategory = 0;
        int subX = plane > 0 ? f.seq.subsamplingX : 0;
        int subY = plane > 0 ? f.seq.subsamplingY : 0;
        int maxX4 = f.miCols >> subX;
        int maxY4 = f.miRows >> subY;
        int ctx = allZeroContext(plane, x4, y4, w4, h4, txSz, maxX4, maxY4);
        int allZero = sd.symbol(cdfs.txbSkip, (txSzCtx * 13 + ctx) * 3, 2);
        if (allZero != 0) {
            if (plane == 0) {
                // 휘도 변환 블록 영역의 변환 종류를 DCT_DCT 로 기록한다.
                for (int j = 0; j < h4; j++) {
                    for (int i = 0; i < w4; i++) {
                        f.txType[(y4 + j) * f.miStride + x4 + i] = DCT_DCT;
                    }
                }
            }
        } else {
            if (plane == 0) {
                readTransformType(x4, y4, txSz);
            }
            planeTxType = computeTxType(plane, txSz, x4, y4);
            int[] scan = scan(txSz, planeTxType);
            int txClass = txClass(planeTxType);
            int eobMultisize = Math.min(AvifTables.TX_WIDTH_LOG2[txSz], 5) + Math.min(AvifTables.TX_HEIGHT_LOG2[txSz], 5) - 4;
            int eobCtx = txClass == TX_CLASS_2D ? 0 : 1;
            int eobPt;
            switch (eobMultisize) {
            case 0:
                eobPt = sd.symbol(cdfs.eobPt16, (ptype * 2 + eobCtx) * 6, 5) + 1;
                break;
            case 1:
                eobPt = sd.symbol(cdfs.eobPt32, (ptype * 2 + eobCtx) * 7, 6) + 1;
                break;
            case 2:
                eobPt = sd.symbol(cdfs.eobPt64, (ptype * 2 + eobCtx) * 8, 7) + 1;
                break;
            case 3:
                eobPt = sd.symbol(cdfs.eobPt128, (ptype * 2 + eobCtx) * 9, 8) + 1;
                break;
            case 4:
                eobPt = sd.symbol(cdfs.eobPt256, (ptype * 2 + eobCtx) * 10, 9) + 1;
                break;
            case 5:
                eobPt = sd.symbol(cdfs.eobPt512, ptype * 11, 10) + 1;
                break;
            default:
                eobPt = sd.symbol(cdfs.eobPt1024, ptype * 12, 11) + 1;
                break;
            }
            eob = eobPt < 2 ? eobPt : ((1 << (eobPt - 2)) + 1);
            int eobShift = Math.max(-1, eobPt - 3);
            if (eobShift >= 0) {
                int extra = sd.symbol(cdfs.eobExtra, ((txSzCtx * 2 + ptype) * 9 + (eobPt - 3)) * 3, 2);
                if (extra != 0) {
                    eob += 1 << eobShift;
                }
                // 끝 위치의 나머지 비트를 하나씩 읽어 더한다.
                for (int i = 1; i < Math.max(0, eobPt - 2); i++) {
                    eobShift = Math.max(0, eobPt - 2) - 1 - i;
                    if (sd.literal(1) != 0) {
                        eob += 1 << eobShift;
                    }
                }
            }
            int adj = AvifTables.ADJUSTED_TX_SIZE[txSz];
            int bwl = AvifTables.TX_WIDTH_LOG2[adj];
            int width = 1 << bwl;
            int height = AvifTables.TX_HEIGHT[adj];
            // 마지막 위치부터 처음 위치까지 계수 수준을 읽는다.
            for (int c = eob - 1; c >= 0; c--) {
                int pos = scan[c];
                int level;
                if (c == eob - 1) {
                    int ectx;
                    if (c == 0) {
                        ectx = 0;
                    } else if (c <= (height << bwl) / 8) {
                        ectx = 1;
                    } else if (c <= (height << bwl) / 4) {
                        ectx = 2;
                    } else {
                        ectx = 3;
                    }
                    level = sd.symbol(cdfs.coeffBaseEob, ((txSzCtx * 2 + ptype) * 4 + ectx) * 4, 3) + 1;
                } else {
                    int bctx = baseContext(txSz, txClass, pos, bwl, width, height);
                    level = sd.symbol(cdfs.coeffBase, ((txSzCtx * 2 + ptype) * 42 + bctx) * 5, 4);
                }
                if (level > 2) {
                    int rctx = rangeContext(txClass, pos, bwl, height);
                    int off = ((Math.min(txSzCtx, 3) * 2 + ptype) * 21 + rctx) * 5;
                    // 기본 수준을 넘는 부분을 최대 4번 읽는다.
                    for (int idx = 0; idx < 4; idx++) {
                        int br = sd.symbol(cdfs.coeffBr, off, 4);
                        level += br;
                        if (br < 3) {
                            break;
                        }
                    }
                }
                quant[pos] = level;
            }
            // 처음 위치부터 부호와 큰 수준의 나머지를 읽는다.
            for (int c = 0; c < eob; c++) {
                int pos = scan[c];
                int sign = 0;
                if (quant[pos] != 0) {
                    if (c == 0) {
                        sign = sd.symbol(cdfs.dcSign, (ptype * 3 + dcSignContext(plane, x4, y4, w4, h4, maxX4, maxY4)) * 3, 2);
                    } else {
                        sign = sd.literal(1);
                    }
                }
                if (quant[pos] > 14) {
                    int length = 0;
                    int bit;
                    // 골롬 부호의 길이를 1 이 나올 때까지 읽는다.
                    do {
                        length++;
                        bit = sd.literal(1);
                        if (length > 32) {
                            throw new MediaException(MediaException.Kind.CORRUPT, "AV1: the Golomb code is too long");
                        }
                    } while (bit == 0);
                    int x = 1;
                    // 골롬 부호의 데이터 비트를 읽어 값을 만든다.
                    for (int i = length - 2; i >= 0; i--) {
                        x = (x << 1) | sd.literal(1);
                    }
                    quant[pos] = x + 14;
                }
                if (pos == 0 && quant[pos] > 0) {
                    dcCategory = sign != 0 ? 1 : 2;
                }
                quant[pos] &= 0xFFFFF;
                culLevel += quant[pos];
                if (sign != 0) {
                    quant[pos] = -quant[pos];
                }
            }
            culLevel = Math.min(63, culLevel);
        }
        // 위쪽과 왼쪽 문맥에 이 블록의 결과를 기록한다.
        for (int i = 0; i < w4; i++) {
            aboveLevel[plane][x4 + i] = culLevel;
            aboveDc[plane][x4 + i] = dcCategory;
        }
        // 왼쪽 문맥에 이 블록의 결과를 기록한다.
        for (int i = 0; i < h4; i++) {
            leftLevel[plane][y4 + i] = culLevel;
            leftDc[plane][y4 + i] = dcCategory;
        }
        return eob;
    }

    /**
     * The context of the all_zero symbol.
     * <p>
     * all_zero 심볼의 문맥.
     *
     * @param plane the plane / 평면
     * @param x4    the x position in 4x4 units / 4x4 단위 x 위치
     * @param y4    the y position in 4x4 units / 4x4 단위 y 위치
     * @param w4    the width in 4x4 units / 4x4 단위 너비
     * @param h4    the height in 4x4 units / 4x4 단위 높이
     * @param txSz  the transform size / 변환 크기
     * @param maxX4 the number of 4x4 columns of the plane / 평면의 4x4 열 수
     * @param maxY4 the number of 4x4 rows of the plane / 평면의 4x4 행 수
     * @return the context / 문맥
     */
    private int allZeroContext(int plane, int x4, int y4, int w4, int h4, int txSz, int maxX4, int maxY4) {
        int w = AvifTables.TX_WIDTH[txSz];
        int h = AvifTables.TX_HEIGHT[txSz];
        int subX = plane > 0 ? t.frame.seq.subsamplingX : 0;
        int subY = plane > 0 ? t.frame.seq.subsamplingY : 0;
        int bsize = AvifTables.SUBSAMPLED_SIZE[t.miSize * 4 + subX * 2 + subY];
        int bw = AvifTables.NUM_4X4_BLOCKS_WIDE[bsize] * 4;
        int bh = AvifTables.NUM_4X4_BLOCKS_HIGH[bsize] * 4;
        int ctx;
        if (plane == 0) {
            int top = 0;
            int left = 0;
            // 변환 블록 너비만큼 위쪽 수준 문맥의 최대값을 구한다.
            for (int k = 0; k < w4; k++) {
                if (x4 + k < maxX4) {
                    top = Math.max(top, aboveLevel[plane][x4 + k]);
                }
            }
            // 변환 블록 높이만큼 왼쪽 수준 문맥의 최대값을 구한다.
            for (int k = 0; k < h4; k++) {
                if (y4 + k < maxY4) {
                    left = Math.max(left, leftLevel[plane][y4 + k]);
                }
            }
            top = Math.min(top, 255);
            left = Math.min(left, 255);
            if (bw == w && bh == h) {
                ctx = 0;
            } else if (top == 0 && left == 0) {
                ctx = 1;
            } else if (top == 0 || left == 0) {
                ctx = 2 + (Math.max(top, left) > 3 ? 1 : 0);
            } else if (Math.max(top, left) <= 3) {
                ctx = 4;
            } else if (Math.min(top, left) <= 3) {
                ctx = 5;
            } else {
                ctx = 6;
            }
        } else {
            int above = 0;
            int left = 0;
            // 색차 평면은 수준과 DC 문맥을 합쳐서 0 인지만 본다.
            for (int i = 0; i < w4; i++) {
                if (x4 + i < maxX4) {
                    above |= aboveLevel[plane][x4 + i];
                    above |= aboveDc[plane][x4 + i];
                }
            }
            // 변환 블록 높이만큼 왼쪽 문맥을 모은다.
            for (int i = 0; i < h4; i++) {
                if (y4 + i < maxY4) {
                    left |= leftLevel[plane][y4 + i];
                    left |= leftDc[plane][y4 + i];
                }
            }
            ctx = (above != 0 ? 1 : 0) + (left != 0 ? 1 : 0);
            ctx += 7;
            if (bw * bh > w * h) {
                ctx += 3;
            }
        }
        return ctx;
    }

    /**
     * The context of the dc_sign symbol.
     * <p>
     * dc_sign 심볼의 문맥.
     *
     * @param plane the plane / 평면
     * @param x4    the x position in 4x4 units / 4x4 단위 x 위치
     * @param y4    the y position in 4x4 units / 4x4 단위 y 위치
     * @param w4    the width in 4x4 units / 4x4 단위 너비
     * @param h4    the height in 4x4 units / 4x4 단위 높이
     * @param maxX4 the number of 4x4 columns of the plane / 평면의 4x4 열 수
     * @param maxY4 the number of 4x4 rows of the plane / 평면의 4x4 행 수
     * @return the context / 문맥
     */
    private int dcSignContext(int plane, int x4, int y4, int w4, int h4, int maxX4, int maxY4) {
        int dcSign = 0;
        // 위쪽 문맥의 DC 부호를 모두 더한다.
        for (int k = 0; k < w4; k++) {
            if (x4 + k < maxX4) {
                int sign = aboveDc[plane][x4 + k];
                if (sign == 1) {
                    dcSign--;
                } else if (sign == 2) {
                    dcSign++;
                }
            }
        }
        // 왼쪽 문맥의 DC 부호를 모두 더한다.
        for (int k = 0; k < h4; k++) {
            if (y4 + k < maxY4) {
                int sign = leftDc[plane][y4 + k];
                if (sign == 1) {
                    dcSign--;
                } else if (sign == 2) {
                    dcSign++;
                }
            }
        }
        if (dcSign < 0) {
            return 1;
        }
        return dcSign > 0 ? 2 : 0;
    }

    /**
     * The context of the coeff_base symbol.
     * <p>
     * coeff_base 심볼의 문맥.
     *
     * @param txSz    the transform size / 변환 크기
     * @param txClass the transform class / 변환 분류
     * @param pos     the position of the coefficient / 계수 위치
     * @param bwl     the base 2 logarithm of the adjusted width / 조정된 너비의 밑 2 로그
     * @param width   the adjusted width / 조정된 너비
     * @param height  the adjusted height / 조정된 높이
     * @return the context / 문맥
     */
    private int baseContext(int txSz, int txClass, int pos, int bwl, int width, int height) {
        int row = pos >> bwl;
        int col = pos - (row << bwl);
        int mag = 0;
        int[] offs = AvifTables.SIG_REF_DIFF_OFFSET;
        // 이미 읽은 이웃 다섯 개의 수준(최대 3)을 더한다.
        for (int idx = 0; idx < 5; idx++) {
            int refRow = row + offs[txClass * 10 + idx * 2];
            int refCol = col + offs[txClass * 10 + idx * 2 + 1];
            if (refRow >= 0 && refCol >= 0 && refRow < height && refCol < width) {
                mag += Math.min(Math.abs(quant[(refRow << bwl) + refCol]), 3);
            }
        }
        int ctx = Math.min((mag + 1) >> 1, 4);
        if (txClass == TX_CLASS_2D) {
            if (row == 0 && col == 0) {
                return 0;
            }
            return ctx + AvifTables.COEFF_BASE_CTX_OFFSET[txSz * 25 + Math.min(row, 4) * 5 + Math.min(col, 4)];
        }
        int idx = txClass == TX_CLASS_VERT ? row : col;
        return ctx + AvifTables.COEFF_BASE_POS_CTX_OFFSET[Math.min(idx, 2)];
    }

    /**
     * The context of the coeff_br symbol.
     * <p>
     * coeff_br 심볼의 문맥.
     *
     * @param txClass the transform class / 변환 분류
     * @param pos     the position of the coefficient / 계수 위치
     * @param bwl     the base 2 logarithm of the adjusted width / 조정된 너비의 밑 2 로그
     * @param txh     the adjusted height / 조정된 높이
     * @return the context / 문맥
     */
    private int rangeContext(int txClass, int pos, int bwl, int txh) {
        int txw = 1 << bwl;
        int row = pos >> bwl;
        int col = pos - (row << bwl);
        int mag = 0;
        int[] offs = AvifTables.MAG_REF_OFFSET_WITH_TX_CLASS;
        // 이미 읽은 이웃 세 개의 수준(최대 15)을 더한다.
        for (int idx = 0; idx < 3; idx++) {
            int refRow = row + offs[txClass * 6 + idx * 2];
            int refCol = col + offs[txClass * 6 + idx * 2 + 1];
            if (refRow >= 0 && refCol >= 0 && refRow < txh && refCol < txw) {
                mag += Math.min(quant[refRow * txw + refCol], 15);
            }
        }
        mag = Math.min((mag + 1) >> 1, 6);
        if (pos == 0) {
            return mag;
        }
        if (txClass == TX_CLASS_2D) {
            return (row < 2 && col < 2) ? mag + 7 : mag + 14;
        }
        if (txClass == TX_CLASS_HORIZ) {
            return col == 0 ? mag + 7 : mag + 14;
        }
        return row == 0 ? mag + 7 : mag + 14;
    }
}
