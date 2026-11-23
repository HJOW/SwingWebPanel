package org.duckdns.hjow.swingwebpanel.media.image.color;

/**
 * A transfer function: it turns an encoded sample value in 0 to 1 into linear light. The functions that colour profiles and the coding-independent
 * code points of ITU-T H.273 name are here: a pure power curve, the parametric curves of ICC.1:2010 (the five function types of the
 * <code>para</code> tag, Table 68), the sampled curve of an ICC <code>curv</code> tag, the sRGB curve (IEC 61966-2-1), SMPTE 240M, and the high
 * dynamic range curves PQ (SMPTE ST 2084) and HLG (ITU-R BT.2100). The Rec. 709 / SMPTE 170M / Rec. 2020 code points use the sRGB curve, as
 * Chromium does (<code>ui/gfx/color_space.cc</code>, <code>GetTransferFunction</code>: "use the same transfer function as sRGB, which will more
 * closely match other media players"). The PQ and HLG functions return linear light relative to the HDR reference white of 203 cd/m2
 * (ITU-R BT.2408), so that a value of 1 is the white of a standard dynamic range display.
 * <p>
 * 전달 함수: 0~1 의 부호화된 샘플 값을 선형 광으로 바꾼다. 색 프로파일과 ITU-T H.273 의 코딩 독립 코드 값이 이름 붙이는 함수가 여기 있다: 순수
 * 거듭제곱 곡선, ICC.1:2010 의 매개변수 곡선(<code>para</code> 태그의 다섯 함수 형식, 표 68), ICC <code>curv</code> 태그의 표본 곡선, sRGB 곡선
 * (IEC 61966-2-1), SMPTE 240M, 고동적 범위 곡선 PQ(SMPTE ST 2084)와 HLG(ITU-R BT.2100). Rec. 709 / SMPTE 170M / Rec. 2020 코드 값은 Chromium 처럼
 * sRGB 곡선을 쓴다(<code>ui/gfx/color_space.cc</code> 의 <code>GetTransferFunction</code>: "sRGB 와 같은 전달 함수를 써서 다른 미디어 재생기와 더 가깝게
 * 맞춘다"). PQ 와 HLG 함수는 HDR 기준 백색 203 cd/m2(ITU-R BT.2408)에 대한 선형 광을 반환하므로 1 이 표준 동적 범위 화면의 흰색이다.
 */
abstract class Transfer {

    /**
     * The HDR reference white luminance in cd/m2 (ITU-R BT.2408 Table 2): the luminance of a diffuse white in an HDR picture, which a standard
     * dynamic range picture shows as 100 percent white.
     * <p>
     * HDR 기준 백색 휘도(cd/m2, ITU-R BT.2408 표 2): HDR 화면에서 확산 흰색의 휘도이며 표준 동적 범위 화면은 이것을 100 % 흰색으로 보여 준다.
     */
    static final double HDR_REFERENCE_WHITE = 203.0;

    /**
     * Creates a transfer function (subclasses only).
     * <p>
     * 전달 함수를 만든다(하위 클래스 전용).
     */
    Transfer() {
    }

    /**
     * Turns an encoded value into linear light.
     * <p>
     * 부호화된 값을 선형 광으로 바꾼다.
     *
     * @param v the encoded value in 0 to 1 / 0~1 의 부호화된 값
     * @return linear light (1 is the reference white; values above 1 are possible for HDR curves) / 선형 광(1 이 기준 백색이며 HDR 곡선은 1 을 넘을 수 있다)
     */
    abstract double toLinear(double v);

    /**
     * Tells whether the function is the sRGB curve (used to recognise sRGB sources, which need no conversion).
     * <p>
     * 함수가 sRGB 곡선인지 알려 준다(변환이 필요 없는 sRGB 소스를 알아보는 데 쓴다).
     *
     * @return true for the sRGB curve / sRGB 곡선이면 true
     */
    boolean isSrgb() {
        return false;
    }

    /**
     * The sRGB curve of IEC 61966-2-1: a linear part below 0.04045 and a power of 2.4 above.
     * <p>
     * IEC 61966-2-1 의 sRGB 곡선: 0.04045 아래는 선형, 위는 2.4 거듭제곱.
     */
    static final Transfer SRGB = new Transfer() {
        /**
         * Evaluates the sRGB curve.
         * <p>
         * sRGB 곡선을 계산한다.
         *
         * @param v encoded value / 부호화된 값
         * @return linear light / 선형 광
         */
        double toLinear(double v) {
            return v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
        }

        /**
         * This is the sRGB curve.
         * <p>
         * 이것이 sRGB 곡선이다.
         *
         * @return true / true
         */
        boolean isSrgb() {
            return true;
        }
    };

