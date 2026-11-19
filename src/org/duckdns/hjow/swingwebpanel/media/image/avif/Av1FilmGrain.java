package org.duckdns.hjow.swingwebpanel.media.image.avif;

import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * The film grain of an AV1 frame (plan item P18-03): the parameters of <code>film_grain_params()</code> (AV1 Bitstream &amp; Decoding Process
 * Specification, section 5.9.30) and the film grain synthesis process (section 7.18.3), which adds pseudo-random noise to the output picture. The
 * noise is generated with a 16-bit shift register, an auto-regressive filter over a 73 x 82 luma and a (38 or 73) x (44 or 82) chroma grain
 * template, the template blocks of 32 x 32 luma samples are chosen with random offsets, overlapping blocks are blended, and the noise is scaled
 * by a piecewise linear function of the sample value before it is added; the arithmetic is the integer arithmetic of the specification, so the
 * result is the same as the output of the reference decoders bit for bit. The synthesis changes the output picture only (a reference frame keeps
 * its samples), so the decoder applies it to the finished picture. Only intra frames come through this decoder, so the parameters are never
 * inherited from a reference frame (<code>update_grain</code> is always 1).
 * <p>
 * AV1 프레임의 필름 그레인(계획 P18-03): <code>film_grain_params()</code>(AV1 Bitstream &amp; Decoding Process Specification 5.9.30절)의 매개변수와
 * 출력 화면에 의사 난수 잡음을 더하는 필름 그레인 합성 과정(7.18.3절). 잡음은 16비트 시프트 레지스터, 73 x 82 휘도와 (38 또는 73) x (44 또는 82) 색차
 * 그레인 견본에 대한 자기 회귀 필터로 만들고, 휘도 32 x 32 샘플 블록의 견본을 무작위 오프셋으로 고르고, 겹치는 블록은 섞고, 잡음은 샘플 값의
 * 구간별 선형 함수로 크기를 조절해 더한다. 연산은 규격의 정수 연산이므로 결과가 기준 디코더의 출력과 비트 단위로 같다. 합성은 출력 화면만
 * 바꾸므로(참조 프레임의 샘플은 그대로) 디코더가 완성된 화면에 적용한다. 이 디코더는 인트라 프레임만 다루므로 매개변수를 참조 프레임에서 물려받지
 * 않는다(<code>update_grain</code> 은 항상 1).
 */
final class Av1FilmGrain {

    /**
     * The seed of the pseudo-random number generator.
     * <p>
     * 의사 난수 생성기의 씨앗.
     */
    private int grainSeed;

    /**
     * The number of points of the scaling function of luma (up to 14).
     * <p>
     * 휘도 크기 조절 함수의 점 수(최대 14).
     */
    private int numYPoints;

    /**
     * The sample values of the points of the luma scaling function.
     * <p>
     * 휘도 크기 조절 함수의 점의 샘플 값.
     */
    private final int[] pointYValue = new int[16];

    /**
     * The scaling values of the points of the luma scaling function.
     * <p>
     * 휘도 크기 조절 함수의 점의 크기 값.
     */
    private final int[] pointYScaling = new int[16];

    /**
     * True if the chroma scaling functions are those of luma.
     * <p>
     * 색차 크기 조절 함수가 휘도의 것과 같으면 true.
     */
    private boolean chromaScalingFromLuma;

    /**
     * The number of points of the scaling function of Cb (up to 10).
     * <p>
     * Cb 크기 조절 함수의 점 수(최대 10).
     */
    private int numCbPoints;

    /**
     * The sample values of the points of the Cb scaling function.
     * <p>
     * Cb 크기 조절 함수의 점의 샘플 값.
     */
    private final int[] pointCbValue = new int[16];

    /**
     * The scaling values of the points of the Cb scaling function.
     * <p>
     * Cb 크기 조절 함수의 점의 크기 값.
     */
    private final int[] pointCbScaling = new int[16];

    /**
     * The number of points of the scaling function of Cr (up to 10).
     * <p>
     * Cr 크기 조절 함수의 점 수(최대 10).
     */
    private int numCrPoints;

    /**
     * The sample values of the points of the Cr scaling function.
     * <p>
     * Cr 크기 조절 함수의 점의 샘플 값.
     */
    private final int[] pointCrValue = new int[16];

    /**
     * The scaling values of the points of the Cr scaling function.
     * <p>
     * Cr 크기 조절 함수의 점의 크기 값.
     */
    private final int[] pointCrScaling = new int[16];

    /**
     * The shift of the scaling (grain_scaling_minus_8 plus 8).
     * <p>
     * 크기 조절의 시프트(grain_scaling_minus_8 더하기 8).
     */
    private int scalingShift;

    /**
     * The lag of the auto-regressive filter (0 to 3).
     * <p>
     * 자기 회귀 필터의 지연(0~3).
     */
    private int arCoeffLag;

