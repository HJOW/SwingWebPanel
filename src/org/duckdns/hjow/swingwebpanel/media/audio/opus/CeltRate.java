package org.duckdns.hjow.swingwebpanel.media.audio.opus;

/**
 * Bit allocation of a CELT frame (RFC 6716 section 4.3.3, <code>celt/rate.c</code>): it turns the total number of bits, the
 * allocation trim, the band boosts and the caps into the number of pulses and fine energy bits of every band, and decodes the
 * band skipping, the intensity stereo start and the dual stereo flag. The decoding is bit exact: a different result would
 * desynchronize the range decoder. One instance holds the outputs of the last allocation.
 * <p>
 * CELT 프레임의 비트 할당(RFC 6716 4.3.3, <code>celt/rate.c</code>): 전체 비트 수, 할당 조정, 대역 증폭, 상한으로부터 대역마다 펄스 수와
 * 미세 에너지 비트 수를 구하고, 대역 건너뛰기, 강도 스테레오 시작 대역, 이중 스테레오 플래그를 디코딩한다. 비트 단위로 정확해야 하며
 * 결과가 다르면 범위 디코더가 어긋난다. 인스턴스는 마지막 할당의 출력을 보관한다.
 */
final class CeltRate {

    /**
     * Fractional bits of the bit counts (1/8 bit units).
     * <p>
     * 비트 수의 소수 비트(1/8 비트 단위).
     */
    static final int BITRES = 3;

    /**
     * Number of pulse count steps in the pseudo-pulse numbering.
     * <p>
     * 의사 펄스 번호 매기기의 단계 수.
     */
    private static final int LOG_MAX_PSEUDO = 6;

    /**
     * Largest number of fine energy bits.
     * <p>
     * 미세 에너지 비트의 최대 수.
     */
    private static final int MAX_FINE_BITS = 8;

    /**
     * Fine energy offset constant.
     * <p>
     * 미세 에너지 오프셋 상수.
     */
    private static final int FINE_OFFSET = 21;

    /**
     * Number of steps of the interpolation search.
     * <p>
     * 보간 탐색의 단계 수.
     */
    private static final int ALLOC_STEPS = 6;

    /**
     * Cost in 1/8 bits of coding the intensity start band for a number of bands.
     * <p>
     * 대역 수에 따라 강도 시작 대역을 부호화하는 비용(1/8 비트 단위).
     */
    private static final int[] LOG2_FRAC_TABLE = { 0, 8, 13, 16, 19, 21, 23, 24, 26, 27, 28, 29, 30, 31, 32, 32, 33, 34, 34, 35, 36, 36, 37, 37 };

    /**
     * Intensity stereo start band chosen by the last allocation.
     * <p>
     * 마지막 할당이 정한 강도 스테레오 시작 대역.
     */
    int intensity;

    /**
     * Dual stereo flag of the last allocation.
     * <p>
     * 마지막 할당의 이중 스테레오 플래그.
     */
    boolean dualStereo;

    /**
     * Balance (unused bits) left over by the last allocation.
     * <p>
     * 마지막 할당이 남긴 균형(쓰지 않은 비트).
     */
    int balance;

    /**
     * Creates the allocator.
     * <p>
     * 할당기를 만든다.
     */
    CeltRate() {
    }

    /**
     * Converts a pseudo pulse index into the number of pulses.
     * <p>
     * 의사 펄스 번호를 펄스 수로 바꾼다.
     *
     * @param i pseudo index / 의사 번호
     * @return the number of pulses / 펄스 수
     */
    static int getPulses(int i) {
        return i < 8 ? i : (8 + (i & 7)) << ((i >> 3) - 1);
    }

    /**
     * Finds the pulse count whose cost is closest to a bit budget.
     * <p>
     * 비용이 비트 예산에 가장 가까운 펄스 수를 찾는다.
     *
     * @param band band / 대역
     * @param lm   log2 of the frame size / 프레임 크기의 log2
     * @param bitsIn budget in 1/8 bits / 1/8 비트 단위 예산
     * @return pseudo pulse index / 의사 펄스 번호
     */
    static int bits2pulses(int band, int lm, int bitsIn) {
        int base = CeltTables.CACHE_INDEX[(lm + 1) * CeltEnergy.NB_BANDS + band];
        int lo = 0;
        int hi = CeltTables.CACHE_BITS[base];
        int bits = bitsIn - 1;
        // 이진 탐색으로 예산에 가까운 펄스 수를 좁힌다.
        for (int i = 0; i < LOG_MAX_PSEUDO; i++) {
            int mid = (lo + hi + 1) >> 1;
            if (CeltTables.CACHE_BITS[base + mid] >= bits) {
                hi = mid;
            } else {
                lo = mid;
            }
        }
        int loBits = lo == 0 ? -1 : CeltTables.CACHE_BITS[base + lo];
        if (bits - loBits <= CeltTables.CACHE_BITS[base + hi] - bits) {
            return lo;
        }
        return hi;
    }