    /**
     * The inverse of the SMPTE 240M transfer function (a linear part below 0.0913 and a power of 1/0.45 above).
     * <p>
     * SMPTE 240M 전달 함수의 역함수(0.0913 아래는 선형, 위는 1/0.45 거듭제곱).
     */
    static final Transfer SMPTE240M = new Transfer() {
        /**
         * Evaluates the inverse of the SMPTE 240M function.
         * <p>
         * SMPTE 240M 함수의 역함수를 계산한다.
         *
         * @param v encoded value / 부호화된 값
         * @return linear light / 선형 광
         */
        double toLinear(double v) {
            return v < 0.0913 ? v / 4.0 : Math.pow((v + 0.1115) / 1.1115, 1.0 / 0.45);
        }
    };

    /**
     * The identity: the samples are linear light.
     * <p>
     * 항등: 샘플이 선형 광이다.
     */
    static final Transfer LINEAR = new Transfer() {
        /**
         * Returns the value itself.
         * <p>
         * 값 자체를 반환한다.
         *
         * @param v encoded value / 부호화된 값
         * @return the same value / 같은 값
         */
        double toLinear(double v) {
            return v;
        }
    };

    /**
     * The inverse of the PQ (perceptual quantizer) curve of SMPTE ST 2084: the encoded value gives an absolute luminance of up to
     * 10000 cd/m2, which is divided by the HDR reference white so that 1 is the reference white.
     * <p>
     * SMPTE ST 2084 PQ(지각 양자화) 곡선의 역함수: 부호화된 값이 최대 10000 cd/m2 의 절대 휘도를 주며, 이것을 HDR 기준 백색으로 나누어 1 이 기준 백색이
     * 되게 한다.
     */
    static final Transfer PQ = new Transfer() {
        /**
         * Evaluates the PQ electro-optical transfer function.
         * <p>
         * PQ 전기광학 전달 함수를 계산한다.
         *
         * @param v encoded value / 부호화된 값
         * @return linear light relative to 203 cd/m2 / 203 cd/m2 에 대한 선형 광
         */
        double toLinear(double v) {
            double m1 = 2610.0 / 16384.0;
            double m2 = 2523.0 / 4096.0 * 128.0;
            double c1 = 3424.0 / 4096.0;
            double c2 = 2413.0 / 4096.0 * 32.0;
            double c3 = 2392.0 / 4096.0 * 32.0;
            double p = Math.pow(v, 1.0 / m2);
            double num = Math.max(p - c1, 0.0);
            double den = c2 - c3 * p;
            return Math.pow(num / den, 1.0 / m1) * 10000.0 / HDR_REFERENCE_WHITE;
        }
    };

    /**
     * The inverse of the HLG (hybrid log-gamma) opto-electronic transfer function of ITU-R BT.2100: it gives scene light between 0 and 1. The
     * display light needs the system gamma (see {@link ColorProfile}), which depends on all three components.
     * <p>
     * ITU-R BT.2100 HLG(하이브리드 로그 감마) 광전 전달 함수의 역함수: 0~1 의 장면 광을 준다. 화면 광에는 세 성분에 모두 의존하는 시스템 감마가
     * 필요하다({@link ColorProfile} 참조).
     */
    static final Transfer HLG_SCENE = new Transfer() {
        /**
         * Evaluates the inverse HLG OETF.
         * <p>
         * HLG OETF 의 역함수를 계산한다.
         *
         * @param v encoded value / 부호화된 값
         * @return scene light / 장면 광
         */
        double toLinear(double v) {
            double a = 0.17883277;
            double b = 0.28466892;
            double c = 0.55991073;
            return v <= 0.5 ? v * v / 3.0 : (Math.exp((v - c) / a) + b) / 12.0;
        }
    };

    /**
     * Creates a pure power curve: linear = value to the power of the exponent.
     * <p>
     * 순수 거듭제곱 곡선을 만든다: 선형 = 값의 지수 거듭제곱.
     *
     * @param exponent the exponent (for example 2.2) / 지수(예: 2.2)
     * @return the function / 함수
     */
    static Transfer gamma(final double exponent) {
        return new Transfer() {
            /**
             * Evaluates the power curve.
             * <p>
             * 거듭제곱 곡선을 계산한다.
             *
             * @param v encoded value / 부호화된 값
             * @return linear light / 선형 광
             */
            double toLinear(double v) {
                return v <= 0 ? 0.0 : Math.pow(v, exponent);
            }
        };
    }