    /**
     * The auto-regressive coefficients of luma plus 128.
     * <p>
     * 휘도의 자기 회귀 계수 더하기 128.
     */
    private final int[] arCoeffsY = new int[24];

    /**
     * The auto-regressive coefficients of Cb plus 128.
     * <p>
     * Cb 의 자기 회귀 계수 더하기 128.
     */
    private final int[] arCoeffsCb = new int[25];

    /**
     * The auto-regressive coefficients of Cr plus 128.
     * <p>
     * Cr 의 자기 회귀 계수 더하기 128.
     */
    private final int[] arCoeffsCr = new int[25];

    /**
     * The shift of the auto-regressive filter (ar_coeff_shift_minus_6 plus 6).
     * <p>
     * 자기 회귀 필터의 시프트(ar_coeff_shift_minus_6 더하기 6).
     */
    private int arCoeffShift;

    /**
     * The extra shift that lowers the amplitude of the grain.
     * <p>
     * 그레인의 진폭을 낮추는 추가 시프트.
     */
    private int grainScaleShift;

    /**
     * The multiplier of the Cb sample in the merge of Cb noise scaling (plus 128 stored).
     * <p>
     * Cb 잡음 크기 조절 병합에서 Cb 샘플의 곱(128 을 더해 저장).
     */
    private int cbMult;

    /**
     * The multiplier of the luma in the merge of Cb noise scaling (plus 128 stored).
     * <p>
     * Cb 잡음 크기 조절 병합에서 휘도의 곱(128 을 더해 저장).
     */
    private int cbLumaMult;

    /**
     * The offset of the merge of Cb noise scaling (plus 256 stored).
     * <p>
     * Cb 잡음 크기 조절 병합의 오프셋(256 을 더해 저장).
     */
    private int cbOffset;

    /**
     * The multiplier of the Cr sample in the merge of Cr noise scaling (plus 128 stored).
     * <p>
     * Cr 잡음 크기 조절 병합에서 Cr 샘플의 곱(128 을 더해 저장).
     */
    private int crMult;

    /**
     * The multiplier of the luma in the merge of Cr noise scaling (plus 128 stored).
     * <p>
     * Cr 잡음 크기 조절 병합에서 휘도의 곱(128 을 더해 저장).
     */
    private int crLumaMult;

    /**
     * The offset of the merge of Cr noise scaling (plus 256 stored).
     * <p>
     * Cr 잡음 크기 조절 병합의 오프셋(256 을 더해 저장).
     */
    private int crOffset;

    /**
     * True if neighbouring blocks of grain overlap and are blended.
     * <p>
     * 이웃한 그레인 블록이 겹쳐 섞이면 true.
     */
    private boolean overlap;

    /**
     * True if the output is clipped to the restricted (studio) range.
     * <p>
     * 출력을 제한(스튜디오) 범위로 자르면 true.
     */
    private boolean clipToRestrictedRange;

    /**
     * The state of the random number generator.
     * <p>
     * 난수 생성기의 상태.
     */
    private int random;

    /**
     * The grain template of luma (73 rows of 82 values).
     * <p>
     * 휘도의 그레인 견본(82개씩 73행).
     */
    private int[][] lumaGrain;

    /**
     * The grain template of Cb.
     * <p>
     * Cb 의 그레인 견본.
     */
    private int[][] cbGrain;

    /**
     * The grain template of Cr.
     * <p>
     * Cr 의 그레인 견본.
     */
    private int[][] crGrain;

    /**
     * The smallest and the largest value of the grain templates.
     * <p>
     * 그레인 견본 값의 최솟값과 최댓값.
     */
    private int grainMin;

    /**
     * The largest value of the grain templates.
     * <p>
     * 그레인 견본 값의 최댓값.
     */
    private int grainMax;

    /**
     * The scaling look-up tables of the three planes.
     * <p>
     * 세 평면의 크기 조절 룩업 표.
     */
    private final int[][] scalingLut = new int[3][257];

    /**
     * Creates an empty parameter set (use {@link #parse}).
     * <p>
     * 빈 매개변수 집합을 만든다({@link #parse} 를 쓴다).
     */
    private Av1FilmGrain() {
    }

