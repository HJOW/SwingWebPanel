package org.duckdns.hjow.swingwebpanel.layout;

/**
 * The content of a replaced element whose picture was turned by metadata of the file (the EXIF orientation of a JPEG, PNG or WebP image). The
 * natural size of such content is the size after the turn; the style <code>image-orientation: none</code> asks for the picture as it is stored,
 * whose natural size has width and height swapped for a quarter turn (CSS Images 3, section 5.1: "All CSS layout and rendering processes use
 * the image after rotation"). The layout builder asks for {@link #withoutOrientation()} when the element has that style.
 * <p>
 * 파일의 메타데이터(JPEG, PNG, WebP 이미지의 EXIF 방향)로 그림이 돌려진 치환 요소의 콘텐츠. 이런 콘텐츠의 자연 크기는 돌린 뒤의 크기이고,
 * <code>image-orientation: none</code> 스타일은 저장된 그대로의 그림을 요구하며 그 자연 크기는 4분의 1 회전이면 가로와 세로가 바뀐다(CSS Images 3,
 * 5.1절: "모든 CSS 레이아웃과 렌더링 과정은 회전한 뒤의 이미지를 쓴다"). 요소에 그 스타일이 있으면 레이아웃 빌더가 {@link #withoutOrientation()} 을 요청한다.
 */
public interface OrientedContent extends ReplacedContent {

    /**
     * Returns the same content for the picture as stored in the file, that is without the orientation of its metadata.
     * <p>
     * 파일에 저장된 그대로의 그림, 즉 메타데이터의 방향을 적용하지 않은 같은 콘텐츠를 반환한다.
     *
     * @return the content with the stored size / 저장된 크기의 콘텐츠
     */
    ReplacedContent withoutOrientation();
}