    /**
     * Cost in 1/8 bits of a pulse count.
     * <p>
     * 펄스 수의 비용(1/8 비트 단위).
     *
     * @param band   band / 대역
     * @param lm     log2 of the frame size / 프레임 크기의 log2
     * @param pulses pseudo pulse index / 의사 펄스 번호
     * @return cost / 비용
     */
    static int pulses2bits(int band, int lm, int pulses) {
        int base = CeltTables.CACHE_INDEX[(lm + 1) * CeltEnergy.NB_BANDS + band];
        return pulses == 0 ? 0 : CeltTables.CACHE_BITS[base + pulses] + 1;
    }

    /**
     * Computes the allocation of a frame and decodes its side information.
     * <p>
     * 프레임의 할당을 계산하고 부가 정보를 디코딩한다.
     *
     * @param start        first band / 첫 대역
     * @param end          one past the last band / 마지막 대역의 다음
     * @param offsets      band boosts / 대역 증폭
     * @param cap          band caps / 대역 상한
     * @param allocTrim    allocation trim / 할당 조정
     * @param totalIn      bits available in 1/8 bits / 쓸 수 있는 비트(1/8 비트 단위)
     * @param pulses       receives the pulse bits of each band / 대역마다 펄스 비트를 받는 배열
     * @param ebits        receives the fine energy bits / 미세 에너지 비트를 받는 배열
     * @param finePriority receives the fine priorities / 미세 우선순위를 받는 배열
     * @param channels     1 or 2 / 1 또는 2
     * @param lm           log2 of the frame size / 프레임 크기의 log2
     * @param rc           the range decoder / 범위 디코더
     * @return the number of coded bands / 부호화된 대역 수
     */
    int computeAllocation(int start, int end, int[] offsets, int[] cap, int allocTrim, int totalIn, int[] pulses, int[] ebits, int[] finePriority,
            int channels, int lm, OpusRangeDecoder rc) {
        int total = Math.max(totalIn, 0);
        final int len = CeltEnergy.NB_BANDS;
        int skipStart = start;
        int skipRsv = total >= 1 << BITRES ? 1 << BITRES : 0;
        total -= skipRsv;
        int intensityRsv = 0;
        int dualStereoRsv = 0;
        // 스테레오이면 강도 시작 대역과 이중 스테레오 비트를 예약한다.
        if (channels == 2) {
            intensityRsv = LOG2_FRAC_TABLE[end - start];
            if (intensityRsv > total) {
                intensityRsv = 0;
            } else {
                total -= intensityRsv;
                dualStereoRsv = total >= 1 << BITRES ? 1 << BITRES : 0;
                total -= dualStereoRsv;
            }
        }
        int[] bits1 = new int[len];
        int[] bits2 = new int[len];
        int[] thresh = new int[len];
        int[] trimOffset = new int[len];
        int[] eb = CeltTables.EBANDS;
        // 대역마다 최소 임계값과 할당 조정에 따른 오프셋을 계산한다.
        for (int j = start; j < end; j++) {
            thresh[j] = Math.max(channels << BITRES, (3 * (eb[j + 1] - eb[j]) << lm << BITRES) >> 4);
            trimOffset[j] = channels * (eb[j + 1] - eb[j]) * (allocTrim - 5 - lm) * (end - j - 1) * (1 << (lm + BITRES)) >> 6;
            if (((eb[j + 1] - eb[j]) << lm) == 1) {
                trimOffset[j] -= channels << BITRES;
            }
        }
        int lo = 1;
        int hi = 10;
        // 할당 벡터 사이를 이진 탐색해 예산에 맞는 두 벡터를 찾는다.
        do {
            boolean done = false;
            int psum = 0;
            int mid = (lo + hi) >> 1;
            // 항목마다 반복해서 처리한다.
            for (int j = end; j-- > start;) {
                int n = eb[j + 1] - eb[j];
                int bitsj = channels * n * CeltTables.ALLOC_VECTORS[mid * len + j] << lm >> 2;
                if (bitsj > 0) {
                    bitsj = Math.max(0, bitsj + trimOffset[j]);
                }
                bitsj += offsets[j];
                if (bitsj >= thresh[j] || done) {
                    done = true;
                    psum += Math.min(bitsj, cap[j]);
                } else if (bitsj >= channels << BITRES) {
                    psum += channels << BITRES;
                }
            }
            if (psum > total) {
                hi = mid - 1;
            } else {
                lo = mid + 1;
            }
        } while (lo <= hi);
        hi = lo--;
        // 두 벡터의 비트와 그 차이를 대역마다 구한다.
        for (int j = start; j < end; j++) {
            int n = eb[j + 1] - eb[j];
            int bits1j = channels * n * CeltTables.ALLOC_VECTORS[lo * len + j] << lm >> 2;
            int bits2j = hi >= 11 ? cap[j] : channels * n * CeltTables.ALLOC_VECTORS[hi * len + j] << lm >> 2;
            if (bits1j > 0) {
                bits1j = Math.max(0, bits1j + trimOffset[j]);
            }
            if (bits2j > 0) {
                bits2j = Math.max(0, bits2j + trimOffset[j]);
            }
            if (lo > 0) {
                bits1j += offsets[j];
            }
            bits2j += offsets[j];
            if (offsets[j] > 0) {
                skipStart = j;
            }
            bits2j = Math.max(0, bits2j - bits1j);
            bits1[j] = bits1j;
            bits2[j] = bits2j;
        }
        return interpBits2pulses(start, end, skipStart, bits1, bits2, thresh, cap, total, skipRsv, intensityRsv, dualStereoRsv, pulses, ebits,
                finePriority, channels, lm, rc);
    }