    /**
     * Reads the body of <code>film_grain_params()</code> after <code>apply_grain</code> was found to be 1 (AV1 specification 5.9.30).
     * <p>
     * <code>apply_grain</code> 이 1 인 것을 확인한 뒤 <code>film_grain_params()</code> 의 나머지를 읽는다(AV1 규격 5.9.30절).
     *
     * @param r         the reader behind <code>apply_grain</code> / <code>apply_grain</code> 뒤의 읽기
     * @param seq       the sequence header / 시퀀스 헤더
     * @param interCode true for an inter frame (which may inherit the parameters of a reference frame) / 인터 프레임이면 true(참조 프레임의 매개변수를 물려받을 수 있다)
     * @param refs the reference slots (for the parameters that come from a reference frame) / 참조 슬롯(참조 프레임에서 오는 매개변수용)
     * @return the parameters, or null if a reference frame without film grain is named / 매개변수, 그레인이 없는 참조 프레임을 가리키면 null
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터이면 CORRUPT
     */
    static Av1FilmGrain parse(Av1BitReader r, Av1Sequence seq, boolean interCode, Av1RefFrame[] refs) throws MediaException {
        Av1FilmGrain g = new Av1FilmGrain();
        g.grainSeed = r.bits(16);
        if (interCode && !r.flag()) {
            // 새 시드와 함께 참조 프레임의 나머지 매개변수를 쓴다(그 프레임이 그레인이 없으면 이 프레임도 없다).
            Av1RefFrame ref = refs[r.bits(3)];
            if (ref == null || ref.filmGrain == null) {
                return null;
            }
            return ref.filmGrain.withSeed(g.grainSeed);
        }
        g.numYPoints = r.bits(4);
        if (g.numYPoints > 14) {
            throw new MediaException(MediaException.Kind.CORRUPT, "AV1: too many luma points of the film grain");
        }
        // 휘도 크기 조절 함수의 점을 읽는다.
        for (int i = 0; i < g.numYPoints; i++) {
            g.pointYValue[i] = r.bits(8);
            g.pointYScaling[i] = r.bits(8);
        }
        boolean mono = seq.monoChrome;
        g.chromaScalingFromLuma = mono ? false : r.flag();
        if (mono || g.chromaScalingFromLuma || (seq.subsamplingX == 1 && seq.subsamplingY == 1 && g.numYPoints == 0)) {
            g.numCbPoints = 0;
            g.numCrPoints = 0;
        } else {
            g.numCbPoints = r.bits(4);
            if (g.numCbPoints > 10) {
                throw new MediaException(MediaException.Kind.CORRUPT, "AV1: too many Cb points of the film grain");
            }
            // Cb 크기 조절 함수의 점을 읽는다.
            for (int i = 0; i < g.numCbPoints; i++) {
                g.pointCbValue[i] = r.bits(8);
                g.pointCbScaling[i] = r.bits(8);
            }
            g.numCrPoints = r.bits(4);
            if (g.numCrPoints > 10) {
                throw new MediaException(MediaException.Kind.CORRUPT, "AV1: too many Cr points of the film grain");
            }
            // Cr 크기 조절 함수의 점을 읽는다.
            for (int i = 0; i < g.numCrPoints; i++) {
                g.pointCrValue[i] = r.bits(8);
                g.pointCrScaling[i] = r.bits(8);
            }
        }
        // 크기 조절 함수의 점은 값이 엄격히 증가해야 한다(규격의 적합성 요건). 아니면 보간에서 0 으로 나누게 된다.
        g.checkIncreasing(g.pointYValue, g.numYPoints);
        g.checkIncreasing(g.pointCbValue, g.numCbPoints);
        g.checkIncreasing(g.pointCrValue, g.numCrPoints);
        g.scalingShift = r.bits(2) + 8;
        g.arCoeffLag = r.bits(2);
        int numPosLuma = 2 * g.arCoeffLag * (g.arCoeffLag + 1);
        int numPosChroma;
        if (g.numYPoints > 0) {
            numPosChroma = numPosLuma + 1;
            // 휘도 자기 회귀 계수를 읽는다.
            for (int i = 0; i < numPosLuma; i++) {
                g.arCoeffsY[i] = r.bits(8);
            }
        } else {
            numPosChroma = numPosLuma;
        }
        if (g.chromaScalingFromLuma || g.numCbPoints > 0) {
            // Cb 자기 회귀 계수를 읽는다.
            for (int i = 0; i < numPosChroma; i++) {
                g.arCoeffsCb[i] = r.bits(8);
            }
        }
        if (g.chromaScalingFromLuma || g.numCrPoints > 0) {
            // Cr 자기 회귀 계수를 읽는다.
            for (int i = 0; i < numPosChroma; i++) {
                g.arCoeffsCr[i] = r.bits(8);
            }
        }
        g.arCoeffShift = r.bits(2) + 6;
        g.grainScaleShift = r.bits(2);
        if (g.numCbPoints > 0) {
            g.cbMult = r.bits(8);
            g.cbLumaMult = r.bits(8);
            g.cbOffset = r.bits(9);
        }
        if (g.numCrPoints > 0) {
            g.crMult = r.bits(8);
            g.crLumaMult = r.bits(8);
            g.crOffset = r.bits(9);
        }
        g.overlap = r.flag();
        g.clipToRestrictedRange = r.flag();
        return g;
    }

