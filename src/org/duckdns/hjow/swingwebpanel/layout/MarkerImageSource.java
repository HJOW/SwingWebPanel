package org.duckdns.hjow.swingwebpanel.layout;

import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.dom.Element;

/**
 * Tells the layout the size of the image of a list marker (<code>list-style-image</code>, CSS Lists 3 section 3.3): the renderer knows the images that have
 * arrived and the image functions of CSS Images 4. A replaced content resolver may also implement this interface; the box builder asks it when a list item
 * has a <code>list-style-image</code>.
 * <p>
 * 목록 표식 이미지(<code>list-style-image</code>, CSS Lists 3 3.3 절)의 크기를 레이아웃에 알려 준다: 도착한 이미지와 CSS Images 4 의 이미지 함수는 렌더러가 안다. 치환 콘텐츠
 * 리졸버가 이 인터페이스도 구현할 수 있으며, 목록 항목에 <code>list-style-image</code> 가 있으면 상자 빌더가 묻는다.
 */
public interface MarkerImageSource {

    /**
     * Returns the natural size of an image value, or null if the image is invalid or still loading (the marker then uses <code>list-style-type</code>).
     * <p>
     * 이미지 값의 고유 크기를 반환한다. 이미지가 잘못되었거나 아직 불러오는 중이면 null(그러면 표식은 <code>list-style-type</code> 을 쓴다).
     *
     * @param image   the value of <code>list-style-image</code> / <code>list-style-image</code> 의 값
     * @param element the list item / 목록 항목
     * @return {width, height}, NaN for a dimension that the image does not have, or null / {너비, 높이}(이미지에 없는 크기는 NaN). 없으면 null
     */
    double[] markerImageSize(CssValue image, Element element);
}
