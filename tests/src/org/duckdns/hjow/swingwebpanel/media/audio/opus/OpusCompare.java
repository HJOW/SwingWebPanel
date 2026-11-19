package org.duckdns.hjow.swingwebpanel.media.audio.opus;

/**
 * Test-only port of the quality metric of <code>opus_compare</code>, the program that RFC 6716 section 6.1 and appendix A.4 use to
 * decide whether a decoder passes a test vector: it compares the band energies of a reference and a test signal (48 kHz, stereo
 * 16-bit) on a perceptual scale and reports a quality in percent, where a negative value means that the test vector fails. The
 * original is part of the reference decoder source of RFC 6716 (BSD license, Xiph.Org Foundation and contributors; see
 * docs/progress/media-opus.md).
 * <p>
 * <code>opus_compare</code> 품질 지표의 시험 전용 이식. RFC 6716 6.1 과 부록 A.4 는 이 프로그램으로 디코더가 시험 벡터를 통과하는지 판정한다:
 * 기준 신호와 시험 신호(48 kHz, 스테레오 16비트)의 대역 에너지를 지각 척도로 비교해 백분율 품질을 알려 주며 음수이면 시험 벡터에 실패한 것이다.
 * 원본은 RFC 6716 참조 디코더 소스의 일부이다(BSD 라이선스, Xiph.Org Foundation 등. docs/progress/media-opus.md 참조).
 */
final class OpusCompare {

    /**
     * Number of bands of the comparison.
     * <p>
     * 비교하는 대역 수.
     */
    private static final int NBANDS = 21;

    /**
     * Number of frequency bins of the comparison.
     * <p>
     * 비교하는 주파수 빈 수.
     */
    private static final int NFREQS = 240;

    /**
     * Band edges in bins.
     * <p>
     * 빈 단위 대역 경계.
     */
    private static final int[] BANDS = { 0, 2, 4, 6, 8, 10, 12, 14, 16, 20, 24, 28, 32, 40, 48, 56, 68, 80, 96, 120, 156, 200 };

    /**
     * Analysis window size in samples.
     * <p>
     * 분석 창 크기(샘플).
     */
    private static final int TEST_WIN_SIZE = 480;

    /**
     * Analysis window step in samples.
     * <p>
     * 분석 창 이동 간격(샘플).
     */
    private static final int TEST_WIN_STEP = 120;

    /**
     * Single precision pi as used by the original.
     * <p>
     * 원본이 쓰는 단정밀도 원주율.
     */
    private static final float OPUS_PI = 3.14159265F;

    /**
     * Not instantiable.
     * <p>
     * 인스턴스를 만들 수 없다.
     */
    private OpusCompare() {
    }

    /**
     * Computes the band energies and the power spectra of a stereo signal.
     * <p>
     * 스테레오 신호의 대역 에너지와 파워 스펙트럼을 계산한다.
     *
     * @param out     receives the band energies (frames x bands x 2), or null / 대역 에너지(프레임 x 대역 x 2)를 받는 배열. 없으면 null
     * @param ps      receives the power spectra (frames x 240 x 2) / 파워 스펙트럼(프레임 x 240 x 2)을 받는 배열
     * @param bands   the band edges / 대역 경계
     * @param nbands  number of bands / 대역 수
     * @param in      interleaved samples / 인터리브된 샘플
     * @param nframes number of frames / 프레임 수
     */
    private static void bandEnergy(float[] out, float[] ps, int[] bands, int nbands, float[] in, int nframes) {
        final int nch = 2;
        final int windowSz = TEST_WIN_SIZE;
        final int psSz = windowSz / 2;
        float[] window = new float[windowSz];
        float[] c = new float[windowSz];
        float[] s = new float[windowSz];
        float[] x = new float[nch * windowSz];
        // 창 함수와 코사인/사인 표를 계산한다.
        for (int xj = 0; xj < windowSz; xj++) {
            window[xj] = 0.5F - 0.5F * (float) Math.cos((2 * OPUS_PI / (windowSz - 1)) * xj);
            c[xj] = (float) Math.cos((2 * OPUS_PI / windowSz) * xj);
            s[xj] = (float) Math.sin((2 * OPUS_PI / windowSz) * xj);
        }
        // 프레임마다 창을 씌우고 대역별로 DFT 에너지를 구한다.
        for (int xi = 0; xi < nframes; xi++) {
            for (int ci = 0; ci < nch; ci++) {
                // xk 를 0 부터 windowSz 까지 늘려 가며 항목마다 처리한다.
                for (int xk = 0; xk < windowSz; xk++) {
                    x[ci * windowSz + xk] = window[xk] * in[(xi * TEST_WIN_STEP + xk) * nch + ci];
                }
            }
            int xj = 0;
            // bi 를 0 부터 nbands 까지 늘려 가며 항목마다 처리한다.
            for (int bi = 0; bi < nbands; bi++) {
                float[] p = new float[2];
                // 항목마다 반복해서 처리한다.
                for (; xj < bands[bi + 1]; xj++) {
                    // ci 를 0 부터 nch 까지 늘려 가며 항목마다 처리한다.
                    for (int ci = 0; ci < nch; ci++) {
                        int ti = 0;
                        float re = 0;
                        float im = 0;
                        // xk 를 0 부터 windowSz 까지 늘려 가며 항목마다 처리한다.
                        for (int xk = 0; xk < windowSz; xk++) {
                            re += c[ti] * x[ci * windowSz + xk];
                            im -= s[ti] * x[ci * windowSz + xk];
                            ti += xj;
                            if (ti >= windowSz) {
                                ti -= windowSz;
                            }
                        }
                        ps[(xi * psSz + xj) * nch + ci] = re * re + im * im + 100000;
                        p[ci] += ps[(xi * psSz + xj) * nch + ci];
                    }
                }
                if (out != null) {
                    out[(xi * nbands + bi) * nch] = p[0] / (bands[bi + 1] - bands[bi]);
                    out[(xi * nbands + bi) * nch + 1] = p[1] / (bands[bi + 1] - bands[bi]);
                }
            }
        }
    }