    /**
     * Checks that the values of the points of a scaling function increase strictly.
     * <p>
     * 크기 조절 함수의 점 값이 엄격히 증가하는지 확인한다.
     *
     * @param values the values of the points / 점의 값
     * @param count  the number of points / 점의 수
     * @throws MediaException CORRUPT if the values do not increase / 값이 증가하지 않으면 CORRUPT
     */
    private void checkIncreasing(int[] values, int count) throws MediaException {
        // 이웃한 두 점의 값을 비교한다.
        for (int i = 1; i < count; i++) {
            if (values[i] <= values[i - 1]) {
                throw new MediaException(MediaException.Kind.CORRUPT, "AV1: the points of the film grain scaling function do not increase");
            }
        }
    }

    /**
     * Returns a copy of the parameters (not of the grain that was synthesized) with another seed.
     * <p>
     * 매개변수의 복사본(합성한 그레인은 제외)을 다른 시드로 반환한다.
     *
     * @param seed the grain seed / 그레인 시드
     * @return the copy / 복사본
     */
    Av1FilmGrain withSeed(int seed) {
        Av1FilmGrain g = new Av1FilmGrain();
        g.grainSeed = seed;
        g.numYPoints = numYPoints;
        System.arraycopy(pointYValue, 0, g.pointYValue, 0, pointYValue.length);
        System.arraycopy(pointYScaling, 0, g.pointYScaling, 0, pointYScaling.length);
        g.chromaScalingFromLuma = chromaScalingFromLuma;
        g.numCbPoints = numCbPoints;
        System.arraycopy(pointCbValue, 0, g.pointCbValue, 0, pointCbValue.length);
        System.arraycopy(pointCbScaling, 0, g.pointCbScaling, 0, pointCbScaling.length);
        g.numCrPoints = numCrPoints;
        System.arraycopy(pointCrValue, 0, g.pointCrValue, 0, pointCrValue.length);
        System.arraycopy(pointCrScaling, 0, g.pointCrScaling, 0, pointCrScaling.length);
        g.scalingShift = scalingShift;
        g.arCoeffLag = arCoeffLag;
        System.arraycopy(arCoeffsY, 0, g.arCoeffsY, 0, arCoeffsY.length);
        System.arraycopy(arCoeffsCb, 0, g.arCoeffsCb, 0, arCoeffsCb.length);
        System.arraycopy(arCoeffsCr, 0, g.arCoeffsCr, 0, arCoeffsCr.length);
        g.arCoeffShift = arCoeffShift;
        g.grainScaleShift = grainScaleShift;
        g.cbMult = cbMult;
        g.cbLumaMult = cbLumaMult;
        g.cbOffset = cbOffset;
        g.crMult = crMult;
        g.crLumaMult = crLumaMult;
        g.crOffset = crOffset;
        g.overlap = overlap;
        g.clipToRestrictedRange = clipToRestrictedRange;
        return g;
    }

    /**
     * Rounds like the specification: adds half of the divisor and shifts (an arithmetic shift for negative numbers).
     * <p>
     * 규격처럼 반올림한다: 제수의 절반을 더하고 시프트한다(음수는 산술 시프트).
     *
     * @param x the value / 값
     * @param n the shift / 시프트
     * @return the rounded value / 반올림한 값
     */
    private static int round2(int x, int n) {
        return n == 0 ? x : (x + (1 << (n - 1))) >> n;
    }

    /**
     * Returns the clip of a value to a range.
     * <p>
     * 값을 범위로 자른 결과를 반환한다.
     *
     * @param lo the smallest value / 최솟값
     * @param hi the largest value / 최댓값
     * @param x  the value / 값
     * @return the clipped value / 자른 값
     */
    private static int clip3(int lo, int hi, int x) {
        return x < lo ? lo : (x > hi ? hi : x);
    }

    /**
     * The random number process (specification 7.18.3.2): a 16-bit linear feedback shift register with the taps 0, 1, 3 and 12.
     * <p>
     * 난수 과정(규격 7.18.3.2): 탭이 0, 1, 3, 12 인 16비트 선형 피드백 시프트 레지스터.
     *
     * @param bits the number of random bits to return / 돌려줄 난수 비트 수
     * @return the random number / 난수
     */
    private int getRandomNumber(int bits) {
        int r = random;
        int bit = ((r >> 0) ^ (r >> 1) ^ (r >> 3) ^ (r >> 12)) & 1;
        r = (r >> 1) | (bit << 15);
        random = r;
        return (r >> (16 - bits)) & ((1 << bits) - 1);
    }

