package org.duckdns.hjow.swingwebpanel.layout;

/**
 * The external content of a replaced element (an image, audio controls, a canvas, a form control, an unsupported-video placeholder ...).
 * The layout engine only needs its natural size; drawing is done by the renderer. Sizes are in CSS pixels and may be unknown (NaN), for
 * example while an image is still loading.
 * <p>
 * 치환 요소의 외부 콘텐츠(이미지, 오디오 컨트롤, 캔버스, 폼 컨트롤, 미지원 비디오 표시 등). 레이아웃 엔진은 자연 크기만 필요로 하며 그리기는 렌더러가
 * 한다. 크기 단위는 CSS 픽셀이고 알 수 없으면(예: 이미지를 아직 읽는 중) NaN 이다.
 */
public interface ReplacedContent {

    /**
     * Returns the natural width.
     * <p>
     * 자연 너비를 반환한다.
     *
     * @return width in CSS pixels, NaN if unknown / CSS 픽셀 단위 너비. 알 수 없으면 NaN
     */
    double getNaturalWidth();

    /**
     * Returns the natural height.
     * <p>
     * 자연 높이를 반환한다.
     *
     * @return height in CSS pixels, NaN if unknown / CSS 픽셀 단위 높이. 알 수 없으면 NaN
     */
    double getNaturalHeight();
}