    /**
     * Interpolates between two allocation vectors and decodes the skip, intensity and dual stereo information.
     * <p>
     * 두 할당 벡터 사이를 보간하고 건너뛰기, 강도, 이중 스테레오 정보를 디코딩한다.
     *
     * @param start         first band / 첫 대역
     * @param end           one past the last band / 마지막 대역의 다음
     * @param skipStart     first band that may be skipped / 건너뛸 수 있는 첫 대역
     * @param bits1         lower vector / 낮은 벡터
     * @param bits2         difference to the upper vector / 높은 벡터와의 차
     * @param thresh        minimum bits per band / 대역별 최소 비트
     * @param cap           caps / 상한
     * @param totalIn       total bits / 전체 비트
     * @param skipRsv       bits reserved for the skip flag / 건너뛰기 플래그용 예약 비트
     * @param intensityRsvIn bits reserved for the intensity / 강도용 예약 비트
     * @param dualStereoRsvIn bits reserved for the dual stereo flag / 이중 스테레오 플래그용 예약 비트
     * @param bits          receives the pulse bits / 펄스 비트를 받는 배열
     * @param ebits         receives the fine bits / 미세 비트를 받는 배열
     * @param finePriority  receives the fine priorities / 미세 우선순위를 받는 배열
     * @param channels      1 or 2 / 1 또는 2
     * @param lm            log2 of the frame size / 프레임 크기의 log2
     * @param rc            the range decoder / 범위 디코더
     * @return the number of coded bands / 부호화된 대역 수
     */
    private int interpBits2pulses(int start, int end, int skipStart, int[] bits1, int[] bits2, int[] thresh, int[] cap, int totalIn, int skipRsv,
            int intensityRsvIn, int dualStereoRsvIn, int[] bits, int[] ebits, int[] finePriority, int channels, int lm, OpusRangeDecoder rc) {
        int total = totalIn;
        int intensityRsv = intensityRsvIn;
        int dualStereoRsv = dualStereoRsvIn;
        int[] eb = CeltTables.EBANDS;
        int allocFloor = channels << BITRES;
        int stereo = channels > 1 ? 1 : 0;
        int logM = lm << BITRES;
        int lo = 0;
        int hi = 1 << ALLOC_STEPS;
        // 두 벡터 사이의 보간 비율을 이진 탐색으로 정한다.
        for (int i = 0; i < ALLOC_STEPS; i++) {
            int mid = (lo + hi) >> 1;
            int psum = 0;
            boolean done = false;
            // 항목마다 반복해서 처리한다.
            for (int j = end; j-- > start;) {
                int tmp = bits1[j] + (mid * bits2[j] >> ALLOC_STEPS);
                if (tmp >= thresh[j] || done) {
                    done = true;
                    psum += Math.min(tmp, cap[j]);
                } else if (tmp >= allocFloor) {
                    psum += allocFloor;
                }
            }
            if (psum > total) {
                hi = mid;
            } else {
                lo = mid;
            }
        }
        int psum = 0;
        boolean done = false;
        // 정한 비율로 대역마다 비트를 배정한다.
        for (int j = end; j-- > start;) {
            int tmp = bits1[j] + (lo * bits2[j] >> ALLOC_STEPS);
            if (tmp < thresh[j] && !done) {
                if (tmp >= allocFloor) {
                    tmp = allocFloor;
                } else {
                    tmp = 0;
                }
            } else {
                done = true;
            }
            tmp = Math.min(tmp, cap[j]);
            bits[j] = tmp;
            psum += tmp;
        }
        int codedBands;
        // 높은 대역부터 건너뛸 수 있는지 플래그를 읽으며 부호화할 대역 수를 줄인다.
        for (codedBands = end;; codedBands--) {
            int j = codedBands - 1;
            if (j <= skipStart) {
                total += skipRsv;
                break;
            }
            int left = total - psum;
            int percoeff = left / (eb[codedBands] - eb[start]);
            left -= (eb[codedBands] - eb[start]) * percoeff;
            int rem = Math.max(left - (eb[j] - eb[start]), 0);
            int bandWidth = eb[codedBands] - eb[j];
            int bandBits = bits[j] + percoeff * bandWidth + rem;
            if (bandBits >= Math.max(thresh[j], allocFloor + (1 << BITRES))) {
                if (rc.bitLogp(1) != 0) {
                    break;
                }
                psum += 1 << BITRES;
                bandBits -= 1 << BITRES;
            }
            psum -= bits[j] + intensityRsv;
            if (intensityRsv > 0) {
                intensityRsv = LOG2_FRAC_TABLE[j - start];
            }
            psum += intensityRsv;
            if (bandBits >= allocFloor) {
                psum += allocFloor;
                bits[j] = allocFloor;
            } else {
                bits[j] = 0;
            }
        }
        if (intensityRsv > 0) {
            intensity = start + (int) rc.readUint(codedBands + 1 - start);
        } else {
            intensity = 0;
        }
        if (intensity <= start) {
            total += dualStereoRsv;
            dualStereoRsv = 0;
        }
        if (dualStereoRsv > 0) {
            dualStereo = rc.bitLogp(1) != 0;
        } else {
            dualStereo = false;
        }
        int left = total - psum;
        int percoeff = left / (eb[codedBands] - eb[start]);
        left -= (eb[codedBands] - eb[start]) * percoeff;
        // 남은 비트를 계수 수에 비례해 각 대역에 더한다.
        for (int j = start; j < codedBands; j++) {
            bits[j] += percoeff * (eb[j + 1] - eb[j]);
        }
        // 나머지는 낮은 대역부터 한 계수씩 나눠 준다.
        for (int j = start; j < codedBands; j++) {
            int tmp = Math.min(left, eb[j + 1] - eb[j]);
            bits[j] += tmp;
            left -= tmp;
        }
        int bal = 0;
        int j;
        // 대역마다 미세 에너지 비트와 펄스 비트를 나누고 남은 비트를 다음 대역으로 넘긴다.
        for (j = start; j < codedBands; j++) {
            int n0 = eb[j + 1] - eb[j];
            int n = n0 << lm;
            bits[j] += bal;
            int excess;
            if (n > 1) {
                excess = Math.max(bits[j] - cap[j], 0);
                bits[j] -= excess;
                int den = channels * n + ((channels == 2 && n > 2 && !dualStereo && j < intensity) ? 1 : 0);
                int nclogn = den * (CeltTables.LOG_N[j] + logM);
                int offset = (nclogn >> 1) - den * FINE_OFFSET;
                if (n == 2) {
                    offset += den << BITRES >> 2;
                }
                if (bits[j] + offset < den * 2 << BITRES) {
                    offset += nclogn >> 2;
                } else if (bits[j] + offset < den * 3 << BITRES) {
                    offset += nclogn >> 3;
                }
                ebits[j] = Math.max(0, (bits[j] + offset + (den << (BITRES - 1))) / (den << BITRES));
                if (channels * ebits[j] > (bits[j] >> BITRES)) {
                    ebits[j] = bits[j] >> stereo >> BITRES;
                }
                ebits[j] = Math.min(ebits[j], MAX_FINE_BITS);
                finePriority[j] = ebits[j] * (den << BITRES) >= bits[j] + offset ? 1 : 0;
                bits[j] -= channels * ebits[j] << BITRES;
            } else {
                excess = Math.max(0, bits[j] - (channels << BITRES));
                bits[j] -= excess;
                ebits[j] = 0;
                finePriority[j] = 1;
            }
            if (excess > 0) {
                int extraFine = Math.min(excess >> (stereo + BITRES), MAX_FINE_BITS - ebits[j]);
                ebits[j] += extraFine;
                int extraBits = extraFine * channels << BITRES;
                finePriority[j] = extraBits >= excess - bal ? 1 : 0;
                excess -= extraBits;
            }
            bal = excess;
        }
        balance = bal;
        // 부호화하지 않는 대역은 비트를 모두 미세 에너지에 쓴다.
        for (; j < end; j++) {
            ebits[j] = bits[j] >> stereo >> BITRES;
            bits[j] = 0;
            finePriority[j] = ebits[j] < 1 ? 1 : 0;
        }
        return codedBands;
    }
}