    /**
     * Adds the film grain to the output picture in place (specification 7.18.3: generate grain, scaling look-up initialization, add noise).
     * <p>
     * 출력 화면에 필름 그레인을 제자리에서 더한다(규격 7.18.3: 그레인 생성, 크기 조절 룩업 초기화, 잡음 더하기).
     *
     * @param pic the finished picture / 완성된 화면
     */
    void apply(Av1Picture pic) {
        int bd = pic.bitDepth;
        int center = 128 << (bd - 8);
        grainMin = -center;
        grainMax = (256 << (bd - 8)) - 1 - center;
        random = grainSeed;
        generateGrain(pic);
        initScaling(pic.numPlanes);
        addNoise(pic);
    }

    /**
     * The generate grain process (specification 7.18.3.3): white noise from the Gaussian sequence, then the auto-regressive filter.
     * <p>
     * 그레인 생성 과정(규격 7.18.3.3): 가우스 수열에서 얻은 백색 잡음, 이어서 자기 회귀 필터.
     *
     * @param pic the picture (for the bit depth and the subsampling) / 화면(비트 깊이와 하위 표본화용)
     */
    private void generateGrain(Av1Picture pic) {
        int bd = pic.bitDepth;
        int shift = 12 - bd + grainScaleShift;
        lumaGrain = new int[73][82];
        // 휘도 백색 잡음: 점이 있을 때만 가우스 수열에서 뽑는다.
        for (int y = 0; y < 73; y++) {
            for (int x = 0; x < 82; x++) {
                int g = numYPoints > 0 ? AvifTables.GAUSSIAN_SEQUENCE[getRandomNumber(11)] : 0;
                lumaGrain[y][x] = round2(g, shift);
            }
        }
        // 휘도 자기 회귀 필터: 이미 만든 위쪽과 왼쪽 이웃의 가중 합을 더한다.
        for (int y = 3; y < 73; y++) {
            for (int x = 3; x < 82 - 3; x++) {
                int s = 0;
                int pos = 0;
                filter:
                // 이웃의 행(위쪽 행부터 현재 행까지)을 차례로 본다.
                for (int dr = -arCoeffLag; dr <= 0; dr++) {
                    // 행 안의 열 이웃을 차례로 본다.
                    for (int dc = -arCoeffLag; dc <= arCoeffLag; dc++) {
                        if (dr == 0 && dc == 0) {
                            break filter;
                        }
                        s += lumaGrain[y + dr][x + dc] * (arCoeffsY[pos] - 128);
                        pos++;
                    }
                }
                lumaGrain[y][x] = clip3(grainMin, grainMax, lumaGrain[y][x] + round2(s, arCoeffShift));
            }
        }
        if (pic.numPlanes == 1) {
            return;
        }
        int subX = pic.subX;
        int subY = pic.subY;
        int chromaW = subX != 0 ? 44 : 82;
        int chromaH = subY != 0 ? 38 : 73;
        cbGrain = new int[chromaH][chromaW];
        crGrain = new int[chromaH][chromaW];
        random = grainSeed ^ 0xb524;
        // Cb 백색 잡음.
        for (int y = 0; y < chromaH; y++) {
            for (int x = 0; x < chromaW; x++) {
                int g = (numCbPoints > 0 || chromaScalingFromLuma) ? AvifTables.GAUSSIAN_SEQUENCE[getRandomNumber(11)] : 0;
                cbGrain[y][x] = round2(g, shift);
            }
        }
        random = grainSeed ^ 0x49d8;
        // Cr 백색 잡음.
        for (int y = 0; y < chromaH; y++) {
            for (int x = 0; x < chromaW; x++) {
                int g = (numCrPoints > 0 || chromaScalingFromLuma) ? AvifTables.GAUSSIAN_SEQUENCE[getRandomNumber(11)] : 0;
                crGrain[y][x] = round2(g, shift);
            }
        }
        // 색차 자기 회귀 필터: 같은 평면의 이웃과, 마지막 계수는 같은 위치의 휘도 그레인(하위 표본화 평균)에 곱한다.
        for (int y = 3; y < chromaH; y++) {
            for (int x = 3; x < chromaW - 3; x++) {
                int s0 = 0;
                int s1 = 0;
                int pos = 0;
                filter:
                // 이웃의 행(위쪽 행부터 현재 행까지)을 차례로 본다.
                for (int dr = -arCoeffLag; dr <= 0; dr++) {
                    // 행 안의 열 이웃을 차례로 본다.
                    for (int dc = -arCoeffLag; dc <= arCoeffLag; dc++) {
                        int c0 = arCoeffsCb[pos] - 128;
                        int c1 = arCoeffsCr[pos] - 128;
                        if (dr == 0 && dc == 0) {
                            if (numYPoints > 0) {
                                int luma = 0;
                                int lumaX = ((x - 3) << subX) + 3;
                                int lumaY = ((y - 3) << subY) + 3;
                                // 하위 표본화 영역의 휘도 그레인을 더해 평균한다.
                                for (int i = 0; i <= subY; i++) {
                                    for (int j = 0; j <= subX; j++) {
                                        luma += lumaGrain[lumaY + i][lumaX + j];
                                    }
                                }
                                luma = round2(luma, subX + subY);
                                s0 += luma * c0;
                                s1 += luma * c1;
                            }
                            break filter;
                        }
                        s0 += cbGrain[y + dr][x + dc] * c0;
                        s1 += crGrain[y + dr][x + dc] * c1;
                        pos++;
                    }
                }
                cbGrain[y][x] = clip3(grainMin, grainMax, cbGrain[y][x] + round2(s0, arCoeffShift));
                crGrain[y][x] = clip3(grainMin, grainMax, crGrain[y][x] + round2(s1, arCoeffShift));
            }
        }
    }

