package org.duckdns.hjow.swingwebpanel.js.host.canvas;

/**
 * The result of <code>measureText()</code>: a <code>TextMetrics</code> value. All lengths are in CSS pixels. The horizontal values are measured
 * from the alignment point of the text (the <code>x</code> that would be given to <code>fillText</code>) and the vertical values from the
 * current text baseline, positive upwards for the ascents and downwards for the descents.
 * <p>
 * <code>measureText()</code> 의 결과인 <code>TextMetrics</code> 값. 모든 길이는 CSS 픽셀이다. 가로 값은 텍스트의 정렬 기준점(<code>fillText</code> 에
 * 주는 <code>x</code>)에서, 세로 값은 현재 텍스트 기준선에서 재며 ascent 는 위쪽, descent 는 아래쪽이 양수이다.
 */
public final class TextMetricsValue {

    /**
     * The advance width of the text.
     * <p>
     * 텍스트의 진행 너비.
     */
    private final double width;

    /**
     * The distance from the alignment point to the left of the ink box, positive to the left.
     * <p>
     * 정렬 기준점에서 글자 잉크 상자의 왼쪽까지의 거리. 왼쪽이 양수.
     */
    private final double actualBoundingBoxLeft;

    /**
     * The distance from the alignment point to the right of the ink box.
     * <p>
     * 정렬 기준점에서 글자 잉크 상자의 오른쪽까지의 거리.
     */
    private final double actualBoundingBoxRight;

    /**
     * The distance from the baseline to the top of the font's bounding box.
     * <p>
     * 기준선에서 글꼴 경계 상자 위쪽까지의 거리.
     */
    private final double fontBoundingBoxAscent;

    /**
     * The distance from the baseline to the bottom of the font's bounding box.
     * <p>
     * 기준선에서 글꼴 경계 상자 아래쪽까지의 거리.
     */
    private final double fontBoundingBoxDescent;

    /**
     * The distance from the baseline to the top of the ink box.
     * <p>
     * 기준선에서 글자 잉크 상자 위쪽까지의 거리.
     */
    private final double actualBoundingBoxAscent;

    /**
     * The distance from the baseline to the bottom of the ink box.
     * <p>
     * 기준선에서 글자 잉크 상자 아래쪽까지의 거리.
     */
    private final double actualBoundingBoxDescent;

    /**
     * The ascent of the em square.
     * <p>
     * em 사각형의 ascent.
     */
    private final double emHeightAscent;

    /**
     * The descent of the em square.
     * <p>
     * em 사각형의 descent.
     */
    private final double emHeightDescent;

    /**
     * The offset of the hanging baseline from the current baseline.
     * <p>
     * 현재 기준선에서 hanging 기준선까지의 오프셋.
     */
    private final double hangingBaseline;

    /**
     * The offset of the alphabetic baseline from the current baseline.
     * <p>
     * 현재 기준선에서 alphabetic 기준선까지의 오프셋.
     */
    private final double alphabeticBaseline;

    /**
     * The offset of the ideographic baseline from the current baseline.
     * <p>
     * 현재 기준선에서 ideographic 기준선까지의 오프셋.
     */
    private final double ideographicBaseline;

    /**
     * Creates the value.
     * <p>
     * 값을 만든다.
     *
     * @param v the twelve numbers in the order of the attributes of <code>TextMetrics</code> / <code>TextMetrics</code> 속성 순서의 열두 숫자
     */
    TextMetricsValue(double[] v) {
        width = v[0];
        actualBoundingBoxLeft = v[1];
        actualBoundingBoxRight = v[2];
        fontBoundingBoxAscent = v[3];
        fontBoundingBoxDescent = v[4];
        actualBoundingBoxAscent = v[5];
        actualBoundingBoxDescent = v[6];
        emHeightAscent = v[7];
        emHeightDescent = v[8];
        hangingBaseline = v[9];
        alphabeticBaseline = v[10];
        ideographicBaseline = v[11];
    }

    /**
     * Returns the advance width.
     * <p>
     * 진행 너비를 반환한다.
     *
     * @return the width / 너비
     */
    public double getWidth() {
        return width;
    }

    /**
     * Returns the left of the ink box.
     * <p>
     * 잉크 상자의 왼쪽을 반환한다.
     *
     * @return the distance / 거리
     */
    public double getActualBoundingBoxLeft() {
        return actualBoundingBoxLeft;
    }

    /**
     * Returns the right of the ink box.
     * <p>
     * 잉크 상자의 오른쪽을 반환한다.
     *
     * @return the distance / 거리
     */
    public double getActualBoundingBoxRight() {
        return actualBoundingBoxRight;
    }

    /**
     * Returns the ascent of the font box.
     * <p>
     * 글꼴 상자의 ascent 를 반환한다.
     *
     * @return the distance / 거리
     */
    public double getFontBoundingBoxAscent() {
        return fontBoundingBoxAscent;
    }

    /**
     * Returns the descent of the font box.
     * <p>
     * 글꼴 상자의 descent 를 반환한다.
     *
     * @return the distance / 거리
     */
    public double getFontBoundingBoxDescent() {
        return fontBoundingBoxDescent;
    }

    /**
     * Returns the ascent of the ink box.
     * <p>
     * 잉크 상자의 ascent 를 반환한다.
     *
     * @return the distance / 거리
     */
    public double getActualBoundingBoxAscent() {
        return actualBoundingBoxAscent;
    }

    /**
     * Returns the descent of the ink box.
     * <p>
     * 잉크 상자의 descent 를 반환한다.
     *
     * @return the distance / 거리
     */
    public double getActualBoundingBoxDescent() {
        return actualBoundingBoxDescent;
    }

    /**
     * Returns the ascent of the em square.
     * <p>
     * em 사각형의 ascent 를 반환한다.
     *
     * @return the distance / 거리
     */
    public double getEmHeightAscent() {
        return emHeightAscent;
    }

    /**
     * Returns the descent of the em square.
     * <p>
     * em 사각형의 descent 를 반환한다.
     *
     * @return the distance / 거리
     */
    public double getEmHeightDescent() {
        return emHeightDescent;
    }

    /**
     * Returns the offset of the hanging baseline.
     * <p>
     * hanging 기준선의 오프셋을 반환한다.
     *
     * @return the offset / 오프셋
     */
    public double getHangingBaseline() {
        return hangingBaseline;
    }

    /**
     * Returns the offset of the alphabetic baseline.
     * <p>
     * alphabetic 기준선의 오프셋을 반환한다.
     *
     * @return the offset / 오프셋
     */
    public double getAlphabeticBaseline() {
        return alphabeticBaseline;
    }

    /**
     * Returns the offset of the ideographic baseline.
     * <p>
     * ideographic 기준선의 오프셋을 반환한다.
     *
     * @return the offset / 오프셋
     */
    public double getIdeographicBaseline() {
        return ideographicBaseline;
    }
}
