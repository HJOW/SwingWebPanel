package org.duckdns.hjow.swingwebpanel.layout;

import org.duckdns.hjow.swingwebpanel.css.values.CssFunctionValue;

/**
 * An optional extension of a {@link ReplacedResolver} that gives the images that define a <code>shape-outside</code> (CSS Shapes Module Level 1
 * section 4): the resolver of the renderer implements it, a resolver without it (as in many layout tests) treats an image shape like
 * <code>none</code>. Both methods return null while the picture is not available (it is still loading, failed, or may not be used because it is a
 * cross-origin image without permission), and then the float is wrapped around by its margin box; the layout runs again when the image arrived.
 * <p>
 * <code>shape-outside</code> 를 정하는 이미지를 주는 {@link ReplacedResolver} 의 선택적 확장(CSS Shapes Module Level 1 4 절): 렌더러의 리졸버가 구현하며, 구현하지 않은
 * 리졸버(많은 레이아웃 시험)에서는 이미지 모양을 <code>none</code> 처럼 다룬다. 두 메서드는 그림을 쓸 수 없는 동안(아직 가져오는 중, 실패, 허락 없는 다른 오리진의
 * 이미지라 쓸 수 없음) null 을 반환하며, 그러면 float 는 margin box 로 감싼다. 이미지가 도착하면 레이아웃을 다시 한다.
 */
public interface ShapeImageSource {

    /**
     * Returns the alpha mask of an image that is fetched with CORS (the shape must be CORS-same-origin).
     * <p>
     * CORS 로 가져온 이미지의 알파 마스크를 반환한다(모양은 CORS 같은 오리진이어야 한다).
     *
     * @param absoluteUrl the absolute address of the image / 이미지의 절대 주소
     * @return the mask of the first frame, or null if the image is not available / 첫 프레임의 마스크. 이미지를 쓸 수 없으면 null
     */
    ShapeImage getShapeImage(String absoluteUrl);

    /**
     * Returns the alpha mask of a gradient that is painted into a box of a size.
     * <p>
     * 크기가 주어진 상자에 그린 그라디언트의 알파 마스크를 반환한다.
     *
     * @param gradient     the gradient function (<code>linear-gradient()</code> and the like) / 그라디언트 함수(<code>linear-gradient()</code> 등)
     * @param currentColor the color that <code>currentcolor</code> means / <code>currentcolor</code> 가 뜻하는 색
     * @param width        the width of the content box in pixels (at least 1) / 콘텐츠 상자의 너비(픽셀, 1 이상)
     * @param height       the height of the content box in pixels (at least 1) / 콘텐츠 상자의 높이(픽셀, 1 이상)
     * @return the mask, or null if the function is not a gradient / 마스크. 그라디언트가 아니면 null
     */
    ShapeImage getShapeGradient(CssFunctionValue gradient, int currentColor, int width, int height);
}