    /**
     * Returns the x coordinate of a point of the scaling function of a plane.
     * <p>
     * 평면의 크기 조절 함수의 점의 x 좌표를 반환한다.
     *
     * @param plane the plane / 평면
     * @param i     the point / 점
     * @return the sample value of the point / 점의 샘플 값
     */
    private int pointX(int plane, int i) {
        return (plane == 0 || chromaScalingFromLuma) ? pointYValue[i] : (plane == 1 ? pointCbValue[i] : pointCrValue[i]);
    }

    /**
     * Returns the y coordinate of a point of the scaling function of a plane.
     * <p>
     * 평면의 크기 조절 함수의 점의 y 좌표를 반환한다.
     *
     * @param plane the plane / 평면
     * @param i     the point / 점
     * @return the scaling value of the point / 점의 크기 값
     */
    private int pointY(int plane, int i) {
        return (plane == 0 || chromaScalingFromLuma) ? pointYScaling[i] : (plane == 1 ? pointCbScaling[i] : pointCrScaling[i]);
    }

    /**
     * The scaling lookup initialization process (specification 7.18.3.4): a piecewise linear interpolation of the points to 256 entries.
     * <p>
     * 크기 조절 룩업 초기화 과정(규격 7.18.3.4): 점을 구간별 선형 보간해 256 항목으로 만든다.
     *
     * @param numPlanes the number of planes / 평면 수
     */
    private void initScaling(int numPlanes) {
        // 평면마다 표를 채운다.
        for (int plane = 0; plane < numPlanes; plane++) {
            int numPoints;
            if (plane == 0 || chromaScalingFromLuma) {
                numPoints = numYPoints;
            } else if (plane == 1) {
                numPoints = numCbPoints;
            } else {
                numPoints = numCrPoints;
            }
            int[] lut = scalingLut[plane];
            if (numPoints == 0) {
                // 점이 없으면 표를 모두 0 으로 채운다.
                for (int x = 0; x < 256; x++) {
                    lut[x] = 0;
                }
                continue;
            }
            // 첫 점 앞은 첫 점의 크기 값으로 채운다.
            for (int x = 0; x < pointX(plane, 0); x++) {
                lut[x] = pointY(plane, 0);
            }
            // 이웃한 두 점 사이는 선형 보간한다.
            for (int i = 0; i < numPoints - 1; i++) {
                int deltaY = pointY(plane, i + 1) - pointY(plane, i);
                int deltaX = pointX(plane, i + 1) - pointX(plane, i);
                int delta = deltaY * ((65536 + (deltaX >> 1)) / deltaX);
                // 두 점 사이의 샘플 값마다 보간한 크기 값을 쓴다.
                for (int x = 0; x < deltaX; x++) {
                    lut[pointX(plane, i) + x] = pointY(plane, i) + ((x * delta + 32768) >> 16);
                }
            }
            // 마지막 점 뒤는 마지막 점의 크기 값으로 채운다.
            for (int x = pointX(plane, numPoints - 1); x < 256; x++) {
                lut[x] = pointY(plane, numPoints - 1);
            }
        }
    }

    /**
     * The scale_lut function of the specification: the scaling of a sample value, interpolated between the entries for high bit depths.
     * <p>
     * 규격의 scale_lut 함수: 샘플 값의 크기 조절. 높은 비트 깊이에서는 항목 사이를 보간한다.
     *
     * @param plane the plane / 평면
     * @param index the sample value / 샘플 값
     * @param bd    the bit depth / 비트 깊이
     * @return the scaling / 크기 조절 값
     */
    private int scaleLut(int plane, int index, int bd) {
        int shift = bd - 8;
        int x = index >> shift;
        int rem = index - (x << shift);
        if (bd == 8 || x == 255) {
            return scalingLut[plane][x];
        }
        int start = scalingLut[plane][x];
        int end = scalingLut[plane][x + 1];
        return start + round2((end - start) * rem, shift);
    }