    /**
     * Compares two stereo 48 kHz signals with the RFC 6716 quality metric.
     * <p>
     * 스테레오 48 kHz 신호 두 개를 RFC 6716 품질 지표로 비교한다.
     *
     * @param ref  the reference samples, interleaved / 기준 샘플(인터리브)
     * @param test the samples under test, interleaved, as many as the reference / 시험 샘플(인터리브). 기준과 같은 개수
     * @return the quality in percent; a negative value means that the test vector fails / 백분율 품질. 음수이면 시험 벡터 실패
     */
    static double quality(short[] ref, short[] test) {
        final int nch = 2;
        if (ref.length != test.length) {
            throw new IllegalArgumentException("sample counts differ");
        }
        int xlength = ref.length / nch;
        if (xlength < TEST_WIN_SIZE) {
            throw new IllegalArgumentException("insufficient sample data");
        }
        float[] x = new float[ref.length];
        float[] y = new float[test.length];
        // 정수 샘플을 float 로 옮긴다.
        for (int i = 0; i < ref.length; i++) {
            x[i] = ref[i];
            y[i] = test[i];
        }
        int nframes = (xlength - TEST_WIN_SIZE + TEST_WIN_STEP) / TEST_WIN_STEP;
        float[] xb = new float[nframes * NBANDS * nch];
        float[] bigX = new float[nframes * NFREQS * nch];
        float[] bigY = new float[nframes * NFREQS * nch];
        bandEnergy(xb, bigX, BANDS, NBANDS, x, nframes);
        bandEnergy(null, bigY, BANDS, NBANDS, y, nframes);
        // 기준 신호의 대역 에너지를 인접 대역과 이전 프레임으로 퍼뜨려 마스킹 문턱을 만든다.
        for (int xi = 0; xi < nframes; xi++) {
            for (int bi = 1; bi < NBANDS; bi++) {
                // ci 를 0 부터 nch 까지 늘려 가며 항목마다 처리한다.
                for (int ci = 0; ci < nch; ci++) {
                    xb[(xi * NBANDS + bi) * nch + ci] += 0.1F * xb[(xi * NBANDS + bi - 1) * nch + ci];
                }
            }
            // 항목마다 반복해서 처리한다.
            for (int bi = NBANDS - 1; bi-- > 0;) {
                // ci 를 0 부터 nch 까지 늘려 가며 항목마다 처리한다.
                for (int ci = 0; ci < nch; ci++) {
                    xb[(xi * NBANDS + bi) * nch + ci] += 0.03F * xb[(xi * NBANDS + bi + 1) * nch + ci];
                }
            }
            if (xi > 0) {
                // bi 를 0 부터 NBANDS 까지 늘려 가며 항목마다 처리한다.
                for (int bi = 0; bi < NBANDS; bi++) {
                    // ci 를 0 부터 nch 까지 늘려 가며 항목마다 처리한다.
                    for (int ci = 0; ci < nch; ci++) {
                        xb[(xi * NBANDS + bi) * nch + ci] += 0.5F * xb[((xi - 1) * NBANDS + bi) * nch + ci];
                    }
                }
            }
            // bi 를 0 부터 NBANDS 까지 늘려 가며 항목마다 처리한다.
            for (int bi = 0; bi < NBANDS; bi++) {
                float l = xb[(xi * NBANDS + bi) * nch];
                float r = xb[(xi * NBANDS + bi) * nch + 1];
                xb[(xi * NBANDS + bi) * nch] += 0.01F * r;
                xb[(xi * NBANDS + bi) * nch + 1] += 0.01F * l;
            }
            // bi 를 0 부터 NBANDS 까지 늘려 가며 항목마다 처리한다.
            for (int bi = 0; bi < NBANDS; bi++) {
                // xj 를 BANDS[bi] 부터 BANDS[bi + 1] 까지 늘려 가며 항목마다 처리한다.
                for (int xj = BANDS[bi]; xj < BANDS[bi + 1]; xj++) {
                    // ci 를 0 부터 nch 까지 늘려 가며 항목마다 처리한다.
                    for (int ci = 0; ci < nch; ci++) {
                        bigX[(xi * NFREQS + xj) * nch + ci] += 0.1F * xb[(xi * NBANDS + bi) * nch + ci];
                        bigY[(xi * NFREQS + xj) * nch + ci] += 0.1F * xb[(xi * NBANDS + bi) * nch + ci];
                    }
                }
            }
        }
        // 주파수 빈마다 프레임 방향으로 인접한 값을 누적한다.
        for (int bi = 0; bi < NBANDS; bi++) {
            for (int xj = BANDS[bi]; xj < BANDS[bi + 1]; xj++) {
                // ci 를 0 부터 nch 까지 늘려 가며 항목마다 처리한다.
                for (int ci = 0; ci < nch; ci++) {
                    float xtmp = bigX[xj * nch + ci];
                    float ytmp = bigY[xj * nch + ci];
                    // xi 를 1 부터 nframes 까지 늘려 가며 항목마다 처리한다.
                    for (int xi = 1; xi < nframes; xi++) {
                        float xtmp2 = bigX[(xi * NFREQS + xj) * nch + ci];
                        float ytmp2 = bigY[(xi * NFREQS + xj) * nch + ci];
                        bigX[(xi * NFREQS + xj) * nch + ci] += xtmp;
                        bigY[(xi * NFREQS + xj) * nch + ci] += ytmp;
                        xtmp = xtmp2;
                        ytmp = ytmp2;
                    }
                }
            }
        }
        int maxCompare = BANDS[NBANDS];
        double err = 0;
        // 프레임마다 두 스펙트럼의 비율로 오차를 구해 합산한다.
        for (int xi = 0; xi < nframes; xi++) {
            double ef = 0;
            // bi 를 0 부터 NBANDS 까지 늘려 가며 항목마다 처리한다.
            for (int bi = 0; bi < NBANDS; bi++) {
                double eb = 0;
                // xj 를 BANDS[bi] 부터 BANDS[bi + 1] && xj < maxCompare 까지 늘려 가며 항목마다 처리한다.
                for (int xj = BANDS[bi]; xj < BANDS[bi + 1] && xj < maxCompare; xj++) {
                    // ci 를 0 부터 nch 까지 늘려 가며 항목마다 처리한다.
                    for (int ci = 0; ci < nch; ci++) {
                        float re = bigY[(xi * NFREQS + xj) * nch + ci] / bigX[(xi * NFREQS + xj) * nch + ci];
                        float im = re - (float) Math.log(re) - 1;
                        if (xj >= 79 && xj <= 81) {
                            im *= 0.1F;
                        }
                        if (xj == 80) {
                            im *= 0.1F;
                        }
                        eb += im;
                    }
                }
                eb /= (BANDS[bi + 1] - BANDS[bi]) * nch;
                ef += eb * eb;
            }
            ef /= NBANDS;
            ef *= ef;
            err += ef * ef;
        }
        err = Math.pow(err / nframes, 1.0 / 16);
        return 100 * (1 - 0.5 * Math.log(1 + err) / Math.log(1.13));
    }
}