    /**
     * Creates a parametric curve of ICC.1:2010 (the <code>para</code> tag, Table 68). The parameters are g, a, b, c, d, e and f in this order;
     * the types use the first 1, 3, 4, 5 and 7 of them:
     * type 0 Y = X^g; type 1 Y = (aX + b)^g for X &gt;= -b/a else 0; type 2 Y = (aX + b)^g + c for X &gt;= -b/a else c;
     * type 3 Y = (aX + b)^g for X &gt;= d else cX; type 4 Y = (aX + b)^g + e for X &gt;= d else cX + f.
     * <p>
     * ICC.1:2010 의 매개변수 곡선(<code>para</code> 태그, 표 68)을 만든다. 매개변수는 g, a, b, c, d, e, f 순서이고 형식마다 앞의 1, 3, 4, 5, 7 개를 쓴다:
     * 형식 0 Y = X^g, 형식 1 X &gt;= -b/a 에서 Y = (aX + b)^g 아니면 0, 형식 2 X &gt;= -b/a 에서 Y = (aX + b)^g + c 아니면 c,
     * 형식 3 X &gt;= d 에서 Y = (aX + b)^g 아니면 cX, 형식 4 X &gt;= d 에서 Y = (aX + b)^g + e 아니면 cX + f.
     *
     * @param type   the function type 0 to 4 / 함수 형식 0~4
     * @param params the parameters g, a, b, c, d, e, f (as many as the type needs) / 매개변수 g, a, b, c, d, e, f(형식에 필요한 만큼)
     * @return the function, or null for an unknown type or too few parameters / 함수. 모르는 형식이거나 매개변수가 모자라면 null
     */
    static Transfer parametric(final int type, final double[] params) {
        int need = type == 0 ? 1 : type == 1 ? 3 : type == 2 ? 4 : type == 3 ? 5 : type == 4 ? 7 : -1;
        if (need < 0 || params.length < need) {
            return null;
        }
        final double g = params[0];
        final double a = need > 1 ? params[1] : 0;
        final double b = need > 2 ? params[2] : 0;
        final double c = need > 3 ? params[3] : 0;
        final double d = need > 4 ? params[4] : 0;
        final double e = need > 6 ? params[5] : 0;
        final double f = need > 6 ? params[6] : 0;
        return new Transfer() {
            /**
             * Evaluates the parametric curve.
             * <p>
             * 매개변수 곡선을 계산한다.
             *
             * @param v encoded value / 부호화된 값
             * @return linear light / 선형 광
             */
            double toLinear(double v) {
                double r;
                switch (type) {
                case 0:
                    r = v <= 0 ? 0.0 : Math.pow(v, g);
                    break;
                case 1:
                    r = a != 0 && v >= -b / a ? pow(a * v + b, g) : 0.0;
                    break;
                case 2:
                    r = a != 0 && v >= -b / a ? pow(a * v + b, g) + c : c;
                    break;
                case 3:
                    r = v >= d ? pow(a * v + b, g) : c * v;
                    break;
                default:
                    r = v >= d ? pow(a * v + b, g) + e : c * v + f;
                    break;
                }
                return r;
            }

            /**
             * A power that is 0 for a base that is not positive (instead of NaN).
             * <p>
             * 밑이 양수가 아니면 NaN 대신 0 인 거듭제곱.
             *
             * @param base     the base / 밑
             * @param exponent the exponent / 지수
             * @return the power / 거듭제곱
             */
            private double pow(double base, double exponent) {
                return base <= 0 ? 0.0 : Math.pow(base, exponent);
            }
        };
    }

    /**
     * Creates the curve of a table: the entries are the output values for evenly spaced inputs from 0 to 1, with linear interpolation between
     * them (the <code>curv</code> tag with more than one entry, ICC.1:2010 section 10.6). One entry is not a table, and no entry is the identity.
     * <p>
     * 표의 곡선을 만든다. 항목은 0~1 을 고르게 나눈 입력에 대한 출력 값이며 그 사이는 선형 보간한다(항목이 둘 이상인 <code>curv</code> 태그,
     * ICC.1:2010 10.6절). 항목 하나는 표가 아니고, 항목이 없으면 항등이다.
     *
     * @param samples output values in 0 to 1 / 0~1 의 출력 값
     * @return the function / 함수
     */
    static Transfer table(final double[] samples) {
        return new Transfer() {
            /**
             * Interpolates the table.
             * <p>
             * 표를 보간한다.
             *
             * @param v encoded value / 부호화된 값
             * @return linear light / 선형 광
             */
            double toLinear(double v) {
                if (samples.length == 0) {
                    return v;
                }
                double pos = Math.max(0.0, Math.min(1.0, v)) * (samples.length - 1);
                int i = (int) Math.floor(pos);
                if (i >= samples.length - 1) {
                    return samples[samples.length - 1];
                }
                double t = pos - i;
                return samples[i] * (1.0 - t) + samples[i + 1] * t;
            }
        };
    }
}