    /**
     * The add noise synthesis process (specification 7.18.3.5): builds the noise stripes of 32 luma rows from the grain templates with random
     * offsets, blends the overlaps into the noise image row by row, and adds the scaled noise to the samples (chroma first, because it needs
     * the luma without noise).
     * <p>
     * 잡음 더하기 합성 과정(규격 7.18.3.5): 그레인 견본에서 무작위 오프셋으로 휘도 32행의 잡음 띠를 만들고, 겹침을 섞어 행 단위로 잡음 이미지를 얻고,
     * 크기를 조절한 잡음을 샘플에 더한다(색차가 잡음 없는 휘도를 필요로 하므로 색차를 먼저 한다).
     *
     * @param pic the picture / 화면
     */
    private void addNoise(Av1Picture pic) {
        int w = pic.width;
        int h = pic.height;
        int subX = pic.subX;
        int subY = pic.subY;
        int bd = pic.bitDepth;
        int numPlanes = pic.numPlanes;
        int stripes = ((h + 1) / 2 + 15) / 16;
        int stripeW = (((w + 1) / 2 + 15) / 16) * 32 + 34;
        short[][][] noiseStripe = new short[stripes][numPlanes][];
        int lumaNum = 0;
        // 32 행짜리 띠마다 난수 상태를 초기화하고 32 x 32 블록마다 무작위 오프셋으로 그레인 견본을 옮겨 적는다.
        for (int y = 0; y < (h + 1) / 2; y += 16) {
            random = grainSeed;
            random ^= ((lumaNum * 37 + 178) & 255) << 8;
            random ^= (lumaNum * 173 + 105) & 255;
            // 평면마다 띠의 잡음 배열을 만든다.
            for (int plane = 0; plane < numPlanes; plane++) {
                noiseStripe[lumaNum][plane] = new short[34 * stripeW];
            }
            // 블록(휘도 32 열)마다 난수로 오프셋을 정한다.
            for (int x = 0; x < (w + 1) / 2; x += 16) {
                int rand = getRandomNumber(8);
                int offsetX = rand >> 4;
                int offsetY = rand & 15;
                // 평면마다 같은 오프셋으로 견본의 블록을 옮겨 적는다.
                for (int plane = 0; plane < numPlanes; plane++) {
                    int planeSubX = plane > 0 ? subX : 0;
                    int planeSubY = plane > 0 ? subY : 0;
                    int planeOffsetX = planeSubX != 0 ? 6 + offsetX : 9 + offsetX * 2;
                    int planeOffsetY = planeSubY != 0 ? 6 + offsetY : 9 + offsetY * 2;
                    int[][] template = plane == 0 ? lumaGrain : (plane == 1 ? cbGrain : crGrain);
                    short[] stripe = noiseStripe[lumaNum][plane];
                    // 견본에서 34 x 34(색차는 하위 표본화한 크기) 블록을 가져와 이전 블록과 겹치는 열을 섞는다.
                    for (int i = 0; i < (34 >> planeSubY); i++) {
                        for (int j = 0; j < (34 >> planeSubX); j++) {
                            int g = template[planeOffsetY + i][planeOffsetX + j];
                            if (planeSubX == 0) {
                                int col = x * 2 + j;
                                if (j < 2 && overlap && x > 0) {
                                    int old = stripe[i * stripeW + col];
                                    g = j == 0 ? old * 27 + g * 17 : old * 17 + g * 27;
                                    g = clip3(grainMin, grainMax, round2(g, 5));
                                }
                                stripe[i * stripeW + col] = (short) g;
                            } else {
                                int col = x + j;
                                if (j == 0 && overlap && x > 0) {
                                    int old = stripe[i * stripeW + col];
                                    g = old * 23 + g * 22;
                                    g = clip3(grainMin, grainMax, round2(g, 5));
                                }
                                stripe[i * stripeW + col] = (short) g;
                            }
                        }
                    }
                }
            }
            lumaNum++;
        }
        int minValue;
        int maxLuma;
        int maxChroma;
        if (clipToRestrictedRange) {
            minValue = 16 << (bd - 8);
            maxLuma = 235 << (bd - 8);
            maxChroma = pic.matrixCoefficients == 0 ? maxLuma : 240 << (bd - 8);
        } else {
            minValue = 0;
            maxLuma = (256 << (bd - 8)) - 1;
            maxChroma = maxLuma;
        }
        int maxSample = (1 << bd) - 1;
        short[] outY = pic.planes[0];
        int strideY = pic.stride[0];
        // 색차: 각 샘플의 잡음을 휘도 평균과 원래 값에서 정한 색인으로 크기를 조절해 더한다.
        if (numPlanes > 1) {
            int cw = (w + subX) >> subX;
            int ch = (h + subY) >> subY;
            // 색차 행마다 잡음 행을 만들고 샘플에 더한다.
            for (int y = 0; y < ch; y++) {
                int[] rowCb = noiseRow(noiseStripe, 1, y, subY, cw, stripeW);
                int[] rowCr = noiseRow(noiseStripe, 2, y, subY, cw, stripeW);
                // 행 안의 색차 샘플마다 병합 색인을 정해 잡음을 더한다.
                for (int x = 0; x < cw; x++) {
                    int lumaX = x << subX;
                    int lumaY = y << subY;
                    int lumaNextX = Math.min(lumaX + 1, w - 1);
                    int averageLuma;
                    if (subX != 0) {
                        averageLuma = round2(outY[lumaY * strideY + lumaX] + outY[lumaY * strideY + lumaNextX], 1);
                    } else {
                        averageLuma = outY[lumaY * strideY + lumaX];
                    }
                    if (numCbPoints > 0 || chromaScalingFromLuma) {
                        int idx = y * pic.stride[1] + x;
                        int orig = pic.planes[1][idx];
                        int merged;
                        if (chromaScalingFromLuma) {
                            merged = averageLuma;
                        } else {
                            int combined = averageLuma * (cbLumaMult - 128) + orig * (cbMult - 128);
                            merged = clip3(0, maxSample, (combined >> 6) + ((cbOffset - 256) << (bd - 8)));
                        }
                        int noise = round2(scaleLut(1, merged, bd) * rowCb[x], scalingShift);
                        pic.planes[1][idx] = (short) clip3(minValue, maxChroma, orig + noise);
                    }
                    if (numCrPoints > 0 || chromaScalingFromLuma) {
                        int idx = y * pic.stride[2] + x;
                        int orig = pic.planes[2][idx];
                        int merged;
                        if (chromaScalingFromLuma) {
                            merged = averageLuma;
                        } else {
                            int combined = averageLuma * (crLumaMult - 128) + orig * (crMult - 128);
                            merged = clip3(0, maxSample, (combined >> 6) + ((crOffset - 256) << (bd - 8)));
                        }
                        int noise = round2(scaleLut(2, merged, bd) * rowCr[x], scalingShift);
                        pic.planes[2][idx] = (short) clip3(minValue, maxChroma, orig + noise);
                    }
                }
            }
        }
        // 휘도: 점이 있을 때만 잡음을 더한다.
        if (numYPoints > 0) {
            for (int y = 0; y < h; y++) {
                int[] row = noiseRow(noiseStripe, 0, y, 0, w, stripeW);
                // 행 안의 휘도 샘플마다 잡음을 더한다.
                for (int x = 0; x < w; x++) {
                    int orig = outY[y * strideY + x];
                    int noise = round2(scaleLut(0, orig, bd) * row[x], scalingShift);
                    outY[y * strideY + x] = (short) clip3(minValue, maxLuma, orig + noise);
                }
            }
        }
    }

