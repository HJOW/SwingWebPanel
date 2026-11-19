package org.duckdns.hjow.swingwebpanel.render;

import org.duckdns.hjow.swingwebpanel.dom.Element;

/**
 * Finds the image of an <code>img</code> element in an {@link ImageSource}: the element with a <code>crossorigin</code> attribute gets the image
 * that was fetched in CORS mode (when the source offers it, see {@link CorsImageSource}), every other element the plain image. The CORS mode
 * is <code>use-credentials</code> for that value of the attribute and <code>anonymous</code> for any other value of the attribute (also the empty
 * one and an unknown one); an element without the attribute has none.
 * <p>
 * {@link ImageSource} 에서 <code>img</code> 요소의 이미지를 찾는다: <code>crossorigin</code> 속성이 있는 요소는 CORS 모드로 가져온 이미지를 받고(소스가 제공하면.
 * {@link CorsImageSource} 참조) 다른 요소는 일반 이미지를 받는다. CORS 모드는 속성이 그 값이면 <code>use-credentials</code>, 속성의 다른 모든 값(빈 값과 모르는 값
 * 포함)이면 <code>anonymous</code> 이고, 속성이 없는 요소에는 없다.
 */
public final class ImageLookup {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private ImageLookup() {
    }

    /**
     * Returns the CORS mode of an <code>img</code> element.
     * <p>
     * <code>img</code> 요소의 CORS 모드를 반환한다.
     *
     * @param img the element / 요소
     * @return <code>anonymous</code>, <code>use-credentials</code>, or null without the attribute / <code>anonymous</code>, <code>use-credentials</code>. 속성이 없으면 null
     */
    public static String corsMode(Element img) {
        String value = img.getAttribute("crossorigin");
        if (value == null) {
            return null;
        }
        return "use-credentials".equalsIgnoreCase(value.trim()) ? "use-credentials" : "anonymous";
    }

    /**
     * Returns the state of the image of an element.
     * <p>
     * 요소의 이미지 상태를 반환한다.
     *
     * @param images the source, may be null / 소스. null 가능
     * @param img    the <code>img</code> element / <code>img</code> 요소
     * @param url    the absolute URL of its image / 이미지의 절대 URL
     * @return the state, or null if the source is null or the image was not requested / 상태. 소스가 null 이거나 요청하지 않았으면 null
     */
    public static ImageResource of(ImageSource images, Element img, String url) {
        if (images == null) {
            return null;
        }
        String mode = corsMode(img);
        if (mode != null && images instanceof CorsImageSource) {
            return ((CorsImageSource) images).getImage(url, mode);
        }
        return images.getImage(url);
    }
}
