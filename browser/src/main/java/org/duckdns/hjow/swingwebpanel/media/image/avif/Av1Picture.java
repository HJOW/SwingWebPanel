package org.duckdns.hjow.swingwebpanel.media.image.avif;

/**
 * A decoded AV1 picture in its coded colour space (plan item P18-03): the sample planes of the luma and the two chroma planes (or only the
 * luma plane of a monochrome picture) together with the colour description of the sequence header. It is the result of the AV1 decoder and the
 * input of the colour conversion and of the grid composition.
 * <p>
 * 부호화된 색 공간의 디코딩된 AV1 화면(계획 P18-03): 휘도와 색차 두 평면(단색 화면은 휘도 평면만)의 샘플 평면과 시퀀스 헤더의 색 설명. AV1
 * 디코더의 결과이며 색 변환과 격자 합성의 입력이다.
 */
final class Av1Picture {

    /**
     * The width in luma samples.
     * <p>
     * 휘도 샘플 단위 너비.
     */
    final int width;

    /**
     * The height in luma samples.
     * <p>
     * 휘도 샘플 단위 높이.
     */
    final int height;

    /**
     * The bit depth (8, 10 or 12).
     * <p>
     * 비트 깊이(8, 10 또는 12).
     */
    final int bitDepth;

    /**
     * The number of planes (1 for monochrome, otherwise 3).
     * <p>
     * 평면 수(단색은 1, 그 외 3).
     */
    final int numPlanes;

    /**
     * The horizontal subsampling of the chroma planes (0 or 1).
     * <p>
     * 색차 평면의 가로 하위 표본화(0 또는 1).
     */
    final int subX;

    /**
     * The vertical subsampling of the chroma planes (0 or 1).
     * <p>
     * 색차 평면의 세로 하위 표본화(0 또는 1).
     */
    final int subY;

    /**
     * The sample planes (index 0 luma, 1 and 2 chroma).
     * <p>
     * 샘플 평면(0 은 휘도, 1 과 2 는 색차).
     */
    final short[][] planes;

    /**
     * The row stride of each plane.
     * <p>
     * 각 평면의 행 간격.
     */
    final int[] stride;

    /**
     * The matrix coefficients identifier of the sequence header (ISO/IEC 23091-2).
     * <p>
     * 시퀀스 헤더의 행렬 계수 식별자(ISO/IEC 23091-2).
     */
    int matrixCoefficients = 2;

    /**
     * The colour primaries identifier of the sequence header.
     * <p>
     * 시퀀스 헤더의 색 원색 식별자.
     */
    int colorPrimaries = 2;

    /**
     * The transfer characteristics identifier of the sequence header.
     * <p>
     * 시퀀스 헤더의 전달 특성 식별자.
     */
    int transferCharacteristics = 2;

    /**
     * True if the samples use the full range, false for the limited (studio) range.
     * <p>
     * 샘플이 전체 범위를 쓰면 true, 제한(스튜디오) 범위면 false.
     */
    boolean fullRange;

    /**
     * The bytes of an embedded ICC profile (a <code>colr</code> property of the type <code>prof</code> or <code>rICC</code>), or null.
     * <p>
     * 내장 ICC 프로파일의 바이트(<code>prof</code> 또는 <code>rICC</code> 형식의 <code>colr</code> 속성). 없으면 null.
     */
    byte[] iccProfile;

    /**
     * Creates a picture over existing planes.
     * <p>
     * 기존 평면으로 화면을 만든다.
     *
     * @param width     the width / 너비
     * @param height    the height / 높이
     * @param bitDepth  the bit depth / 비트 깊이
     * @param numPlanes the number of planes / 평면 수
     * @param subX      the horizontal subsampling / 가로 하위 표본화
     * @param subY      the vertical subsampling / 세로 하위 표본화
     * @param planes    the planes / 평면
     * @param stride    the strides / 행 간격
     */
    Av1Picture(int width, int height, int bitDepth, int numPlanes, int subX, int subY, short[][] planes, int[] stride) {
        this.width = width;
        this.height = height;
        this.bitDepth = bitDepth;
        this.numPlanes = numPlanes;
        this.subX = subX;
        this.subY = subY;
        this.planes = planes;
        this.stride = stride;
    }

    /**
     * The width of a plane in samples.
     * <p>
     * 평면의 샘플 단위 너비.
     *
     * @param plane the plane / 평면
     * @return the width / 너비
     */
    int planeWidth(int plane) {
        return plane == 0 ? width : (width + subX) >> subX;
    }

    /**
     * The height of a plane in samples.
     * <p>
     * 평면의 샘플 단위 높이.
     *
     * @param plane the plane / 평면
     * @return the height / 높이
     */
    int planeHeight(int plane) {
        return plane == 0 ? height : (height + subY) >> subY;
    }
}