    /**
     * Builds one row of the noise image of a plane from the noise stripes: the row of the stripe that holds it, blended with the rows of the
     * stripe above for the first two rows (one row with a subsampled height) of a stripe when the overlap is on.
     * <p>
     * 잡음 띠에서 평면의 잡음 이미지 한 행을 만든다: 그 행이 속한 띠의 행이며, 겹침이 켜져 있으면 띠의 첫 두 행(하위 표본화한 높이는 한 행)을 위쪽 띠의 행과
     * 섞는다.
     *
     * @param noiseStripe the noise stripes / 잡음 띠
     * @param plane       the plane / 평면
     * @param y           the row in the plane / 평면 안의 행
     * @param planeSubY   the vertical subsampling of the plane / 평면의 세로 하위 표본화
     * @param width       the width of the plane / 평면의 너비
     * @param stripeW     the width of a stripe / 띠의 너비
     * @return the noise values of the row / 행의 잡음 값
     */
    private int[] noiseRow(short[][][] noiseStripe, int plane, int y, int planeSubY, int width, int stripeW) {
        int lumaNum = y >> (5 - planeSubY);
        int i = y - (lumaNum << (5 - planeSubY));
        int[] row = new int[width];
        short[] cur = noiseStripe[lumaNum][plane];
        // 행의 값마다 필요하면 위쪽 띠의 값과 섞는다.
        for (int x = 0; x < width; x++) {
            int g = cur[i * stripeW + x];
            if (planeSubY == 0) {
                if (i < 2 && lumaNum > 0 && overlap) {
                    int old = noiseStripe[lumaNum - 1][plane][(i + 32) * stripeW + x];
                    g = i == 0 ? old * 27 + g * 17 : old * 17 + g * 27;
                    g = clip3(grainMin, grainMax, round2(g, 5));
                }
            } else if (i < 1 && lumaNum > 0 && overlap) {
                int old = noiseStripe[lumaNum - 1][plane][(i + 16) * stripeW + x];
                g = old * 23 + g * 22;
                g = clip3(grainMin, grainMax, round2(g, 5));
            }
            row[x] = g;
        }
        return row;
    }
}
